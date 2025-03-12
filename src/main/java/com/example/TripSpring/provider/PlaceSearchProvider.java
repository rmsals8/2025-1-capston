package com.example.TripSpring.provider;

import com.example.TripSpring.dto.domain.PlaceInfo;

public interface PlaceSearchProvider {
    PlaceInfo searchPlace(String placeName, double lat, double lng);
}