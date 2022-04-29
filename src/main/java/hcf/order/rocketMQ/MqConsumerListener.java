package hcf.order.rocketMQ;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.shade.com.alibaba.fastjson.JSON;
import hcf.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 *  * 秒杀系统 - 生产消息
 *  * 订单系统 - 消费消息
 * @author hechaofan
 * @date 2022/4/24 15:09
 *
 * MqConsumerListener 相当于 controller，接受的数据来自于
 */
public class MqConsumerListener implements MessageListenerConcurrently {

    private static final Logger log = LoggerFactory.getLogger(MqConsumerListener.class);

    @Autowired
    private OrderService orderService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        for (MessageExt messageExt : msgs) {
            log.info("# {}", messageExt);
            try {
                String body = new String(messageExt.getBody(), "UTF-8");
                Map<String, Object> map = JSON.parseObject(body, Map.class);
                long seckillId = Long.valueOf(map.get("seckillId").toString());
                long phone= Long.valueOf(map.get("userPhone").toString());
                System.out.println(seckillId + " - " + phone);
                orderService.consumeSeckillMsg(seckillId, phone);
                log.info("# {}", body);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (Exception e){
                log.info(e.getMessage());
            }
            //System.out.println(messageExt.toString());
            //System.out.println(new String(messageExt.getBody()));
        }
        log.info("# getDelayLevelWhenNextConsume={} , getMessageQueue={} , getDelayLevelWhenNextConsume={}"
                , context.getDelayLevelWhenNextConsume()
                , context.getMessageQueue()
                , context.getDelayLevelWhenNextConsume());
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
