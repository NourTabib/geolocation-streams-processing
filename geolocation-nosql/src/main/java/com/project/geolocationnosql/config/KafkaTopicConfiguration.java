package com.project.geolocationnosql.config;


import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfiguration {

    @Bean
    public KafkaAdmin kafkaAdmin(){
        Map<String,Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        return new KafkaAdmin(configs);
    }
    @Bean
    public NewTopic rawPositions(){
        return new NewTopic("raw-vehicule-positions",4,(short) 1);
    }
    @Bean
    public NewTopic encrichedPositions(){
        return new NewTopic("enriched-vehicule-positions",4,(short) 1);
    }
}
