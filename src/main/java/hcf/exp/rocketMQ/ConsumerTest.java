package hcf.exp.rocketMQ;



import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.UtilAll;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.remoting.common.RemotingHelper;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/*

<!--配置消息队列中间件 依赖包-->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>5.0.0-PREVIEW</version>
</dependency>

*/

/**
 * @author hechaofan
 * @date 2022/3/22 18:43
 */
public class ConsumerTest {
    public static void main(String[] args) throws InterruptedException, MQClientException {

        // 实例化消费者， Push : MQ主动推送信息
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("HCFGroup");

        // 设置NameServer的地址  http://10.16.65.76:9999/
        consumer.setNamesrvAddr("10.16.65.76:9876");
        // 设置广播消费策略，（默认是 集群消费  MessageModel.BROADCASTING）
        // consumer.setMessageModel(MessageModel.BROADCASTING);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        // consumer.setConsumeTimestamp(UtilAll.timeMillisToHumanString3(System.currentTimeMillis() - 1800000L));
        // 订阅一个或者多个Topic，以及Tag来过滤需要消费的消息
        consumer.subscribe("MyTopic", "*");
        // 注册回调实现类来处理从broker拉取回来的消息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                int i = 1;
                for (MessageExt msg : msgs) {
                    String msgStr = null;
                    try {
                        msgStr = new String(msg.getBody(), RemotingHelper.DEFAULT_CHARSET);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    SimpleDateFormat sd = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
                    Date date = new Date(msg.getStoreTimestamp());
                    System.out.printf("%d : %s Receive New Messages: %s | time = %s %n"
                            , i
                            , Thread.currentThread().getName()
                            , msgStr
                            ,sd.format(date));
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

