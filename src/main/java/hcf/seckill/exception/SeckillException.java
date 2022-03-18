package hcf.seckill.exception;

/**
 * @author hechaofan
 * @date 2022/3/17 14:58
 */
public class SeckillException extends RuntimeException{

    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
