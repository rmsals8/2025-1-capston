package com.example.TripSpring.service;

import com.example.TripSpring.dto.domain.PlaceInfo;
import com.example.TripSpring.provider.PlaceSearchProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationInfoService {
    private final List<PlaceSearchProvider> searchProviders;
    private final double defaultLat = 37.5665;
    private final double defaultLng = 126.9780;

    public PlaceInfo getPlaceInfo(String placeName) {
        log.debug("Searching place info for: {}", placeName);
        
        // Try each provider in sequence
        for (PlaceSearchProvider provider : searchProviders) {
            try {
                PlaceInfo placeInfo = provider.searchPlace(placeName, defaultLat, defaultLng);
                if (placeInfo != null) {
                    log.debug("Found place info using provider: {}", provider.getClass().getSimpleName());
                    return placeInfo;
                }
            } catch (Exception e) {
                log.error("Error with provider {}: {}", provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        // If all providers fail, return default
        log.warn("No provider found info for: {}. Using default.", placeName);
        return createDefaultPlaceInfo(placeName);
    }

    private PlaceInfo createDefaultPlaceInfo(String placeName) {
        return new PlaceInfo(
            placeName,          // id
            placeName,          // name
            true,              // isOpen
            LocalTime.of(9, 0), // openTime
            LocalTime.of(22, 0),// closeTime
            0.5,               // crowdLevel
            "",                // address
            "",                // phoneNumber
            "",                // category
            0.0,               // rating
            LocalTime.of(1, 0) // averageVisitDuration
        );
    }
}