package hcf.seckill.dao.cache;

import hcf.seckill.dao.SeckillDao;
import hcf.seckill.entity.Seckill;
import hcf.seckill.exception.SeckillException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * @author hechaofan
 * @date 2022/3/19 16:17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class RedisDaoTest {

    private long id = 1;

    @Autowired
    private RedisDao redisDao;

    @Autowired
    private SeckillDao seckillDao;

    @Test
    public void getSeckill() {
        //get and put
        Seckill seckill = redisDao.getSeckill(id);
        if (seckill == null){
            seckill = seckillDao.queryById(id);
            if (seckill != null){
                String result = redisDao.setSeckill(seckill);
                System.out.println(result);
                seckill = redisDao.getSeckill(id);
                System.out.println(seckill);
            }
        }
    }


    @Test
    public void setSeckill() {
    }

    /**
     * 测试 Lua 脚本，减库存操作
     */
    @Test
    public void operateLUA(){
        long seckillId = 6L;
        long userPhone = 18400000001L;
        redisDao.delKey(seckillId, 0L, "inventory");
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
        long result = redisDao.callLuaScriptToDecrInventory(seckillId, userPhone);
        boolean state = redisDao.getUserSeckillState(seckillId, userPhone);
        System.out.println("lua执行结果 = "+ result+ "     state = "+  state);
    }
}