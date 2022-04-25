package hcf.seckill.controller;

import com.alibaba.rocketmq.shade.com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.RateLimiter;
import hcf.order.rocketMQ.MqProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    public String testRateLimiter(HttpServletResponse response, HttpServletRequest request, HttpSession session, Integer id){


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


    /*
    * RocketMQ 的测试
    * */
    @Autowired
    private MqProducer mqProducer;

    @ResponseBody
    @RequestMapping(value = "/testRocketMQ")
    public String testRocketMQ(){
        String consumerStr = "666";
        //发送消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("name", "hcf");
        msg.put("phone", "18400001111");
        MqProducer.sendMsgIntime("OrderTopic",
                "order",
                "",
                JSON.toJSONString(msg));
        return "消费信息:" +consumerStr;

    }

}
