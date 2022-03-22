package hcf.seckill.service.Impl;

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

    private SuccessKilledDao successKilledDao;

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

    @Override
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // TODO 1. 判断Redis中 对应秒杀商品 是否存在 获取不到锁就结束了
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
            // TODO 2. 判断秒杀时间是否在范围内（好像没什么必要）
            /*
            Date startTime = seckill.getStartTime();
            Date endTime = seckill.getEndTime();
            Date nowTime = new Date();
            if(nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
                throw new SeckillException("该商品不属于秒杀商品");
            }
            */
            // TODO 3. 判断是否重复购买 （需完善）
            /*
                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
                if (insertCount <= 0) { //seckillId-userPhone唯一
                    //重复秒杀（是DB级别的判断）
                    throw new RepeatKillException("重复秒杀");
                }*/
            // TODO 4. 减库存操作（可能发生超卖） 4.1 和 4.2 整体不是原子操作  // 潜在的超卖问题
            Long val = redisDao.getInventory(seckillId);            // 4.1 获取当前库存
            if(val <= 0){
                throw new RepeatKillException("重复秒杀");
            }
            Long updateVal = redisDao.decrInventory(seckillId); // 4.2 Redis 减库存
            int updateNum = seckillDao.reduceNumber(seckillId, new Date()); // TODO DB减库存(秒杀阶段进行有点不合适)
            if (updateNum <= 0) { //
                // 记录更新失败，秒杀失败
                updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复缓存的数据
                throw new SeckillCloseException("超卖 Or 秒杀关闭!!!");
            }
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
}
