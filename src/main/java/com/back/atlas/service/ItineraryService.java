package com.back.atlas.service;

import com.back.atlas.dto.ItineraryResponse;
import com.back.atlas.dto.SaveItineraryRequest;
import com.back.atlas.dto.UpdateItineraryRequest;
import com.back.atlas.entity.Itinerary;
import com.back.atlas.entity.User;
import com.back.atlas.mapper.ItineraryMapper;
import com.back.atlas.repository.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** CRUD on the current user's saved itineraries. Every query is scoped to that user. */
@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;

    public ItineraryResponse saveForCurrentUser(SaveItineraryRequest request) {
        Itinerary itinerary = ItineraryMapper.toEntity(request, getCurrentUser().getId());
        return ItineraryMapper.toResponse(itineraryRepository.save(itinerary));
    }

    public List<ItineraryResponse> getAllUserItineraries() {
        return itineraryRepository.findByUserId(getCurrentUser().getId()).stream()
                .map(ItineraryMapper::toResponse)
                .toList();
    }

    public ItineraryResponse getItineraryById(Long id) {
        return ItineraryMapper.toResponse(findOwnedItinerary(id));
    }

    public ItineraryResponse updateItinerary(Long id, UpdateItineraryRequest request) {
        Itinerary itinerary = findOwnedItinerary(id);

        if (request.getDestination() != null) {
            itinerary.setDestination(request.getDestination());
        }
        if (request.getTotalBudget() != null) {
            itinerary.setTotalBudget(request.getTotalBudget());
        }
        if (request.getTotalDays() != null) {
            itinerary.setTotalDays(request.getTotalDays());
        }
        if (request.getAccommodation() != null) {
            itinerary.setAccommodation(request.getAccommodation());
        }
        if (request.getItinerary() != null) {
            itinerary.setItinerary(request.getItinerary());
        }
        if (request.getTips() != null) {
            itinerary.setTips(request.getTips());
        }

        return ItineraryMapper.toResponse(itineraryRepository.save(itinerary));
    }

    public void deleteItinerary(Long id) {
        itineraryRepository.delete(findOwnedItinerary(id));
    }

    public List<ItineraryResponse> searchItineraries(String destination) {
        return itineraryRepository
                .findByUserIdAndDestinationContainingIgnoreCase(getCurrentUser().getId(), destination)
                .stream()
                .map(ItineraryMapper::toResponse)
                .toList();
    }

    public long getItineraryCount() {
        return itineraryRepository.countByUserId(getCurrentUser().getId());
    }

    /** Returns 404 (not 403) for other users' itineraries, so their existence is not revealed. */
    private Itinerary findOwnedItinerary(Long id) {
        return itineraryRepository.findByIdAndUserId(id, getCurrentUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Itinerary not found"));
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
