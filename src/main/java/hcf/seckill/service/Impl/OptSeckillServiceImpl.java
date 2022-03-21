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

    @Override
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5 == null || md5.equals(Md5Utils.getMD5(seckillId))){
            throw new SeckillException("秒杀路径错误！");
        }
        try{
            // 1. 判断Redis中 对应秒杀商品 是否存在 TODO 获取不到锁就结束了
            Seckill seckill = redisDao.getSeckill(seckillId);
            if(seckill == null){
                // 1.1 Redis中不存在对应商品，加锁
                String ret = redisDao.setSeckillLock(seckillId, userPhone);
                if( ! "OK".equals(ret)){
                    // 1.1.1 未获取到锁，抛出异常
                    throw new SeckillException("抢锁失败！");
                }
                    // 1.1.2 加锁成功
                seckill = seckillDao.queryById(seckillId);
                if(seckill == null) {
                        // 1.1.2.1 DB中不存在
                    throw new SeckillException("该商品不属于秒杀商品");
                }else{
                        // 1.1.2.2 DB中存在该数据
                    redisDao.setSeckill(seckill); // 重新更新放入
                    // 释放锁： ret_release == 1 成功， == 0 已经释放锁
                    Long ret_release = redisDao.releaseSeckillLock(seckillId, userPhone);

                }
            }
            // 2. 判断秒杀时间是否在范围内
            Date startTime = seckill.getStartTime();
            Date endTime = seckill.getEndTime();
            Date nowTime = new Date();
            if(nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
                throw new SeckillException("该商品不属于秒杀商品");
            }
            // 3. 判断是否重复购买
            // TODO 判断是否重复购买 未完成
            /*
                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
                if (insertCount <= 0) { //seckillId-userPhone唯一
                    //重复秒杀（是DB级别的判断）
                    throw new RepeatKillException("重复秒杀");
                }*/
            // 4. 减库存操作（可能发生超卖） 4.1 和 4.2 整体不是原子操作  // TODO 潜在的超卖问题
            Long val = redisDao.getInventory(seckillId);            // 4.1 获取当前库存
            if(val <= 0){
                throw new RepeatKillException("重复秒杀");
            }
            Long updateVal = redisDao.decrInventory(seckillId);     // 4.2 减库存
            int updateNum = seckillDao.reduceNumber(seckillId, nowTime); // TODO DB减库存
            if (updateNum <= 0) { //
                // 更新记录失败，未秒杀成功，秒杀结束
                throw new SeckillCloseException("超卖 Or 秒杀关闭!!!");
            }
            return new SeckillExecution(
                    seckillId
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
