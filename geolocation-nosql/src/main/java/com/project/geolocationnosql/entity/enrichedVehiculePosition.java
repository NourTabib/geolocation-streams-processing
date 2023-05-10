package com.project.geolocationnosql.entity;

import com.project.geolocationnosql.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "enrichedVehiculePosition")
public class enrichedVehiculePosition {
    private String _id;
    private String id;
    private long timestamp;
    private Location location;
    private double bearing;
    private double milesPerHour;
    private String h3;
}
