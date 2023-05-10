package com.project.geolocationnosql.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "vehiculePosition")
public class vehiculePosition {
    private String _id;
    private String id;
    private long timestamp;
    private Location location;
    private double bearing;
}
