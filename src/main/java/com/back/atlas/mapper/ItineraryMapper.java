package com.back.atlas.mapper;

import com.back.atlas.dto.ItineraryResponse;
import com.back.atlas.dto.SaveItineraryRequest;
import com.back.atlas.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ItineraryMapper {

    public static Itinerary toEntity(SaveItineraryRequest dto) {
        Itinerary itinerary = new Itinerary();
        itinerary.setDestination(dto.getDestination());
        itinerary.setTotalBudget(dto.getTotal_budget());
        itinerary.setTotalDays(dto.getTotal_days());
        itinerary.setTotalEstimatedCost(dto.getTotalEstimatedCost());
        itinerary.setBudgetStatus(dto.getBudgetStatus());

        if (dto.getAccommodation() != null) {
            Accommodation acc = new Accommodation();
            acc.setName(dto.getAccommodation().getName());
            acc.setCost(dto.getAccommodation().getCost());
            itinerary.setAccommodation(acc);
        }

        if (dto.getItinerary() != null) {
            List<DayItinerary> days = dto.getItinerary().stream().map(dayDTO -> {
                DayItinerary day = new DayItinerary();
                day.setDay(dayDTO.getDay());

                if (dayDTO.getMorning() != null) {
                    ActivityDetail morning = new ActivityDetail();
                    morning.setActivity(dayDTO.getMorning().getActivity());
                    morning.setCost(dayDTO.getMorning().getCost());
                    morning.setTime(dayDTO.getMorning().getTime());
                    day.setMorning(morning);
                }

                if (dayDTO.getAfternoon() != null) {
                    ActivityDetail afternoon = new ActivityDetail();
                    afternoon.setActivity(dayDTO.getAfternoon().getActivity());
                    afternoon.setCost(dayDTO.getAfternoon().getCost());
                    afternoon.setTime(dayDTO.getAfternoon().getTime());
                    day.setAfternoon(afternoon);
                }

                if (dayDTO.getEvening() != null) {
                    ActivityDetail evening = new ActivityDetail();
                    evening.setActivity(dayDTO.getEvening().getActivity());
                    evening.setCost(dayDTO.getEvening().getCost());
                    evening.setTime(dayDTO.getEvening().getTime());
                    day.setEvening(evening);
                }

                return day;
            }).collect(Collectors.toList());

            itinerary.setItinerary(days);
        }

        itinerary.setTips(dto.getTips());

        return itinerary;
    }

    public ItineraryResponse toResponse(Itinerary itinerary) {
        return ItineraryResponse.builder()
                .id(itinerary.getId())
                .destination(itinerary.getDestination())
                .totalBudget(itinerary.getTotalBudget())
                .totalDays(itinerary.getTotalDays())
                .accommodation(itinerary.getAccommodation())
                .itinerary(itinerary.getItinerary())
                .tips(itinerary.getTips())
                .userId(itinerary.getUserId())
                .build();
    }
}
