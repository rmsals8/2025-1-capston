// src/main/java/com/example/TripSpring/domain/VisitHistory.java
package com.example.TripSpring.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "visit_histories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String userId;
    
    private String placeName;
    
    private String placeId;
    
    private String category;
    
    private Double latitude;
    
    private Double longitude;
    
    private String address;
    
    private LocalDateTime visitDate;
    
    private Integer visitCount;
}