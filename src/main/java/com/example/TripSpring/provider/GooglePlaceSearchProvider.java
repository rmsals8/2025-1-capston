package com.example.TripSpring.provider;

import com.example.TripSpring.dto.Place;
import com.example.TripSpring.dto.PlaceDetails;
import com.example.TripSpring.dto.PlaceDetailsResponse;
import com.example.TripSpring.dto.OpeningHours;
import com.example.TripSpring.dto.domain.PlaceInfo;
import com.example.TripSpring.service.PlacesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class GooglePlaceSearchProvider implements PlaceSearchProvider {
    private final PlacesService placesService;

    @Value("${app.api.google}")
    private String googleApiKey; 

    @Override
    public PlaceInfo searchPlace(String placeName, double lat, double lng) {
        try {
            return placesService.searchPlaces(placeName, lat, lng)
                .map(response -> {
                    if (response.getResults() != null && !response.getResults().isEmpty()) {
                        Place place = response.getResults().get(0);
                        return placesService.getPlaceDetails(place.getPlace_id())
                            .map(this::convertToPlaceInfo)
                            .block();
                    }
                    return null;
                })
                .block();
        } catch (Exception e) {
            log.error("Google Places API failed for place {}: {}", placeName, e.getMessage());
            return null;
        }
    }

    private PlaceInfo convertToPlaceInfo(PlaceDetailsResponse details) {
        PlaceDetails place = details.getResult();
        return new PlaceInfo(
            place.getPlace_id(),
            place.getName(),
            place.getOpening_hours() != null && place.getOpening_hours().getOpen_now(),
            parseGoogleOpeningHours(place.getOpening_hours()),
            parseGoogleClosingHours(place.getOpening_hours()),
            0.5, // default crowd level
            place.getFormatted_address(),
            place.getInternational_phone_number(),
            place.getTypes() != null && !place.getTypes().isEmpty() ? place.getTypes().get(0) : "",
            place.getRating() != null ? place.getRating() : 0.0,
            LocalTime.of(1, 0) // default visit duration
        );
    }

    private LocalTime parseGoogleOpeningHours(OpeningHours hours) {
        if (hours == null || hours.getWeekday_text() == null || hours.getWeekday_text().isEmpty()) {
            return LocalTime.of(9, 0); // default opening time
        }
        return parseHoursFromText(hours.getWeekday_text().get(0), true);
    }

    private LocalTime parseGoogleClosingHours(OpeningHours hours) {
        if (hours == null || hours.getWeekday_text() == null || hours.getWeekday_text().isEmpty()) {
            return LocalTime.of(22, 0); // default closing time
        }
        return parseHoursFromText(hours.getWeekday_text().get(0), false);
    }

    private LocalTime parseHoursFromText(String text, boolean isOpening) {
        try {
            // Example format: "Monday: 9:00 AM – 10:00 PM"
            String[] parts = text.split(": ")[1].split(" – ");
            String timeStr = isOpening ? parts[0] : parts[1];
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            log.error("Failed to parse hours from text: {}", text);
            return isOpening ? LocalTime.of(9, 0) : LocalTime.of(22, 0);
        }
    }
}