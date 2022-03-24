package hcf.seckill.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.sun.org.apache.xpath.internal.operations.Bool;
import hcf.seckill.entity.Seckill;
import hcf.seckill.exception.RepeatKillException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author hechaofan
 * @date 2022/3/19 15:53
 */
public class RedisDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JedisPool jedisPool;

    // 序列化工具(优势：空间小， 速度快)
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);
    private StringBuilder luastr;
    private StringBuilder luasb;

    public RedisDao(String ip, int port) {
        this.jedisPool = new JedisPool(ip, port);
    }

    public Boolean existsKey(long seckillId, long phone, String type){
            try {
                Jedis jedis = jedisPool.getResource();
                try {
                    String key = null;
                    switch (type){
                        case "seckill":
                            key=calKey(seckillId);
                            break;
                        case "inventory":
                            key=calInventoryKey(seckillId);
                            break;
                        case "optSeckillLock":
                            key=calLockKey(seckillId, phone);
                            break;
                        case "repeatSeckill":
                            key=calRepeatSeckillKey(seckillId);
                            break;
                    }
                    Boolean result = jedis.exists(key);
                    return result;
                } finally {
                    jedis.close();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            return null;
    }

    public Boolean existsInventoryKey(long seckillId){
        return existsKey(seckillId, 0L, "inventory");
    }
    public String delKey(long seckillId, long phone, String type){
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = null;
                switch (type){
                    case "seckill":
                        key=calKey(seckillId);
                        break;
                    case "inventory":
                        key=calInventoryKey(seckillId);
                        break;
                    case "optSeckillLock":
                        key=calLockKey(seckillId, phone);
                        break;
                    case "repeatSeckill":
                        key=calRepeatSeckillKey(seckillId);
                        break;
                }
                jedis.del(key);
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 获取键
     *  * 在redis中存储的都是字节码：
     *  * key = 字节码
     *  * value = 字节码
     * @param seckillId
     * @return
     */
    private String calKey(long seckillId) {
        String key = "seckill:" + seckillId;
        return key;
    }
    private String calInventoryKey(long seckillId) {
        String key = "seckillInventory:" + seckillId;
        return key;
    }
    /**
     * 构建秒杀中Redis分布式锁的键 Key
     */
    private String calLockKey(long seckillId, long phone) {
        String key = "optSeckill:" + seckillId + "+" + phone;
        return key;
    }
    private String calRepeatSeckillKey(long seckillId) {
        String key = "optRepeatSeckillSet:" + seckillId;
        return key;
    }


    public Seckill getSeckill(long seckillId) {
        //redis操作逻辑
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calKey(seckillId);
                // 序列化问题 jvm-serialization
                //并没有实现内部序列化操作
                //get -> byte[] -> 反序列化 -> Object(Seckill)
                //采用自定义序列化
                //protostuff:pojo
                byte[] bytes = jedis.get(key.getBytes());
                if (bytes != null) {
                    // 构建一个空对象
                    Seckill seckill = schema.newMessage();
                    // bytes 反序列化为 Seckill对象
                    ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
                    return seckill;
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public String setSeckill(Seckill seckill) {
        //set Object(Seckill)->序列化->byte[]
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calKey(seckill.getSeckillId());
                // Seckill对象 序列化 bytes
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill
                        , schema
                        , LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                int timeout = 60 * 60; // 保留的时间 1个小时
                // 成功则返回 "OK"
                // 否则返回 错误信息
                String result = jedis.setex(key.getBytes(), timeout, bytes);
                return result;
            } finally {
                jedis.close();
                ;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    public Long getInventory(long seckillId) {
        //redis操作逻辑
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calInventoryKey(seckillId);
                Long number = Long.parseLong(jedis.get(key));
                return number;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public String setInventory(long seckillId, long number) {
        //set Object(Seckill)->序列化->byte[]
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calInventoryKey(seckillId);
                // Seckill对象 序列化 bytes
                int timeout = 60 * 60; // 保留的时间 1个小时
                // 成功则返回 "OK"
                // 否则返回 错误信息
                String result = jedis.setex(key, timeout, String.valueOf(number));
                return result;
            } finally {
                jedis.close();
                ;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /***
     * 自减一
     * @param seckillId
     * @return
     */
    public Long decrInventory(long seckillId) {
        return operateInventory(seckillId, -1);
    }

    /***
     * 自增一
     * @param seckillId
     * @return
     */
    public Long incrInventory(long seckillId) {
        return operateInventory(seckillId, 1);
    }

    /****
     * 自增自减操作
     * @param seckillId
     * @param increment 负数 ： 自减 ； 正数： 自增
     * @return 操作之后对应的值
     */
    public Long operateInventory(long seckillId, int increment) {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calInventoryKey(seckillId);
                long now_val = jedis.incrBy(key, increment);
                return now_val;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }



    /**
     * Redis 实现的锁： 通过seckillId, phone作为键，设置锁
     *
     * @param seckillId
     * @param phone
     * @return
     */
    public String setSeckillLock(long seckillId, long phone) {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calLockKey(seckillId, phone);
                SetParams setParams = SetParams.setParams().nx().px(500);
                String result = jedis.set(key, String.valueOf(seckillId), setParams);
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Redis 实现的锁： 通过seckillId, phone作为键，释放锁
     *
     * @param seckillId
     * @param phone
     * @return
     */
    public Long releaseSeckillLock(long seckillId, long phone) {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calLockKey(seckillId, phone);
                long result = jedis.del(key);
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }



    /***
     * 判断用户是否已经进行了秒杀
     * @param seckillId 秒杀商品id
     * @param phone 用户账号
     * @return
     */
    public Boolean getUserSeckillState(long seckillId, long phone) {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calRepeatSeckillKey(seckillId);
                boolean result = jedis.sismember(key, String.valueOf(phone));
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /***
     * 添加参与秒杀的用户到Redis的Set中
     * @param seckillId
     * @param phone
     * @return 已经存在了，返回 0， 否则返回 1
     */
    public Long addSeckillUser(long seckillId, long phone) {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calRepeatSeckillKey(seckillId);
                long result = jedis.sadd(key, String.valueOf(phone));
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /****
     * 到从Redis的Set中删除对应的秒杀用户
     * @param seckillId
     * @param phone
     * @return 向set中 添加记录，如果已经存在，返回0 否则返回1
     */
    public Long delSeckillUser(long seckillId, long phone) {
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = calRepeatSeckillKey(seckillId);
                long result = jedis.srem(key, String.valueOf(phone));
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /***
     * lua + redis 解决秒杀高并发
     */
    private String getAtomLuaScript(){
        StringBuilder luasb = new StringBuilder();
        luasb.append("if (redis.call('exists', KEYS[1]) == 1) then"); // 查询键是否存在
        luasb.append("    local stock = tonumber(redis.call('get', KEYS[1]));"); // tonumber( * ) -> 转化为数字 // 获取值
        /*
        luasb.append("    if (stock == -1) then");
        luasb.append("        return 1;");
        luasb.append("    end;");
        */
        luasb.append("    if (stock > 0) then");
        luasb.append("        local stock = tonumber(redis.call('incrby', KEYS[1], -1));"); // 4.2 Redis 减库存
        luasb.append("        redis.call('sadd', KEYS[2], ARGV[1]);");      // 将秒杀用户添加到Redis中，防止重复秒杀
        luasb.append("        return stock;");
        luasb.append("    end;");
        luasb.append("    return -1;");     // 返回-1 ： stock <= 0 商品卖光了
        luasb.append("end;");
        luasb.append("return -2;");         // 返回-2 ： KEYS[1] 不存在
        return luasb.toString();
    }

    public Long callLuaScriptToDecrInventory(long seckillId, long phone) {
        /*
            // 4. 减库存操作：
            // 4.1 到 4.2 的过程不是原子操作，可能导致潜在的超卖问题
            Long val = redisDao.getInventory(seckillId);
            // 4.1 获取当前库存
            if(val <= 0){
                throw new RepeatKillException("秒杀商品结束了");
            }
            Long updateVal = redisDao.decrInventory(seckillId);
            // 4.2 Redis 减库存
            Long userSeckillState = redisDao.addSeckillUser(seckillId, userPhone);  // 将秒杀用户添加到Redis中，防止重复秒杀
            使用 Lua 解决该问题！！！
        */
        String luaScript = getAtomLuaScript();
        // Lua脚本里的KEYS参数
        List<String> keys = new ArrayList<>();
        String key1 = calInventoryKey(seckillId);
        String key2 = calRepeatSeckillKey(seckillId);
        keys.add(key1);
        keys.add(key2);
        // Lua脚本里的ARGV参数
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(phone));
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                Long result = (Long)jedis.eval(luaScript, keys, args);
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
