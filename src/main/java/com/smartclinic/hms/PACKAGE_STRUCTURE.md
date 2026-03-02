# com.smartclinic.hms 패키지 구조

> doc/aki.md Spring Boot 패키지 구조 기반. 각 폴더의 FILES.md는 해당 패키지에 포함될 파일 목록을 정리한다.

```
com.smartclinic.hms
│
├── config/                    ◆ 경력자
│   └── FILES.md
│
├── common/                    ◆ 경력자
│   ├── interceptor/
│   ├── exception/
│   ├── util/
│   ├── service/
│   └── FILES.md
│
├── domain/                    ◆ 경력자 (Entity)
│   └── FILES.md
│
├── reservation/               ▲ 개발자 A
│   ├── dto/
│   └── FILES.md
│
├── staff/                     ● 개발자 B
│   ├── reception/
│   │   └── dto/
│   ├── walkin/
│   ├── reservation/
│   ├── dashboard/
│   └── FILES.md
│
├── doctor/                    ● 개발자 B
│   ├── treatment/
│   │   └── dto/
│   └── FILES.md
│
├── nurse/                     ● 개발자 B
│   ├── schedule/
│   ├── patient/
│   └── FILES.md
│
├── admin/                     ■ 개발자 C
│   ├── dashboard/
│   ├── reservation/
│   ├── reception/
│   ├── patient/
│   ├── staff/
│   ├── department/
│   ├── item/
│   ├── rule/
│   └── FILES.md
│
├── llm/                       ◆ 경력자 + ● 개발자 B (UI)
│   ├── dto/
│   └── FILES.md
│
├── HmsApplication.java         (기존)
└── PACKAGE_STRUCTURE.md       (본 문서)
```

**범례:** ◆ 경력자 ▲ 개발자 A ● 개발자 B ■ 개발자 C
