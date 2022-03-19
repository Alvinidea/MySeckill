package hcf.seckill.service.Impl;

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
import hcf.seckill.service.SeckillService;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author hechaofan
 * @date 2022/3/17 15:03
 */
@Service
public class SeckillServiceImpl implements SeckillService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillDao seckillDao;

    @Autowired
    private SuccessKilledDao successKilledDao;

    @Autowired
    private RedisDao redisDao;

    // 盐，MD5混淆因子
    private final String slat = "lsdfjolghoaes&**(sdgfklsfaf&&*(seorfihdsnj";

    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0,10);
    }

    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

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
            seckill = seckillDao.queryById(seckillId); //2访问数据库
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
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }

    private String getMD5(long seckillId){
        String base = seckillId  + "/"+ slat;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }


    /**
     * 使用注解控制事务方法的优点：
     *  1、开发团队达成一致约定，约定明确标注事务方法的编程风格。
     *  2、保证事务方法的 执行时间尽可能短 ，不要穿插其他的网络操作（RPC/HTTP请求），或者剥离到方法外面。
     *  3、不是所有的方法都需要事务，如：只有一条修改操作、只读操作。
     */
    @Transactional(rollbackFor = Exception.class)
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(getMD5(seckillId))){
            throw new SeckillException("Seckill data rewrite!!!");
        }
        // 秒杀操作逻辑：减库存 + 记录购买行为
        Date nowTime = new Date();
        try {
            // 减库存成功记录购买行为
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) { //seckillId-userPhone唯一
                //重复秒杀（是DB级别的判断）
                throw new RepeatKillException("hcf.seckill repeat"); // TODO 如果失败了还是会减库存 ？？？
            } else {
                // 减库存,热点商品竞争
                int updateNum = seckillDao.reduceNumber(seckillId, nowTime);
                if (updateNum <= 0) { //
                    // 更新记录失败，未秒杀成功，秒杀结束
                    throw new SeckillCloseException("hcf.seckill already closed!!!");
                } else {
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(
                            seckillId, userPhone);
                    return new SeckillExecution(
                            seckillId,
                            SeckillStateEnum.SUCCESS,
                            successKilled);
                }//秒杀成功
            }
        }catch (SeckillCloseException e1){
            logger.error(e1.getMessage(), e1);
            throw e1;
        }catch (RepeatKillException e2){
            logger.error(e2.getMessage(), e2);
            throw e2;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }

    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if(md5 == null || !md5.equals(getMD5(seckillId))){
            throw new SeckillException("Seckill data rewrite!!!");
        }
        Date killTime = new Date();
        Map<String, Object> map = new HashMap<>();
        map.put("seckillId",seckillId);
        map.put("phone",userPhone);
        map.put("killTime",killTime);
        map.put("result",null);
        //执行完存储过程之后result被赋值
        try{
            seckillDao.killByProcedure(map);
            int result = MapUtils.getInteger(map, "result", -2);
            if(result == 1){
                SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
            }else{
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return  new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }
}


/*
    public Exposer exportSeckillUrl(long seckillId) {
        Seckill seckill = seckillDao.queryById(seckillId);
        if(seckill == null){
            return new Exposer(false, seckillId);
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
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);



        @Transactional(rollbackFor = Exception.class)
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || !md5.equals(getMD5(seckillId))){
            throw new SeckillException("Seckill data rewrite!!!");
        }
        // 秒杀操作逻辑：减库存 + 记录购买行为
        Date nowTime = new Date();
        try {
            // 减库存
            int updateNum = seckillDao.reduceNumber(seckillId, nowTime);
            if (updateNum <= 0) { //
                // 更新记录失败，未秒杀成功，秒杀结束
                throw new SeckillCloseException("hcf.seckill already closed!!!");
            } else {
                // 减库存成功记录购买行为
                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
                if (insertCount <= 0) { //seckillId-userPhone唯一
                    //重复秒杀
                    throw new RepeatKillException("hcf.seckill repeat"); // TODO 如果失败了还是会减库存 ？？？
                } else {
                    //秒杀成功
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS,
                            successKilled);
                }
            }
        }catch (SeckillCloseException e1){
            logger.error(e1.getMessage(), e1);
            throw e1;
        }catch (RepeatKillException e2){
            logger.error(e2.getMessage(), e2);
            throw e2;
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            throw new SeckillException("Seckill inner error " + e.getMessage());
        }
    }
    }*/
