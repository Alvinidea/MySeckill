package hcf.seckill.dao;

import hcf.seckill.entity.Seckill;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author hechaofan
 * @date 2022/3/16 18:33
 */

public interface SeckillDao {

    /**
     * 减库存
     * @param seckillId
     * @param killTime
     * @return 影响行数 > 1，表示更新记录 的 行数
     */
    int reduceNumber(@Param("seckillId") long seckillId,
                     @Param("killTime") Date killTime);

    /**
     * 根据id查询秒杀对象
     * @param seckillId
     * @return
     */
    Seckill queryById(long seckillId);

    /**
     * 根据偏移量查询秒杀商品列表
     * @param offset
     * @param limit
     * @return
     */
    List<Seckill> queryAll(@Param("offset") int offset,
                           @Param("limit") int limit);

}
