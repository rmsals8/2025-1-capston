// PlaceSearchConfig.java
package com.example.TripSpring.config;

import com.example.TripSpring.provider.FoursquarePlaceSearchProvider;
import com.example.TripSpring.provider.GooglePlaceSearchProvider;
import com.example.TripSpring.provider.NaverPlaceSearchProvider;
import com.example.TripSpring.provider.TmapPlaceSearchProvider;
import com.example.TripSpring.provider.PlaceSearchProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PlaceSearchConfig {

    @Bean
    public List<PlaceSearchProvider> placeSearchProviders(
            FoursquarePlaceSearchProvider foursquareProvider,
            GooglePlaceSearchProvider googleProvider,
            NaverPlaceSearchProvider naverProvider,
            TmapPlaceSearchProvider tmapProvider) {
        return List.of(
            foursquareProvider,  // First priority
            googleProvider,      // Second priority
            naverProvider,       // Third priority
            tmapProvider        // Fourth priority
        );
    }
}