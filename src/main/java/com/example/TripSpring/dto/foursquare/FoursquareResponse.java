package com.example.TripSpring.dto.foursquare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoursquareResponse {
    private List<FoursquarePlace> results;
    private Context context;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Context {
        private Geo geo;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Geo {
        private String center;
        private String bounds;
    }
}