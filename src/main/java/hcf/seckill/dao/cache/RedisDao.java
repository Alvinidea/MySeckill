package hcf.seckill.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import hcf.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author hechaofan
 * @date 2022/3/19 15:53
 *
 * 在redis中存储的都是字节码：
 * key = 字节码
 * value = 字节码
 */
public class RedisDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JedisPool jedisPool;

    // 序列化工具(优势：空间小， 速度快)
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    public RedisDao(String ip, int port) {
        this.jedisPool = new JedisPool(ip, port);
    }



    public Seckill getSeckill(long seckillId){
        //redis操作逻辑
        try{
            Jedis jedis = jedisPool.getResource();
            try{
                String key = "seckill:"+seckillId;
                // 序列化问题 jvm-serialization
                //并没有实现内部序列化操作
                //get -> byte[] -> 反序列化 -> Object(Seckill)
                //采用自定义序列化
                //protostuff:pojo
                byte[] bytes = jedis.get(key.getBytes());
                if(bytes != null){
                    // 构建一个空对象
                    Seckill seckill = schema.newMessage();
                    // bytes 反序列化为 Seckill对象
                    ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
                    return seckill;
                }
            }finally {
                jedis.close();
            }
        }catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    public String setSeckill(Seckill seckill){
        //set Object(Seckill)->序列化->byte[]
        try{
            Jedis jedis = jedisPool.getResource();
            try{
                String key = "seckill:"+seckill.getSeckillId();
                // Seckill对象 序列化 bytes
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill
                        , schema
                        , LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                int timeout = 60*60; // 保留的时间 1个小时
                // 成功则返回 "OK"
                // 否则返回 错误信息
                String result = jedis.setex(key.getBytes(), timeout, bytes);
                return result;
            }finally{
                jedis.close();;
            }
        }catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        return null;
    }


}
