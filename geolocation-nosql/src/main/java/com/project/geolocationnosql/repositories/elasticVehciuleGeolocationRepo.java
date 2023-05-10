package com.project.geolocationnosql.repositories;

import com.project.geolocationnosql.index.VehiculeGeolocation;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface elasticVehciuleGeolocationRepo extends Repository<VehiculeGeolocation,String> {
    VehiculeGeolocation save(VehiculeGeolocation entity);
    Optional<VehiculeGeolocation> findById(String primaryKey);
    Iterable<VehiculeGeolocation> findAll();
}
