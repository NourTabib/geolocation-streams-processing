package com.project.geolocationnosql;

import org.elasticsearch.client.indices.CreateIndexRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GeolocationNosqlApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeolocationNosqlApplication.class, args);
	}

}
