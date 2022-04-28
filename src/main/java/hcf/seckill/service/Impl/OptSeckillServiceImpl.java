package hcf.seckill.service.Impl;

import com.alibaba.rocketmq.shade.com.alibaba.fastjson.JSON;
import hcf.order.rocketMQ.MqProducer;
import hcf.seckill.Utils.Md5Utils;
import hcf.seckill.dao.SeckillDao;
import hcf.seckill.dao.SuccessKilledDao;
import hcf.seckill.dao.cache.RedisDao;
import hcf.seckill.dto.Exposer;
import hcf.seckill.dto.SeckillExecution;
import hcf.seckill.entity.Seckill;
import hcf.seckill.entity.SuccessKilled;
import hcf.seckill.enums.SeckillStateEnum;
import hcf.seckill.exception.RepeatKillException;
import hcf.seckill.exception.SeckillCloseException;
import hcf.seckill.exception.SeckillException;
import hcf.seckill.service.OptSeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hechaofan
 * @date 2022/3/21 16:12
 */
@Service
public class OptSeckillServiceImpl implements OptSeckillService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RedisDao redisDao;

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;

    // 消息队列，消息生产者
    @Autowired
    private MqProducer mqProducer;

    /**
     * 秒杀开启时输出秒杀接口地址，否则输出系统时间和秒杀时间
     * @param seckillId
     * @return
     */
    public Exposer exportSeckillUrl(long seckillId) {
        //优化点：缓存优化,超时的基础上维护一致性
        /**
         *
         * get from cache（从缓存中获取）
         * if null（缓存中没有）
         *     get db（从DB中获取）
         *     put cache（放入缓存中）
         * logoin
         */
        Seckill seckill = redisDao.getSeckill(seckillId);
        if(seckill == null){
            seckill = seckillDao.queryById(seckillId); //2 访问数据库
            if(seckill == null) {
                return new Exposer(false, seckillId);
            }else{  //放入redis
                redisDao.setSeckill(seckill);
            }
        }
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date nowTime = new Date();
        // 判断秒杀时间是否在范围内
        if(nowTime.getTime() < startTime.getTime()
                || nowTime.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId,
                    nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
        //转化为一个特定字符串，不可逆
        String md5 = Md5Utils.getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }


    /**
     * 生产秒杀消息
     * @param seckillId
     * @return
     */
    public void sendSeckillInfoToOrderSystem(long seckillId, long userPhone){
        Map<String, Object> msg = new HashMap<>();
        msg.put("seckillId", seckillId);
        msg.put("userPhone", userPhone);
        MqProducer.sendMsgIntime(
                "OrderTopic"
                ,"order"
                ,""
                ,JSON.toJSONString(msg));
    }
    /***
     * executeSeckill 方法 current Version ( 秒杀 | 订单 系统分离)
     * lua + redis版本
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    @Override
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // 1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
            boolean lessToSellFlag = false;      // 少卖的flag标签
            boolean isExist = redisDao.existsInventoryKey(seckillId);
            if(!isExist){
                // 1.1 Redis中不存在对应商品，加锁 + LOCK
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    /*
                    // 1.1.1 未获取到锁，抛出异常
                    // throw new SeckillException("抢锁失败！");
                    */
                    int spinning=5;
                    while(spinning > 0 && !"OK".equals(redisDao.existsInventoryKey(seckillId)) ){
                        Thread.sleep(100);
                        spinning--;
                    }
                    if(spinning <= 0 ){
                        throw new SeckillException("抢锁失败！");
                    }else if("OK".equals(redisDao.existsInventoryKey(seckillId))){
                        lessToSellFlag = true;
                    }
                    // TODO 首次获取锁的过程中，可能1000个用户在抢锁，但是只有一个用户获取到，那么其他999个全部失败, 这种情况下可能导致少卖
                    // 这种情况： 一共 10 个产品，1000人来抢，发现redis中没有数据 所以抢分布式锁，
                    // 1000个就一个抢到了，999失败， ...，那就还有 9 个没卖出去
                }
                if(lessToSellFlag == false){
                    // 1.1.2 加锁成功
                    Seckill seckill = seckillDao.queryById(seckillId);
                    if(seckill == null) {
                        // 1.1.2.1 DB中不存在
                        throw new SeckillException("该商品不属于秒杀商品");
                    }else{
                        // 1.1.2.2 DB中存在该数据
                        redisDao.setInventory(seckill.getSeckillId(), seckill.getNumber()); // 重新更新放入
                        // 释放锁 - LOCK： ret_release == 1 成功， == 0 已经释放锁
                        Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);
                    }
                }
            }
            // 2. 判断秒杀时间是否在范围内（好像没什么必要）
            // 3. 判断是否重复购买 ：使用Redis的Set来判断，单机情况下JVM的HashSet也可以
            isExist = redisDao.getUserSeckillState(seckillId, userPhone);
            if(isExist){ // 已经存在则抛出异常
                throw new RepeatKillException("重复秒杀");
            }
            // 4. 减库存操作： lua 版本解决了潜在的超卖问题
            long result = redisDao.callLuaScriptToDecrInventory(seckillId, userPhone);
            if(result < 0){                                 //note：不加以判断则会出现DB、缓存不一致问题
                redisDao.delKey(seckillId,0,"inventory");
                throw new SeckillException("商品卖光光了！");
            }
            // ------------------ 通过 RocketMQ 将 秒杀系统 与 订单系统解耦
            sendSeckillInfoToOrderSystem(seckillId, userPhone);
            // -------------------------------------------------------------------------------------------------------
            return new SeckillExecution( seckillId
                    , SeckillStateEnum.SUCCESS
                    , new SuccessKilled()); // TODO 秒杀记录未处理操作
        }catch (SeckillCloseException | RepeatKillException e1){
            logger.error(e1.getMessage(), e1);
            throw e1;
        } catch (SeckillException e3){
            logger.error(e3.getMessage(), e3);
            throw e3;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }
    /***
     * executeSeckill 方法的Jmeter测试版本，
     * 便于测试，取消了重复秒杀的判断，用户秒杀信息在 success_killed的存储
     * lua + redis版本
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    @Override
    public SeckillExecution executeSeckillForJmeter(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            boolean lessToSellFlag = false;      // 少卖的flag标签
            // 1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
            boolean isExist = redisDao.existsInventoryKey(seckillId);
            if(!isExist){
                // 1.1 Redis中不存在对应商品，加锁 + LOCK
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    /*
                    // 1.1.1 未获取到锁，抛出异常
                    // throw new SeckillException("抢锁失败！");
                    */
                    int spinning=5;
                    while(spinning > 0 && !"OK".equals(redisDao.existsInventoryKey(seckillId)) ){
                        Thread.sleep(100);
                        spinning--;
                    }
                    if(spinning <= 0 ){
                        throw new SeckillException("抢锁失败！");
                    }else if("OK".equals(redisDao.existsInventoryKey(seckillId))){
                        lessToSellFlag = true;
                    }
                    // TODO 首次获取锁的过程中，可能1000个用户在抢锁，但是只有一个用户获取到，那么其他999个全部失败, 这种情况下可能导致少卖
                    // 这种情况： 一共 10 个产品，1000人来抢，发现redis中没有数据 所以抢分布式锁，
                    // 1000个就一个抢到了，999失败， ...，那就还有 9 个没卖出去
                }
                if(lessToSellFlag == false){
                    // 1.1.2 加锁成功
                    Seckill seckill = seckillDao.queryById(seckillId);
                    if(seckill == null) {
                        // 1.1.2.1 DB中不存在
                        throw new SeckillException("该商品不属于秒杀商品");
                    }else{
                        // 1.1.2.2 DB中存在该数据
                        redisDao.setInventory(seckill.getSeckillId(), seckill.getNumber()); // 重新更新放入
                        // 释放锁 - LOCK： ret_release == 1 成功， == 0 已经释放锁
                        Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);
                    }
                }
            }
            long result = redisDao.callLuaScriptToDecrInventory(seckillId, userPhone);
            if(result < 0){             // note：不加以判断则会出现DB、缓存不一致问题
                throw new SeckillException("商品卖光光了！");
            }
            // ------------------ 通过 RocketMQ 将 秒杀系统 与 订单系统解耦
            sendSeckillInfoToOrderSystem(seckillId, userPhone);
            // -------------------------------------------------------------------------------------------------------
            return new SeckillExecution( seckillId
                    , SeckillStateEnum.SUCCESS
                    , new SuccessKilled()); // TODO 秒杀记录未处理操作
        }catch (SeckillCloseException | RepeatKillException e1){
            // logger.error(e1.getMessage(), e1);
            throw e1;
        } catch (SeckillException e3){
            // logger.error(e3.getMessage(), e3);
            throw e3;
        }catch (Exception e){
            // logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }

    /***
     * executeSeckill 方法的Jmeter测试版本，
     * 便于测试，取消了重复秒杀的判断，用户秒杀信息在 success_killed的存储
     * lua + redis版本
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    public SeckillExecution executeSeckillForJmeter_Version2(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // 1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
            boolean isExist = redisDao.existsInventoryKey(seckillId);
            if(!isExist){
                // 1.1 Redis中不存在对应商品，加锁 + LOCK
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    // 1.1.1 未获取到锁，抛出异常
                    throw new SeckillException("抢锁失败！");
                    // TODO 首次获取锁的过程中，可能1000个用户在抢锁，但是只有一个用户获取到，那么其他999个全部失败
                    // 这样可能导致 少卖 问题
                }
                // 1.1.2 加锁成功，从DB中获取数据
                Seckill seckill = seckillDao.queryById(seckillId);
                if(seckill == null) {
                    // 1.1.2.1 DB中不存在
                    throw new SeckillException("该商品不属于秒杀商品");
                }else{
                    // 1.1.2.2 DB中存在该数据
                    redisDao.setInventory(seckill.getSeckillId(), seckill.getNumber()); // 更新
                    // 释放锁 - LOCK： ret_release == 1 成功， == 0 已经释放锁
                    Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);
                }
            }
            long result = redisDao.callLuaScriptToDecrInventory(seckillId, userPhone);
            if(result < 0){             // note：不加以判断则会出现DB、缓存不一致问题
                throw new SeckillException("商品卖光光了！");
            }
            // ------------------ 通过 RocketMQ 将 秒杀系统 与 订单系统解耦
            sendSeckillInfoToOrderSystem(seckillId, userPhone);
            // -------------------------------------------------------------------------------------------------------
            return new SeckillExecution( seckillId
                    , SeckillStateEnum.SUCCESS
                    , new SuccessKilled()); // TODO 秒杀记录未处理操作
        }catch (SeckillCloseException | RepeatKillException e1){
            // logger.error(e1.getMessage(), e1);
            throw e1;
        } catch (SeckillException e3){
            // logger.error(e3.getMessage(), e3);
            throw e3;
        }catch (Exception e){
            // logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }

    /***
     * executeSeckill 方法的Jmeter测试版本，
     * 便于测试，取消了重复秒杀的判断，用户秒杀信息在 success_killed的存储
     * lua + redis版本
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    public SeckillExecution executeSeckillForJmeter_Version1(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // 1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
            boolean isExist = redisDao.existsInventoryKey(seckillId);
            if(!isExist){
                // 1.1 Redis中不存在对应商品，加锁 + LOCK
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    // 1.1.1 未获取到锁，抛出异常
                    throw new SeckillException("抢锁失败！");
                    // TODO 首次获取锁的过程中，可能1000个用户在抢锁，但是只有一个用户获取到，那么其他999个全部失败
                    // 这样可能导致 少卖 问题

                }
                // 1.1.2 加锁成功，从DB中获取数据
                Seckill seckill = seckillDao.queryById(seckillId);
                if(seckill == null) {
                    // 1.1.2.1 DB中不存在
                    throw new SeckillException("该商品不属于秒杀商品");
                }else{
                    // 1.1.2.2 DB中存在该数据
                    redisDao.setInventory(seckill.getSeckillId(), seckill.getNumber()); // 更新
                    // 释放锁 - LOCK： ret_release == 1 成功， == 0 已经释放锁
                    Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);
                }
            }
            long result = redisDao.callLuaScriptToDecrInventory(seckillId, userPhone);
            if(result < 0){             // note：不加以判断则会出现DB、缓存不一致问题
                throw new SeckillException("商品卖光光了！");
            }
            //  TODO DB减库存(秒杀阶段不合适) 需更新
            int updateNum = seckillDao.reduceNumber(seckillId, new Date()); //
            if (updateNum <= 0) { //记录更新失败，秒杀失败
                Long updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复缓存的数据
                throw new SeckillCloseException("超卖 Or 秒杀关闭!!!");
            }

            // -------------------------------------------------------------------------------------------------------
            return new SeckillExecution( seckillId
                    , SeckillStateEnum.SUCCESS
                    , new SuccessKilled()); // TODO 秒杀记录未处理操作
        }catch (SeckillCloseException | RepeatKillException e1){
            // logger.error(e1.getMessage(), e1);
            throw e1;
        } catch (SeckillException e3){
            // logger.error(e3.getMessage(), e3);
            throw e3;
        }catch (Exception e){
            // logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }


    /***
     * executeSeckill 方法的旧版本 秒杀 + 订单 一体化
     * lua + redis版本
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    public SeckillExecution executeSeckill_Version4(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // 1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
            boolean isExist = redisDao.existsInventoryKey(seckillId);
            if(!isExist){
                // 1.1 Redis中不存在对应商品，加锁 + LOCK
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    // 1.1.1 未获取到锁，抛出异常
                    throw new SeckillException("抢锁失败！");
                    // TODO 首次获取锁的过程中，可能1000个用户在抢锁，但是只有一个用户获取到，那么其他999个全部失败
                }
                // 1.1.2 加锁成功
                Seckill seckill = seckillDao.queryById(seckillId);
                if(seckill == null) {
                    // 1.1.2.1 DB中不存在
                    throw new SeckillException("该商品不属于秒杀商品");
                }else{
                    // 1.1.2.2 DB中存在该数据
                    redisDao.setInventory(seckill.getSeckillId(), seckill.getNumber()); // 重新更新放入
                    // 释放锁 - LOCK： ret_release == 1 成功， == 0 已经释放锁
                    Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);

                }
            }
            // 2. 判断秒杀时间是否在范围内（好像没什么必要）
            // 3. 判断是否重复购买 ：使用Redis的Set来判断，单机情况下JVM的HashSet也可以
            isExist = redisDao.getUserSeckillState(seckillId, userPhone);
            if(isExist){ // 已经存在则抛出异常
                throw new RepeatKillException("重复秒杀");
            }
            // 4. 减库存操作： lua 版本解决了潜在的超卖问题
            long result = redisDao.callLuaScriptToDecrInventory(seckillId, userPhone);
            if(result < 0){                                 //note：不加以判断则会出现DB、缓存不一致问题
                throw new SeckillException("商品卖光光了！");
            }
            // -------------------------------------------------------------------------------------------------------
            // ------------------ 数据库层面的减库存操作  TODO DB减库存(秒杀阶段不合适)
            // -------------------------------------------------------------------------------------------------------
            int updateNum = seckillDao.reduceNumber(seckillId, new Date()); //
            if (updateNum <= 0) { //记录更新失败，秒杀失败
                Long updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复缓存的数据
                throw new SeckillCloseException("超卖 Or 秒杀关闭!!!");
            }
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) { //seckillId-userPhone唯一 //重复秒杀（是DB级别的判断）
                Long updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复Redis缓存的数据
                Long userSeckillState = redisDao.delSeckillUser(seckillId, userPhone);
                throw new RepeatKillException("重复秒杀");
            }
            // -------------------------------------------------------------------------------------------------------
            return new SeckillExecution( seckillId
                    , SeckillStateEnum.SUCCESS
                    , new SuccessKilled()); // TODO 秒杀记录未处理操作
        }catch (SeckillCloseException | RepeatKillException e1){
            logger.error(e1.getMessage(), e1);
            throw e1;
        } catch (SeckillException e3){
            logger.error(e3.getMessage(), e3);
            throw e3;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }

    /***
     * executeSeckill 方法的旧版本 current Version
     * lua + redis版本
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    public SeckillExecution executeSeckill_Version3(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // Q 1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
            boolean isExist = redisDao.existsInventoryKey(seckillId);
            if(!isExist){
                // 1.1 Redis中不存在对应商品，加锁 + LOCK
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    // 1.1.1 未获取到锁，抛出异常
                    throw new SeckillException("抢锁失败！");
                }
                // 1.1.2 加锁成功
                Seckill seckill = seckillDao.queryById(seckillId);
                if(seckill == null) {
                    // 1.1.2.1 DB中不存在
                    throw new SeckillException("该商品不属于秒杀商品");
                }else{
                    // 1.1.2.2 DB中存在该数据
                    redisDao.setInventory(seckill.getSeckillId(), seckill.getNumber()); // 重新更新放入
                    // 释放锁 - LOCK： ret_release == 1 成功， == 0 已经释放锁
                    Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);

                }
            }
            // Q 2. 判断秒杀时间是否在范围内（好像没什么必要）
            // Q 3. 判断是否重复购买 ：使用Redis的Set来判断，单机情况下JVM的HashSet也可以
            isExist = redisDao.getUserSeckillState(seckillId, userPhone);
            if(isExist){ // 已经存在则抛出异常
                throw new RepeatKillException("重复秒杀");
            }
            // Q 4. 减库存操作： lua 版本解决了潜在的超卖问题  result 不加以判断则会出现DB、缓存不一致问题
            long result = redisDao.callLuaScriptToDecrInventory(seckillId, userPhone);

            // -------------------------------------------------------------------------------------------------------
            // ------------------ 数据库层面的减库存操作  Q DB减库存(秒杀阶段不合适)
            // -------------------------------------------------------------------------------------------------------
            int updateNum = seckillDao.reduceNumber(seckillId, new Date()); //
            if (updateNum <= 0) { //记录更新失败，秒杀失败
                Long updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复缓存的数据
                throw new SeckillCloseException("超卖 Or 秒杀关闭!!!");
            }
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) { //seckillId-userPhone唯一 //重复秒杀（是DB级别的判断）
                Long updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复Redis缓存的数据
                Long userSeckillState = redisDao.delSeckillUser(seckillId, userPhone);
                throw new RepeatKillException("重复秒杀");
            }
            // -------------------------------------------------------------------------------------------------------
            return new SeckillExecution( seckillId
                    , SeckillStateEnum.SUCCESS
                    , new SuccessKilled()); // Q 秒杀记录未处理操作
        }catch (SeckillCloseException | RepeatKillException e1){
            logger.error(e1.getMessage(), e1);
            throw e1;
        } catch (SeckillException e3){
            logger.error(e3.getMessage(), e3);
            throw e3;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }


    /***
     * executeSeckill 方法的旧版本 Version2
     * Redis版本
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    public SeckillExecution executeSeckill_Version2(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // Q1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
            Long inventory = redisDao.getInventory(seckillId);
            if(inventory == null){
                // 1.1 Redis中不存在对应商品，加锁 + LOCK
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    // 1.1.1 未获取到锁，抛出异常
                    throw new SeckillException("抢锁失败！");
                }
                // 1.1.2 加锁成功
                Seckill seckill = seckillDao.queryById(seckillId);
                if(seckill == null) {
                    // 1.1.2.1 DB中不存在
                    throw new SeckillException("该商品不属于秒杀商品");
                }else{
                    // 1.1.2.2 DB中存在该数据
                    redisDao.setInventory(seckill.getSeckillId(), seckill.getNumber()); // 重新更新放入
                    // 释放锁 - LOCK： ret_release == 1 成功， == 0 已经释放锁
                    Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);

                }
            }
            // Q 2. 判断秒杀时间是否在范围内（好像没什么必要）
            /*
                Date startTime = seckill.getStartTime();
                Date endTime = seckill.getEndTime();
                Date nowTime = new Date();
                if(nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
                    throw new SeckillException("该商品不属于秒杀商品");
                }
            */
            // Q3. 判断是否重复购买 ：使用Redis的Set来判断，单机情况下JVM的HashSet也可以
            boolean isExist = redisDao.getUserSeckillState(seckillId, userPhone);
            if(isExist){ // 已经存在则抛出异常
                throw new RepeatKillException("重复秒杀");
            }
            // Q4. 减库存操作： 4.1 到 4.2 的过程不是原子操作，可能导致潜在的超卖问题
            Long val = redisDao.getInventory(seckillId);            // 4.1 获取当前库存
            if(val <= 0){
                throw new RepeatKillException("秒杀商品结束了");
            }
            Long updateVal = redisDao.decrInventory(seckillId);     // 4.2 Redis 减库存
            Long userSeckillState = redisDao.addSeckillUser(seckillId, userPhone);  // 将秒杀用户添加到Redis中，防止重复秒杀

            // -------------------------------------------------------------------------------------------------------
            // DB减库存
            // -------------------------------------------------------------------------------------------------------
            int updateNum = seckillDao.reduceNumber(seckillId, new Date()); // Q DB减库存(秒杀阶段进行有点不合适)
            if (updateNum <= 0) { //记录更新失败，秒杀失败
                updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复缓存的数据
                throw new SeckillCloseException("超卖 Or 秒杀关闭!!!");
            }
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) { //seckillId-userPhone唯一 //重复秒杀（是DB级别的判断）
                updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复Redis缓存的数据
                userSeckillState = redisDao.delSeckillUser(seckillId, userPhone);
                throw new RepeatKillException("重复秒杀");
            }
            // -------------------------------------------------------------------------------------------------------
            return new SeckillExecution( seckillId
                    , SeckillStateEnum.SUCCESS
                    , new SuccessKilled()); // Q 秒杀记录未处理操作
        }catch (SeckillCloseException | RepeatKillException e1){
            logger.error(e1.getMessage(), e1);
            throw e1;
        } catch (SeckillException e3){
            logger.error(e3.getMessage(), e3);
            throw e3;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }

    /***
     * executeSeckill 方法的旧版本 Version1
     * DB版本
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    public SeckillExecution executeSeckill_Version1(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // Q 1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
            Long inventory = redisDao.getInventory(seckillId);
            if(inventory == null){
                // 1.1 Redis中不存在对应商品，加锁 + LOCK
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    // 1.1.1 未获取到锁，抛出异常
                    throw new SeckillException("抢锁失败！");
                }
                    // 1.1.2 加锁成功
                Seckill seckill = seckillDao.queryById(seckillId);
                if(seckill == null) {
                        // 1.1.2.1 DB中不存在
                    throw new SeckillException("该商品不属于秒杀商品");
                }else{
                        // 1.1.2.2 DB中存在该数据
                    redisDao.setInventory(seckill.getSeckillId(), seckill.getNumber()); // 重新更新放入
                    // 释放锁 - LOCK： ret_release == 1 成功， == 0 已经释放锁
                    Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);

                }
            }
            // Q 2. 判断秒杀时间是否在范围内（好像没什么必要）
            /*
            Date startTime = seckill.getStartTime();
            Date endTime = seckill.getEndTime();
            Date nowTime = new Date();
            if(nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
                throw new SeckillException("该商品不属于秒杀商品");
            }
            */
            // Q 3. 判断是否重复购买 （需完善） ，当前是 DB 级别的判断
            /*
                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
                if (insertCount <= 0) { //seckillId-userPhone唯一
                    //重复秒杀（是DB级别的判断）
                    throw new RepeatKillException("重复秒杀");
                }*/
            // Q 4. 减库存操作（可能发生超卖） 4.1 和 4.2 整体不是原子操作  // 潜在的超卖问题
            Long val = redisDao.getInventory(seckillId);            // 4.1 获取当前库存
            if(val <= 0){
                throw new RepeatKillException("重复秒杀");
            }
            Long updateVal = redisDao.decrInventory(seckillId); // 4.2 Redis 减库存
            int updateNum = seckillDao.reduceNumber(seckillId, new Date()); // Q DB减库存(秒杀阶段进行有点不合适)
            if (updateNum <= 0) { //
                // 记录更新失败，秒杀失败
                updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复缓存的数据
                throw new SeckillCloseException("超卖 Or 秒杀关闭!!!");
            }
            return new SeckillExecution( seckillId
                    , SeckillStateEnum.SUCCESS
                    , new SuccessKilled()); // Q 秒杀记录未处理操作
        }catch (SeckillCloseException | RepeatKillException e1){
            logger.error(e1.getMessage(), e1);
            throw e1;
        } catch (SeckillException e3){
            logger.error(e3.getMessage(), e3);
            throw e3;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }
}
