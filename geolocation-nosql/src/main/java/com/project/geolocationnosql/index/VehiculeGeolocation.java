package com.project.geolocationnosql.index;

import com.project.geolocationnosql.entity.enrichedVehiculePosition;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document(indexName = "geolocations-index")
public class VehiculeGeolocation {

    String vehiculeNum;
    @Id
    String id;
    Long timestamp;
    GeoPoint location;
    Double bearing;
    Double milesPerHour;
    String h3;
    public VehiculeGeolocation(enrichedVehiculePosition entry){
        this.vehiculeNum = entry.getId();
        this.location = new GeoPoint(entry.getLocation().getLat(),entry.getLocation().getLon());
        this.id = UUID.randomUUID().toString();
        this.timestamp = entry.getTimestamp();
        this.bearing = entry.getBearing();
        this.milesPerHour = entry.getMilesPerHour();
        this.h3 = entry.getH3();
    }
}
