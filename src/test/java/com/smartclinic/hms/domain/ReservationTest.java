package com.smartclinic.hms.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@ExtendWith(MockitoExtension.class)
@DisplayName("Reservation 도메인 취소·전이")
class ReservationTest {

    @Mock
    Patient patient;

    @Mock
    Doctor doctor;

    @Mock
    Department department;

    private Reservation reserved;

    @BeforeEach
    void setUp() {
        reserved = Reservation.create(
                "RES-TEST-001",
                patient,
                doctor,
                department,
                LocalDate.of(2026, 5, 1),
                "09:00",
                ReservationSource.ONLINE);
    }

    @Test
    @DisplayName("cancel — RESERVED면 CANCELLED로 전이하고 사유를 기록")
    void cancel_reserved_becomesCancelled() {
        // when
        reserved.cancel("개인 사정");

        // then
        assertThat(reserved.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reserved.getCancellationReason()).isEqualTo("개인 사정");
    }

    @Test
    @DisplayName("cancel — RECEIVED면 RESERVED로 롤백 (접수 취소)")
    void cancel_received_rollsBackToReserved() {
        // given
        reserved.receive();

        // when
        reserved.cancel("오접수");

        // then
        assertThat(reserved.getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("cancel — IN_TREATMENT면 IllegalStateException (조용한 무전이 방지)")
    void cancel_inTreatment_throws() {
        // given
        reserved.receive();
        reserved.startTreatment();

        // when & then
        assertThatThrownBy(() -> reserved.cancel(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("진료 중");
    }

    @Test
    @DisplayName("cancelFully — RECEIVED여도 즉시 CANCELLED")
    void cancelFully_received_becomesCancelled() {
        // given
        reserved.receive();

        // when
        reserved.cancelFully("비회원 취소");

        // then
        assertThat(reserved.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reserved.getCancellationReason()).isEqualTo("비회원 취소");
    }

    @Test
    @DisplayName("cancel — COMPLETED면 IllegalStateException")
    void cancel_completed_throws() {
        // given
        reserved.receive();
        reserved.complete();

        // when & then
        assertThatThrownBy(() -> reserved.cancel(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("진료 완료");
    }
}