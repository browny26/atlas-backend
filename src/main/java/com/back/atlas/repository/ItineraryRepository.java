package com.back.atlas.repository;

import com.back.atlas.entity.Itinerary;
import com.back.atlas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    List<Itinerary> findByUserId(Long userId);

    Optional<Itinerary> findByIdAndUserId(Long id, Long userId);

    List<Itinerary> findByDestinationContainingIgnoreCase(String destination);

    long countByUserId(Long userId);
}
