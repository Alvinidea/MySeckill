package hcf.seckill.dao;

import hcf.seckill.entity.SuccessKilled;
import org.apache.ibatis.annotations.Param;


/**
 * @author hechaofan
 * @date 2022/3/16 18:37
 */

public interface SuccessKilledDao {

    /**
     * 插入购买明细，联合主键过滤重复
     * @param seckillId
     * @param userPhone
     * @return  =0，表示失败
     * 插入的行数
     */
    int insertSuccessKilled(@Param("seckillId") long seckillId,
                            @Param("userPhone") long userPhone);

    /**
     * 根据id查询 SuccessKilled 并携带秒杀产品对象实体
     * @param seckillId
     * @return
     */
    SuccessKilled queryByIdWithSeckill(@Param("seckillId") long seckillId,
                                       @Param("userPhone") long userPhone);
}
