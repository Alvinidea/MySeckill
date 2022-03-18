package hcf.seckill.exception;

/**
 * 秒杀关闭异常
 * @author hechaofan
 * @date 2022/3/17 14:57
 */
public class SeckillCloseException extends SeckillException{
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
