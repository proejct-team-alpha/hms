package com.smartclinic.hms.llm.dto;

import com.smartclinic.hms.domain.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class LlmReservationResponse {

    @Getter
    @AllArgsConstructor
    public static class Max {
        private Long id;
        private Long doctorId;
        private String doctorName;
        private LocalDate reservationDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String status;

        public static Max from(Reservation entity) {
            return new Max(
                    entity.getId(),
                    entity.getDoctor().getId(),
                    entity.getDoctor().getStaff().getName(),
                    entity.getReservationDate(),
                    entity.getStartTime(),
                    entity.getEndTime(),
                    entity.getStatus().name()
            );
        }
    }

    @Data
    public static class Slot {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean available;

        public Slot(LocalDate date, LocalTime startTime, LocalTime endTime, boolean available) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.available = available;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class SlotList {
        private Long doctorId;
        private String doctorName;
        private List<Slot> slots;
    }
}
