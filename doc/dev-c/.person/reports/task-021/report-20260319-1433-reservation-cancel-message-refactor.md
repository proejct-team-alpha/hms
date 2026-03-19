# 이번 작업 보고서: 예약/접수 취소 성공 메시지 분기 리팩토링

- **작업 일시**: 2026-03-19 14:33 (Asia/Seoul)
- **진행 상태**: 완료

## 1. 전체 작업 흐름 (Workflow)

1. 관리자 예약 목록에서 취소 성공 시 항상 `예약이 취소되었습니다.`라는 고정 문구가 보이고 있다는 점을 다시 확인했다. 이 방식은 `RECEIVED` 상태, 즉 접수 상태 항목을 취소했을 때도 같은 문구가 노출되어 사용자 경험이 어색해질 수 있었다.
2. 먼저 [AdminReservationApiController.java](c:/workspace/Team/hms/src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationApiController.java)와 [Resp.java](c:/workspace/Team/hms/src/main/java/com/smartclinic/hms/common/util/Resp.java)를 확인해, API는 현재 상태별 사용자 메시지를 내려주는 구조가 아니라 공통 `성공` 메시지를 사용하는 구조라는 점을 확인했다. 따라서 이번 단계는 API 계약을 넓히기보다, 실제 사용자 문구가 노출되는 SSR 흐름만 정리하는 것이 더 적절하다고 판단했다.
3. 이어서 [AdminReservationService.java](c:/workspace/Team/hms/src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationService.java)의 `cancelReservation()`을 `void`에서 `String` 반환으로 변경했다. 서비스는 취소 직전 예약의 원래 상태를 보고, `RESERVED`이면 `예약이 취소되었습니다.`, `RECEIVED`이면 `접수가 취소되었습니다.`를 반환하도록 정리했다.
4. [AdminReservationController.java](c:/workspace/Team/hms/src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java)는 더 이상 성공 문구를 직접 알지 않도록 바꿨다. 컨트롤러는 서비스가 돌려준 메시지를 flash attribute에 넣기만 하고, 문구 판단 책임은 서비스에 위임한다.
5. 마지막으로 서비스/컨트롤러 테스트를 함께 보강한 뒤 `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*'`를 실행해, 예약 취소/접수 취소 메시지 분기와 기존 예약 모듈 범위 회귀가 모두 정상인지 확인했다.

## 2. 핵심 코드 (Core Logic)

```java
@Transactional
public String cancelReservation(Long reservationId) {
    var reservation = adminReservationRepository.findById(reservationId)
            .orElseThrow(() -> CustomException.notFound("예약을 찾을 수 없습니다. ID: " + reservationId));
    ReservationStatus originalStatus = reservation.getStatus();

    try {
        reservation.cancel();
    } catch (IllegalStateException ex) {
        throw CustomException.invalidStatusTransition(ex.getMessage());
    }

    return buildCancelSuccessMessage(originalStatus);
}

private String buildCancelSuccessMessage(ReservationStatus originalStatus) {
    if (originalStatus == ReservationStatus.RECEIVED) {
        return "접수가 취소되었습니다.";
    }
    return "예약이 취소되었습니다.";
}
```

이 변경의 핵심은 “화면이 어떤 필터를 보고 있었는가”가 아니라 “실제로 무엇을 취소했는가”를 기준으로 성공 메시지를 정하는 데 있다.

## 3. 초등학생도 이해할 수 있는 비유 (Easy Analogy)

- 같은 지우개라도 연필 글씨를 지웠는지, 색연필 글씨를 지웠는지에 따라 설명이 달라질 수 있다.
- 이번 작업은 화면이 어디에 있었는지를 보는 게 아니라, 실제로 지운 대상이 무엇이었는지를 보고 문장을 다르게 말해주는 것과 비슷하다.
- 그래서 접수를 취소했으면 `접수가 취소되었습니다.`라고 말하고, 예약을 취소했으면 `예약이 취소되었습니다.`라고 말하게 된다.

## 4. 기술 딥다이브 (Technical Deep-dive)

- **왜 필터 기준이 아니라 원래 상태 기준인가**: 사용자가 `ALL` 화면에서 `RECEIVED` 상태 항목을 취소할 수도 있다. 이때 현재 필터값만 보면 문구가 어긋날 수 있다. 따라서 성공 메시지는 현재 화면 상태가 아니라, 취소 직전 엔티티의 실제 상태를 기준으로 정하는 것이 더 정확하다.
- **왜 서비스가 메시지를 반환하는가**: 이번 리팩토링의 핵심 판단은 비즈니스 문맥에 있다. “예약 취소”인지 “접수 취소”인지는 컨트롤러가 아니라 도메인 상태를 아는 서비스가 가장 잘 판단할 수 있다. 컨트롤러는 메시지를 직접 조합하지 않고, 반환된 메시지를 flash에 넣기만 하도록 얇게 유지하는 편이 책임 분리에 더 맞다.
- **왜 API는 이번 단계에서 건드리지 않았는가**: API는 현재 공통 `Resp.ok(...)` 구조로 `msg=성공`을 쓰고 있고, 사용자에게 직접 노출되는 세밀한 취소 문구를 다루는 책임은 없다. 이번 이슈는 관리자 SSR 화면 UX 문제에 가깝기 때문에, API 응답 계약을 같이 넓히기보다 SSR만 바꾸는 쪽이 범위 대비 효과가 좋았다.
- **테스트 전략**: 서비스 테스트에서는 `RESERVED`와 `RECEIVED` 두 상태를 각각 취소했을 때 반환 메시지가 다름을 직접 검증했고, 컨트롤러 테스트에서는 서비스가 반환한 메시지가 flash에 그대로 실리는지 확인했다. 이렇게 하면 문구 결정 책임과 화면 전달 책임이 둘 다 분리된 상태로 고정된다.

## 5. 검증 결과 (Verification)

- 실행 명령: `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*'`
- 결과: `BUILD SUCCESSFUL`

## 6. 변경 파일 (Changed Files)

- [AdminReservationService.java](c:/workspace/Team/hms/src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationService.java)
- [AdminReservationController.java](c:/workspace/Team/hms/src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java)
- [AdminReservationServiceTest.java](c:/workspace/Team/hms/src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationServiceTest.java)
- [AdminReservationControllerTest.java](c:/workspace/Team/hms/src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationControllerTest.java)

## 7. 다음 작업 메모 (Next Step)

- 현재 단계에서는 SSR 성공 메시지 분기만 반영했고, API 응답 계약은 그대로 유지했다.
- 만약 나중에 관리자 API에서도 상태별 사용자 메시지를 노출해야 하는 요구가 생기면, 그때는 [AdminReservationApiController.java](c:/workspace/Team/hms/src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationApiController.java)와 응답 DTO를 확장하는 방향을 따로 검토하면 된다.
- 지금 기준으로는 컨트롤러가 메시지를 직접 소유하지 않도록 정리됐기 때문에, 추후 상태별 문구가 더 늘어나더라도 서비스 중심으로 확장하기 쉬운 상태다.