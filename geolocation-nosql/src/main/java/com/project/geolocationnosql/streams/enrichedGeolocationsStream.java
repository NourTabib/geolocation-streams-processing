package com.project.geolocationnosql.streams;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.geolocationnosql.entity.enrichedVehiculePosition;
import com.project.geolocationnosql.entity.vehiculePosition;
import com.project.geolocationnosql.utils.HaversineDistanceCalculator;
import com.project.geolocationnosql.utils.HaversineTransformerSupplier;
import com.uber.h3core.H3Core;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Component
public class enrichedGeolocationsStream {
    private static final Serde<String> STRING_SERDE = Serdes.String();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RocksDB  rocksDb = this.getRocksDb();
    private File dbDir = null;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    void buildStream(StreamsBuilder streamsBuilder){
        final KStream<String,String>
                positionStream = streamsBuilder
                .stream(
                        "raw-vehicule-positions",
                        Consumed.with(STRING_SERDE,STRING_SERDE)
                );
        positionStream
//                .peek(
//                (key,value) -> {logger.info("key :" + key + " value : " + enrichBusPosition(value));}
//                )
                .mapValues(value -> {
                    return enrichBusPosition(value);
                }
                )
        .to("enriched-vehicule-positions", Produced.with(STRING_SERDE,STRING_SERDE));
    }
    private String enrichBusPosition(String value){
        vehiculePosition vehiculePosition = new vehiculePosition();
        try {
            vehiculePosition = objectMapper.readValue(value, vehiculePosition.class);
        }catch (Exception e){
            logger.error("",e);
            return null;
        }
        String vehiculeId = vehiculePosition.getId();
        vehiculePosition previousVehiculePosition = new vehiculePosition();
        String previousVehiculePositionString;
        try{
            byte[] lastVehiculePosition = rocksDb.get(vehiculeId.getBytes());
            if(lastVehiculePosition != null){
                previousVehiculePositionString = new String(lastVehiculePosition);
                previousVehiculePosition = objectMapper.readValue(previousVehiculePositionString, vehiculePosition.class);
                System.out.println("prev :" +previousVehiculePositionString);
                System.out.println("current : " + value);
                HaversineDistanceCalculator haversineDistanceCalculator = new HaversineDistanceCalculator();
                double distance = haversineDistanceCalculator.calculateDistance(
                        previousVehiculePosition.getLocation().getLat(),
                        previousVehiculePosition.getLocation().getLon(),
                        vehiculePosition.getLocation().getLat(),
                        vehiculePosition.getLocation().getLon()
                );
                long deltaTime = vehiculePosition.getTimestamp() - previousVehiculePosition.getTimestamp();
                double milesPerHour = HaversineTransformerSupplier.calculateMilesPerHour(distance,deltaTime);
                logger.info("distance :"+distance+"time :" +deltaTime+ "speed :"+milesPerHour);
                H3Core h3 = H3Core.newInstance();
                String hexAddr = h3.geoToH3Address(
                        vehiculePosition.getLocation().getLat(),
                        vehiculePosition.getLocation().getLon(),
                        12
                );
                enrichedVehiculePosition enrichedVehiculePos = enrichedVehiculePosition.builder()
                        .id(vehiculePosition.getId())
                        .location(vehiculePosition.getLocation())
                        .milesPerHour(milesPerHour)
                        .bearing(vehiculePosition.getBearing())
                        .h3(hexAddr)
                        .timestamp(vehiculePosition.getTimestamp())
                        .build();
//                logger.info("after enriched "+enrichedVehiculePos.toString());
                try{
                    rocksDb.put(vehiculeId.getBytes(),value.getBytes());
                }catch (Exception e){
                    logger.error("",e);
                }
                return objectMapper.writeValueAsString(enrichedVehiculePos);
            }
            try{
                rocksDb.put(vehiculeId.getBytes(),value.getBytes());
            }catch (Exception e){
                logger.error("",e);
            }
        }catch (Exception e){
            logger.error("",e);
        }
        return null;
    }
    private RocksDB getRocksDb(){
        RocksDB.loadLibrary();
        final Options options = new Options().setCreateIfMissing(true);
        dbDir = new File("/tmp/rocks-db", "postion-data");
        try{
            Files.createDirectories(dbDir.getParentFile().toPath());
            Files.createDirectories(dbDir.getAbsoluteFile().toPath());
            RocksDB db =RocksDB.open(options,dbDir.getAbsolutePath());
            logger.info("RocksDB initialized and ready to use");
            return db;
        }catch(Exception e){
            logger.error("",e);
            return null;
        }

    }
}
