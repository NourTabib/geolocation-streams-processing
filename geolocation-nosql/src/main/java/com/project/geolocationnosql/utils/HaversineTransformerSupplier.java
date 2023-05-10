package com.project.geolocationnosql.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.geolocationnosql.entity.enrichedVehiculePosition;
import com.project.geolocationnosql.entity.vehiculePosition;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HaversineTransformerSupplier implements TransformerSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(HaversineTransformerSupplier.class);

    public String busPositionStoreName;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HaversineTransformerSupplier(String busPositionStoreName) {
        this.busPositionStoreName = busPositionStoreName;
    }

    @Override
    public Transformer<String, String, KeyValue<String, String>> get() {
        return new Transformer<String, String, KeyValue<String, String>>() {

            private KeyValueStore<String, String> busPositionStore;

            @SuppressWarnings("unchecked")
            @Override
            public void init(final ProcessorContext context) {
                busPositionStore = (KeyValueStore<String, String>) context.getStateStore(busPositionStoreName);
            }

            @Override
            public KeyValue<String, String> transform(final String dummy, final String value) {
                vehiculePosition busPosition ;
                String prevPositionStringFromDb;
                vehiculePosition previousBusPosition;
                try{
                    busPosition = objectMapper.readValue(value,vehiculePosition.class);
                    prevPositionStringFromDb = busPositionStore.get(String.valueOf(busPosition.getId()));
                    previousBusPosition = objectMapper.readValue(prevPositionStringFromDb,vehiculePosition.class);
                }catch (Exception e){
                    logger.error("",e);
                    return null;
                }
                enrichedVehiculePosition enrichedVehiculPosition = enrichedVehiculePosition.builder()
                        .id(busPosition.getId())
                        .location(busPosition.getLocation())
                        .milesPerHour(0)
                        .bearing(busPosition.getBearing())
                        .timestamp(busPosition.getTimestamp())
                        .build();

                // if there is a previous location for that bus ID, calculate the speed based on its previous position/timestamp.
                if (previousBusPosition != null) {

                    // calculate distance and time between last two measurements
                    HaversineDistanceCalculator haversineDistanceCalculator = new HaversineDistanceCalculator();
                    double distanceKm = haversineDistanceCalculator.calculateDistance(
                            previousBusPosition.getLocation().getLat(),
                            previousBusPosition.getLocation().getLon(),
                            busPosition.getLocation().getLat(),
                            busPosition.getLocation().getLon()); // distance is in kilometers

                    long timeDeltaMillis = busPosition.getTimestamp() - previousBusPosition.getTimestamp();
                    double milesPerHour = calculateMilesPerHour(distanceKm * 1000, timeDeltaMillis / 1000);
                    enrichedVehiculPosition.setMilesPerHour(milesPerHour);
                }
                busPositionStore.put(String.valueOf(busPosition.getId()), value);
                String enrichedVehiculPositionString;
                try{
                    enrichedVehiculPositionString = objectMapper.writeValueAsString(enrichedVehiculPosition);
                }catch (Exception e){
                    return null;
                }
                return new KeyValue<>(String.valueOf(busPosition.getId()), enrichedVehiculPositionString);

            }

            @Override
            public void close() {
            }
        };
    }
    public static double calculateMilesPerHour(double meters, long seconds) {
        if (seconds == 0){
            return 0;
        } else {
            double metersPerSecond = meters / seconds;
            return metersPerSecond * 2.2369;
        }
    }
}