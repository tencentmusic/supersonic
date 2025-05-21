package com.tencent.supersonic;

import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tencent.supersonic.chat.server.service.ChatQueryService;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChatKafkaConsumer {
    @Autowired
    private ChatQueryService chatQueryService;

    public static final String brokerList =
            "113.45.129.200:19092,113.45.129.21:19092,110.41.159.123:19092";
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private static final String TOPIC = "supersonic";
    public static final String groupId = "supersonic-consumer-group-0";

    @PostConstruct
    public void startConsumer() {
        new Thread(this::consume).start();
    }

    public void consume() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "com.tencent.supersonic.ChatParseReqDeserializer");
//        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
//        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ChatParseReq.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Kerberos Authentication
        props.put("security.protocol", "SASL_PLAINTEXT");
        props.put("sasl.mechanism", "GSSAPI");
        props.put("sasl.kerberos.service.name", "kafka");
        props.put("sasl.jaas.config",
                "com.sun.security.auth.module.Krb5LoginModule required " +
                        "useKeyTab=true " +
                        "keyTab=\"D:/Java_workspace/KafkaLearning/kafkaProject/kerberos_files/kafkauserService.keytab\" " +
                        "storeKey=true " +
                        "useTicketCache=false " +
                        "principal=\"kafka_user@EXAMPLE.COM\";");

        try (KafkaConsumer<String, ChatParseReq> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            while (isRunning.get()) {
                ConsumerRecords<String, ChatParseReq> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, ChatParseReq> record : records) {
                    ChatParseReq chatParseReq = record.value();

                    try {
                        // 处理业务请求
                        Object result = chatQueryService.parse(chatParseReq);
                        System.out.println("Kafka Consumer处理完成: " + result);
                    } catch (Exception e) {
                        System.err.println("Kafka Consumer处理失败: " + e.getMessage());
                    }

                    // 每秒最多处理10条 => 控制速率为每100ms处理1条
                    Thread.sleep(100);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
