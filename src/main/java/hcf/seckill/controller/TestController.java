package hcf.seckill.controller;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 纯粹为了进行一些测试
 *
 *
 * @author hechaofan
 * @date 2022/3/24 19:48
 */

@Controller
@RequestMapping(value = "/test")
public class TestController {

    static AtomicInteger integer = new AtomicInteger(0);
    private RateLimiter rateLimiter = RateLimiter.create(10);
    @RequestMapping(value = "/sale")
    @ResponseBody
    public String testRateLimiter(Integer id){

        System.out.println();
        if(!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)){
            System.out.println(integer.incrementAndGet());
            System.out.println("当前请求被限流,直接抛弃，无法调用后续秒杀逻辑\n");
            return "failure .........";
        }
        //ArrayList
        return "test";
    }
    // https://blog.csdn.net/u011291072/article/details/107943586 令牌桶
}
