package hcf.seckill.service;

import hcf.seckill.dto.SeckillExecution;
import hcf.seckill.exception.RepeatKillException;
import hcf.seckill.exception.SeckillCloseException;
import hcf.seckill.exception.SeckillException;

/**
 * @author hechaofan
 * @date 2022/3/21 16:11
 */
public interface OptSeckillService {
    /**
     * 执行秒杀操作
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    SeckillExecution executeSeckill(long seckillId,
                                    long userPhone,
                                    String md5)
            throws SeckillException, RepeatKillException, SeckillCloseException;
}
