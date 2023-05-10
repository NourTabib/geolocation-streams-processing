package com.project.geolocationnosql.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.geolocationnosql.index.VehiculeGeolocation;
import com.project.geolocationnosql.repositories.elasticVehciuleGeolocationRepo;
import com.project.geolocationnosql.entity.enrichedVehiculePosition;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class enrichedGeolocationElasticSink {
    private static final Serde<String> STRING_SERDE = Serdes.String();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final elasticVehciuleGeolocationRepo elasticVehciuleGeolocationRep;

    public enrichedGeolocationElasticSink(elasticVehciuleGeolocationRepo elasticVehciuleGeolocationRepo) {
        this.elasticVehciuleGeolocationRep = elasticVehciuleGeolocationRepo;
    }

    @Autowired
    void buildSink(StreamsBuilder streamsBuilder){
        final KStream<String,String> sinkStream =
                streamsBuilder
                        .stream(
                                "enriched-vehicule-positions",
                                Consumed.with(STRING_SERDE,STRING_SERDE)
                        )
                        .peek(((key, value) -> {
                            try{
                                enrichedVehiculePosition record = objectMapper.readValue(value,enrichedVehiculePosition.class);
                                VehiculeGeolocation vehiculeGeolocation = new VehiculeGeolocation(record);
                                VehiculeGeolocation saved = elasticVehciuleGeolocationRep.save(vehiculeGeolocation);
//                                logger.info("Record inserted into ElasticSearch"+saved.toString());
                            }catch (Exception e){
                                logger.error("ERROR", e);
                            }
                        }));
    }
}
