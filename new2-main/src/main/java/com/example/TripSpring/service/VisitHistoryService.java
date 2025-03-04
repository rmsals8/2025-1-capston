// src/main/java/com/example/TripSpring/service/VisitHistoryService.java
package com.example.TripSpring.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.TripSpring.domain.VisitHistory;
import com.example.TripSpring.dto.VisitHistoryDto;
import com.example.TripSpring.repository.VisitHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VisitHistoryService {
    
    private final VisitHistoryRepository visitHistoryRepository;
    
    @Transactional
    public VisitHistory addVisitHistory(VisitHistoryDto dto, String userId) {
        // 이미 방문한 적이 있는 장소인지 확인
        Optional<VisitHistory> existingVisit = visitHistoryRepository.findByUserIdAndPlaceId(userId, dto.getPlaceId());
        
        if (existingVisit.isPresent()) {
            // 기존 방문 기록이 있다면 방문 횟수 증가
            VisitHistory history = existingVisit.get();
            history.setVisitCount(history.getVisitCount() + 1);
            history.setVisitDate(LocalDateTime.now()); // 최근 방문 날짜 업데이트
            return visitHistoryRepository.save(history);
        } else {
            // 새로운 방문 기록 생성
            VisitHistory newVisit = VisitHistory.builder()
                .userId(userId)
                .placeName(dto.getPlaceName())
                .placeId(dto.getPlaceId())
                .category(dto.getCategory())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .address(dto.getAddress())
                .visitDate(LocalDateTime.now())
                .visitCount(1)
                .build();
            return visitHistoryRepository.save(newVisit);
        }
    }
    
    public List<VisitHistory> getVisitHistories(String userId) {
        return visitHistoryRepository.findByUserIdOrderByVisitDateDesc(userId);
    }
    
    public List<VisitHistory> getVisitHistoriesByCategory(String userId, String category) {
        return visitHistoryRepository.findByUserIdAndCategory(userId, category);
    }
    
    public List<Object[]> getCategoryStats(String userId) {
        return visitHistoryRepository.countVisitsByCategory(userId);
    }
    
    @Transactional
    public void deleteVisitHistory(Long id, String userId) {
        VisitHistory history = visitHistoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Visit history not found"));
        
        if (!history.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this visit history");
        }
        
        visitHistoryRepository.delete(history);
    }
    
    @Transactional
    public void deleteAllVisitHistories(String userId) {
        List<VisitHistory> userHistories = visitHistoryRepository.findByUserId(userId);
        visitHistoryRepository.deleteAll(userHistories);
    }
}