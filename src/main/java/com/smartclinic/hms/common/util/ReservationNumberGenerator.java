package com.smartclinic.hms.common.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * 예약번호 발급 유틸리티 — 동시성 안전(Thread-Safe) 채번
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 예약번호 형식
 *   RES-{YYYYMMDD}-{NNN}
 *   예) RES-20260315-001  (2026년 3월 15일, 당일 1번째 예약)
 *       RES-20260315-042  (같은 날 42번째)
 *
 * ■ 동시성 전략
 *   - 날짜별 AtomicLong 카운터를 ConcurrentHashMap으로 관리
 *   - ConcurrentHashMap.computeIfAbsent() → 동일 날짜 최초 접근 시 DB 조회 1회만 실행
 *   - AtomicLong.incrementAndGet()        → 이후 채번은 락 없이 원자적으로 증가
 *   - 단일 서버 환경에서 완전한 동시성 보장
 *
 * ■ 서버 재시작 처리
 *   - 재시작 시 counters 맵이 초기화되므로 DB 조회(currentCountSupplier)로 재동기화
 *   - currentCountSupplier가 DB의 현재 예약 수를 반환하면 그 다음 번호부터 채번
 *
 * ■ 사용 예 (ReservationService)
 * <pre>
 * &#64;Autowired private ReservationNumberGenerator numberGenerator;
 *
 * String reservationNumber = numberGenerator.generate(
 *     reservationDate,
 *     () -> reservationRepository.countByReservationDate(reservationDate)
 * );
 * </pre>
 *
 * ■ 멀티 서버(HA) 환경 주의
 *   멀티 서버 배포 시 DB 시퀀스 테이블 기반 채번으로 전환 필요.
 * ════════════════════════════════════════════════════════════════════════════
 */
@Component
public class ReservationNumberGenerator {

    private static final String PREFIX = "RES";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 날짜별 원자적 시퀀스 카운터.
     * key   : 예약 날짜
     * value : 해당 날짜의 현재 최대 시퀀스 번호 (DB 카운트로 초기화 후 AtomicLong으로 증가)
     */
    private final ConcurrentHashMap<LocalDate, AtomicLong> counters = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════════════════════════════
    // 채번 — 동시성 안전
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 예약번호 채번.
     *
     * <p>처리 순서:
     * <ol>
     *   <li>해당 날짜의 카운터가 없으면 {@code currentCountSupplier}로 DB 조회 후 초기화
     *       (computeIfAbsent — 동일 날짜 동시 최초 접근 시에도 DB 조회 1회 보장)</li>
     *   <li>AtomicLong.incrementAndGet()으로 다음 시퀀스 번호 원자적 획득</li>
     *   <li>형식에 맞게 포맷 후 반환</li>
     * </ol>
     *
     * @param date                 예약 날짜 (예약번호 날짜 파트)
     * @param currentCountSupplier 해당 날짜의 현재 DB 예약 수 조회 함수
     *                             (서버 재시작 시 또는 첫 채번 시 1회 호출됨)
     * @return 포맷된 예약번호 (예: {@code "RES-20260315-001"})
     */
    public String generate(LocalDate date, LongSupplier currentCountSupplier) {
        AtomicLong counter = counters.computeIfAbsent(
                date,
                d -> new AtomicLong(currentCountSupplier.getAsLong())
        );
        long sequence = counter.incrementAndGet();
        return format(date, sequence);
    }

    // ════════════════════════════════════════════════════════════════════════
    // 정적 유틸리티
    // ════════════════════════════════════════════════════════════════════════

    /**
     * 예약번호 형식 변환 (정적 메서드).
     * DB에서 직접 예약번호를 생성해야 할 때 사용.
     *
     * @param date     예약 날짜
     * @param sequence 시퀀스 번호 (1 이상)
     * @return 포맷된 예약번호 (예: {@code "RES-20260315-001"})
     */
    public static String format(LocalDate date, long sequence) {
        return String.format("%s-%s-%03d", PREFIX, date.format(DATE_FORMATTER), sequence);
    }

    /**
     * 예약번호 날짜 파트 추출.
     * 예) "RES-20260315-001" → LocalDate(2026, 3, 15)
     *
     * @param reservationNumber 예약번호
     * @return 예약 날짜
     * @throws IllegalArgumentException 형식이 올바르지 않은 경우
     */
    public static LocalDate extractDate(String reservationNumber) {
        try {
            String datePart = reservationNumber.split("-")[1];  // "20260315"
            return LocalDate.parse(datePart, DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("예약번호 형식이 올바르지 않습니다: " + reservationNumber, e);
        }
    }

    /** 과거 날짜 카운터 정리 — 24시간마다 실행. 2일 이전 카운터를 제거하여 메모리 누수 방지. */
    @Scheduled(fixedRate = 86_400_000)
    public void cleanupExpiredCounters() {
        LocalDate threshold = LocalDate.now().minusDays(2);
        counters.keySet().removeIf(date -> date.isBefore(threshold));
    }

    /**
     * 인메모리 카운터 초기화 (특정 날짜).
     * 테스트 또는 운영 중 수동 보정이 필요할 때 사용.
     *
     * @param date 초기화할 날짜
     */
    public void resetCounter(LocalDate date) {
        counters.remove(date);
    }
}
