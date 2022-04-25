package hcf.order.service;

import com.alibaba.rocketmq.shade.com.alibaba.fastjson.JSON;
import hcf.order.rocketMQ.MqProducer;
import hcf.seckill.dao.SeckillDao;
import hcf.seckill.dao.SuccessKilledDao;
import hcf.seckill.exception.RepeatKillException;
import hcf.seckill.exception.SeckillCloseException;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hechaofan
 * @date 2022/4/24 13:50
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    @Autowired
    private SeckillDao seckillDao;
    @Autowired
    private SuccessKilledDao successKilledDao;

    public void consumeSeckillMsg(long seckillId, long userPhone){
        int updateNum = seckillDao.reduceNumber(seckillId, new Date()); //
        if (updateNum <= 0) { //记录更新失败，秒杀失败
            // Long updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复缓存的数据
            // throw new SeckillCloseException("超卖 Or 秒杀关闭!!!");
            System.out.println("order - 超卖 Or 秒杀关闭!!!");
            logger.info("order - 超卖 Or 秒杀关闭!!!");
            return;
        }
        int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
        if (insertCount <= 0) { //seckillId-userPhone唯一 //重复秒杀（是DB级别的判断）
            // Long updateVal = redisDao.incrInventory(seckillId);  // DB减库存未成功，则需要恢复Redis缓存的数据
            // Long userSeckillState = redisDao.delSeckillUser(seckillId, userPhone);
            logger.info("重复秒杀");
            // throw new RepeatKillException("重复秒杀");
            return;
        }
    }

}
