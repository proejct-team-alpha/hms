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

        List<LlmReservationResponse.Slot> slots = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.plusDays(1);
        LocalDate toDate = today.plusDays(7);

        // 날짜 범위 전체를 한 번에 조회해 인메모리 Set으로 변환 — 루프 내 N+1 방지
        Set<String> bookedKeys = reservationRepository.findBookedSlotsBetween(doctorId, fromDate, toDate)
                .stream()
                .map(row -> row[0] + ":" + row[1])
                .collect(Collectors.toSet());

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

                            boolean available = !bookedKeys.contains(date + ":" + slotTime);
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
