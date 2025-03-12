package com.example.TripSpring.dto.response.route;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactInfo {
    private String phone;
    private String website;
    private Map<String, String> socialMedia;
}