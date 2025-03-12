package com.example.TripSpring.dto.foursquare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoursquarePlaceDetails {
    private String fsq_id;
    private String name;
    private FoursquareLocation location;
    private List<Category> categories;
    private Rating rating;
    private Hours hours;
    private Contact contact;
    private List<Photo> photos;
}