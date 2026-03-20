package com.smartclinic.hms.llm.service;

import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.DoctorScheduleRepository;
import com.smartclinic.hms.llm.dto.LlmReservationResponse;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
@Slf4j
public class LlmReservationService {

    private final ReservationRepository reservationRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    public LlmReservationResponse.SlotList getAvailableSlots(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        var schedules = doctorScheduleRepository.findByDoctor_IdAndIsAvailableTrue(doctorId);

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.plusDays(1);
        LocalDate endDate = today.plusDays(7);

        // 1회 벌크 쿼리로 예약된 슬롯 전체 조회 (N+1 제거)
        Set<String> bookedSlots = reservationRepository
                .findBookedSlots(doctorId, startDate, endDate)
                .stream()
                .map(row -> row[0].toString() + "_" + row[1].toString())
                .collect(Collectors.toSet());

        List<LlmReservationResponse.Slot> slots = new ArrayList<>();

        for (int dayOffset = 1; dayOffset <= 7; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            DayOfWeek dow = date.getDayOfWeek();
            String dayCode = toEnglishDayCode(dow);

            schedules.stream()
                    .filter(s -> s.getDayOfWeek().equalsIgnoreCase(dayCode))
                    .forEach(schedule -> {
                        LocalTime slotTime = schedule.getStartTime();
                        while (slotTime.isBefore(schedule.getEndTime())) {
                            LocalTime slotEnd = slotTime.plusMinutes(30);
                            if (slotEnd.isAfter(schedule.getEndTime())) break;

                            boolean available = !bookedSlots.contains(date + "_" + slotTime);
                            slots.add(new LlmReservationResponse.Slot(date, slotTime, slotEnd, available));
                            slotTime = slotEnd;
                        }
                    });

            if (slots.size() >= 12) break;
        }

        return new LlmReservationResponse.SlotList(doctorId, doctor.getStaff().getName(), slots);
    }

    private String toEnglishDayCode(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }
}
