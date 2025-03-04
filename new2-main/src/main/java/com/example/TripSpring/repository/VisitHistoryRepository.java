// src/main/java/com/example/TripSpring/repository/VisitHistoryRepository.java
package com.example.TripSpring.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.TripSpring.domain.VisitHistory;

@Repository
public interface VisitHistoryRepository extends JpaRepository<VisitHistory, Long> {
    
    List<VisitHistory> findByUserId(String userId);
    
    List<VisitHistory> findByUserIdOrderByVisitDateDesc(String userId);
    
    Optional<VisitHistory> findByUserIdAndPlaceId(String userId, String placeId);
    
    @Query("SELECT vh FROM VisitHistory vh WHERE vh.userId = ?1 AND vh.category = ?2")
    List<VisitHistory> findByUserIdAndCategory(String userId, String category);
    
    @Query("SELECT vh.category, COUNT(vh) FROM VisitHistory vh WHERE vh.userId = ?1 GROUP BY vh.category")
    List<Object[]> countVisitsByCategory(String userId);
}