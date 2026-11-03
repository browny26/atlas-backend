package com.back.atlas.repository;

import com.back.atlas.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    List<Itinerary> findByUserId(Long userId);

    Optional<Itinerary> findByIdAndUserId(Long id, Long userId);

    List<Itinerary> findByUserIdAndDestinationContainingIgnoreCase(Long userId, String destination);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);
}
