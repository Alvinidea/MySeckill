package hcf.order.service;

import com.alibaba.rocketmq.shade.com.alibaba.fastjson.JSON;
import hcf.order.rocketMQ.MqProducer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hechaofan
 * @date 2022/4/24 13:50
 */
public class OrderServiceImpl implements OrderService {


    @Autowired
    private MqProducer mqProducer;

    public void sendMsg(){
        Map<String, Object> msg = new HashMap<>();
        msg.put("carNum", "浙A888888");
        msg.put("code", "778882222");
        MqProducer.sendMsgIntime("OrderTopic",
                "order",
                "",
                JSON.toJSONString(msg));
    }

}
