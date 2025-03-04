package com.example.TripSpring.dto.domain;

import java.time.LocalTime;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class PlaceInfo {
   private String id;
   private String name;
   private boolean isOpen;
   private LocalTime openTime;
   private LocalTime closeTime;
   private double crowdLevel; // 0.0 ~ 1.0
   private String address;
   private String phoneNumber;
   private String category;
   private double rating; // 0.0 ~ 5.0
   private LocalTime averageVisitDuration;
}