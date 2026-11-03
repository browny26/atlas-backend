package com.back.atlas.mapper;

import com.back.atlas.dto.ItineraryResponse;
import com.back.atlas.dto.SaveItineraryRequest;
import com.back.atlas.entity.Accommodation;
import com.back.atlas.entity.ActivityDetail;
import com.back.atlas.entity.DayItinerary;
import com.back.atlas.entity.Itinerary;

public final class ItineraryMapper {

    private ItineraryMapper() {
    }

    public static Itinerary toEntity(SaveItineraryRequest dto, Long userId) {
        Itinerary itinerary = new Itinerary();
        itinerary.setUserId(userId);
        itinerary.setDestination(dto.getDestination());
        itinerary.setTotalBudget(dto.getTotalBudget());
        itinerary.setTotalDays(dto.getTotalDays());
        itinerary.setTotalEstimatedCost(dto.getTotalEstimatedCost());
        itinerary.setBudgetStatus(dto.getBudgetStatus());
        itinerary.setTips(dto.getTips());

        if (dto.getAccommodation() != null) {
            SaveItineraryRequest.AccommodationDTO acc = dto.getAccommodation();
            itinerary.setAccommodation(
                    new Accommodation(acc.getName(), acc.getCost(), acc.getType(), acc.getDetails()));
        }
        if (dto.getItinerary() != null) {
            itinerary.setItinerary(dto.getItinerary().stream().map(ItineraryMapper::toDay).toList());
        }
        return itinerary;
    }

    public static ItineraryResponse toResponse(Itinerary itinerary) {
        return ItineraryResponse.builder()
                .id(itinerary.getId())
                .destination(itinerary.getDestination())
                .totalBudget(itinerary.getTotalBudget())
                .totalDays(itinerary.getTotalDays())
                .accommodation(itinerary.getAccommodation())
                .itinerary(itinerary.getItinerary())
                .tips(itinerary.getTips())
                .userId(itinerary.getUserId())
                .totalEstimatedCost(itinerary.getTotalEstimatedCost())
                .budgetStatus(itinerary.getBudgetStatus())
                .build();
    }

    private static DayItinerary toDay(SaveItineraryRequest.DayItineraryDTO dto) {
        return new DayItinerary(dto.getDay(), toActivity(dto.getMorning()), toActivity(dto.getAfternoon()),
                toActivity(dto.getEvening()));
    }

    private static ActivityDetail toActivity(SaveItineraryRequest.DayItineraryDTO.ActivityDTO dto) {
        return dto == null ? null : new ActivityDetail(dto.getActivity(), dto.getCost(), dto.getTime());
    }
}
