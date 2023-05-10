package com.project.geolocationnosql.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.geolocationnosql.entity.*;
import com.google.transit.realtime.GtfsRealtime;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@EnableScheduling
public class FeedPoller{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String rtdUrl = "https://www.rtd-denver.com/files/gtfs-rt/VehiclePosition.pb";
//    @Autowired
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    FeedPoller(KafkaTemplate<String, String> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }
    @Scheduled(cron = "*/32 * * * * *")
    private void getVehiculePosition(){
        try {
            Unirest.get(rtdUrl).thenConsume(
                    response -> {
                        try{
                            InputStream stream = response.getContent();
                            GtfsRealtime.FeedMessage feeds = GtfsRealtime.FeedMessage.parseFrom(stream);
                            for(GtfsRealtime.FeedEntity feed:feeds.getEntityList()){
                                GtfsRealtime.VehiclePosition vehiculPosition = feed.getVehicle();
                                Location loc = Location.builder()
                                        .lat(vehiculPosition.getPosition().getLatitude())
                                        .lon(vehiculPosition.getPosition().getLongitude())
                                        .build();
                                vehiculePosition vehiculePositionAvro = vehiculePosition.builder()
                                        .id(
                                                vehiculPosition.getVehicle().getId()
                                        )
                                        .location(loc)
                                        .timestamp(
                                                vehiculPosition.getTimestamp()
                                        )
                                        .bearing(
                                                vehiculPosition.getPosition().getBearing()
                                        )
                                        .build();
                                String vehiculePositionJson = objectMapper.writeValueAsString(vehiculePositionAvro);
                                kafkaTemplate.send("raw-vehicule-positions",vehiculePositionAvro.getId().toString(),vehiculePositionJson);
                            }
                        }catch (Exception e){
                            logger.error("ERROR : ",e);
                        }
                    }
            );
        }catch (Exception e){
            logger.error("ERROR : ",e);
        }
    }
}
