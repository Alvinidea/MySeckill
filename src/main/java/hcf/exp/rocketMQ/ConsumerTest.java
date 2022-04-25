package hcf.exp.rocketMQ;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.remoting.common.RemotingHelper;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * @author hechaofan
 * @date 2022/3/22 18:43
 */
public class ConsumerTest {
    public static void main(String[] args) throws InterruptedException, MQClientException {

        // 实例化消费者 : DefaultMQPushConsumer 的默认消费方式，集群消费。 Push : MQ主动推送信息
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("WP");

        // 设置NameServer的地址
        consumer.setNamesrvAddr("10.16.65.76:9876");
        consumer.start();
        // 订阅一个或者多个Topic，以及Tag来过滤需要消费的消息
        consumer.subscribe("HCF", "*");
        // 注册回调实现类来处理从broker拉取回来的消息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> msgs
                    , ConsumeConcurrentlyContext context) {
                int i = 1;
                for (MessageExt msg : msgs) {
                    String msgStr = null;
                    try {
                        msgStr = new String(msg.getBody(), RemotingHelper.DEFAULT_CHARSET);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    System.out.printf("%d : %s Receive New Messages: %s %n"
                            , i
                            , Thread.currentThread().getName()
                            , msgStr);
                    i++;
                }
                       // 标记该消息已经被成功消费
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 启动消费者实例
        consumer.start();
        System.out.printf("Consumer Started.%n");
    }
}

