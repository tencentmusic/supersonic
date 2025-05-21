package com.tencent.supersonic;

import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    public static final String brokerList =
            "113.45.129.200:19092,113.45.129.21:19092,110.41.159.123:19092";

    @Bean
    public ProducerFactory<String, ChatParseReq> producerFactory() {
        Map<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "supersonic-kafka-producer-client");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "com.tencent.supersonic.ChatParseReqSerializer");
        // kerberos
        configProps.put("security.protocol", "SASL_PLAINTEXT");
        configProps.put("sasl.mechanism", "GSSAPI");
        configProps.put("sasl.kerberos.service.name", "kafka");
        configProps.put("sasl.jaas.config", "com.sun.security.auth.module.Krb5LoginModule required "
                + "useKeyTab=true "
                + "keyTab=\"D:/Java_workspace/KafkaLearning/kafkaProject/kerberos_files/kafkauserService.keytab\" "
                + "storeKey=true " + "useTicketCache=false "
                + "principal=\"kafka_user@EXAMPLE.COM\";");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @PostConstruct
    public void setKrb5Config() {
        System.setProperty("java.security.krb5.conf",
                "D:\\Java_workspace\\KafkaLearning\\kafkaProject\\kerberos_files\\krb5.ini");
    }

    @Bean("supersonicKafkaTemplate")
    public KafkaTemplate<String, ChatParseReq> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
