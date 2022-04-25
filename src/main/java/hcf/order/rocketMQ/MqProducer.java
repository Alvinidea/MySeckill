package hcf.order.rocketMQ;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;


/**
 * 只是在本项目实现中，将 MqProducer，MqConsumerListener 放在了一起而已
 * 秒杀系统 - 生产消息
 * 订单系统 - 消费消息
 * @author hechaofan
 * @date 2022/4/24 15:09
 */
public class MqProducer {
    private final static Logger logger = LoggerFactory.getLogger(MqProducer.class);
    private static DefaultMQProducer defaultMQProducer;
    private String producerGroup;
    private String namesrvAddr;

    public String getProducerGroup() {
        return producerGroup;
    }

    public void setProducerGroup(String producerGroup) {
        this.producerGroup = producerGroup;
    }

    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public void init() throws MQClientException{
        // 参数信息
        logger.info("DefaultMQProducer initialize!");
        logger.info(producerGroup);
        logger.info(namesrvAddr);
        // 初始化
        defaultMQProducer = new DefaultMQProducer(producerGroup);
        defaultMQProducer.setNamesrvAddr(namesrvAddr);
        defaultMQProducer.setInstanceName(String.valueOf(System.currentTimeMillis()));
        defaultMQProducer.start();
        logger.info("DefaultMQProudcer start success!");
    }
    public void destroy() {
        defaultMQProducer.shutdown();
    }

    public DefaultMQProducer getDefaultMQProducer() {
        return defaultMQProducer;
    }

    /**
     * rocketmq发送消息方法
     *
     * @param topic 组名
     * @param tagName 同一个topic下的不同 分支,同一个消费者只能取一个组的下的不同的tag分支
     * @param key 保持唯一
     * @param msgBody 消息体
     * @return
     */
    public static void sendMsgIntime(String topic, String tagName, String key, String msgBody) {
        Message msg = null;
        try {
            msg = new Message(topic,tagName,key,msgBody.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            String result = defaultMQProducer.send(msg).toString();
            logger.info("send rockmq topic:" + topic + " " + result);
        } catch (Exception e) {
            logger.error("send rockmq error topic:" + topic + new String(msgBody), e);
        }
    }
}
