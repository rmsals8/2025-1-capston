package com.example.TripSpring.service;




import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.TripSpring.dto.PlaceDetailsResponse;
import com.example.TripSpring.dto.PlacesResponse;

@Service
public class PlacesService {
    private static final String GOOGLE_PLACES_API_KEY = "AIzaSyA036NtD7ALG40jOnqSGks2QsI1nAG9cGI";
    private final WebClient webClient;

    public PlacesService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<PlacesResponse> searchPlaces(String query, Double lat, Double lng) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/maps/api/place/textsearch/json")
            .queryParam("query", query)
            .queryParam("key", GOOGLE_PLACES_API_KEY);

        if (lat != null && lng != null) {
            builder.queryParam("location", String.format("%f,%f", lat, lng));
        }

        return webClient.get()
            .uri(builder.build().toString())
            .retrieve()
            .bodyToMono(PlacesResponse.class);
    }

    public Mono<PlaceDetailsResponse> getPlaceDetails(String placeId) {
        String uri = UriComponentsBuilder.fromPath("/maps/api/place/details/json")
            .queryParam("place_id", placeId)
            .queryParam("key", GOOGLE_PLACES_API_KEY)
            .build()
            .toString();

        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(PlaceDetailsResponse.class);
    }
}
