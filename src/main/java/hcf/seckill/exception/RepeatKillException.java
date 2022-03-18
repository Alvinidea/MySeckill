package hcf.seckill.exception;

/**
 * 重复秒杀异常（运行时异常）
 * @author hechaofan
 * @date 2022/3/17 14:55
 */
public class RepeatKillException extends SeckillException{

    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
