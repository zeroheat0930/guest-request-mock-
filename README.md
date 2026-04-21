# 🏨 다올 컨시어지 (Guest Request)

호텔 투숙객이 QR/태블릿으로 요청을 보내면 **부서별로 자동 라우팅되어 스태프(CCS)에게 실시간 전달**되는 독립 제품 프로젝트.
공모전 Mock 에서 출발해, 디스패처 추상화·기능 플래그·게스트/스태프 번들 분리까지 갖춘 **자체 완결 호텔 운영 시스템**.

---

## 📦 구성

```
guest-request-mock/
 ├ api_server/    ← Spring Boot 3.2 / Java 17 / MyBatis (MariaDB, PMS MVC 패턴)
 ├ vue_client/    ← Vue 3 + Vite 멀티 엔트리 (index.html 게스트 / staff.html 스태프·관리자)
 └ landing/       ← QR 랜딩 페이지 (순수 HTML, 심사 제출용)
```

### 기술 스택
| 레이어 | 스택 |
|---|---|
| 프론트 | Vue 3, Vite, Vue Router, Axios, PWA |
| 백엔드 | Spring Boot 3.2, Java 17, MyBatis, Jackson, java.net.http |
| 저장소 | MariaDB (사내 PMS DB 직결, INV/PMS cross-schema) |
| AI | Anthropic Claude API (프록시) + 룰 기반 폴백 |
| 인증 | 게스트 JWT (예약번호+체크인일자+생년월일 → 72h) |

---

## 🚀 로컬 실행

### 백엔드
```bash
cd api_server
./run.bat     # 윈도우 (JDK17 + Maven 경로 고정)
# 또는
mvn spring-boot:run
# → http://localhost:8080
```

### 프론트
```bash
cd vue_client
npm install
npm run dev
# → http://localhost:5173
```

### 환경변수 (`api_server/env.local.bat` 또는 셸에서 export)
```
ANTHROPIC_API_KEY=...               # AI 챗봇 활성화 (비어있으면 룰 기반 폴백)
CONCIERGE_DISPATCHER_CCS=true       # 게스트 요청 → CCS 부서 라우팅 (기본 ON, 끄면 요청 삼켜짐)
CONCIERGE_DISPATCHER_EXTERNAL=false # 외부 REST 훅(옵션, 커스텀 연동)
CONCIERGE_TENANT_PROP_CD=0000000010 # 멀티 테넌시 — 호텔 프로퍼티 코드
CONCIERGE_TENANT_CMPX_CD=00001      # 멀티 테넌시 — 부속건물 코드
CONCIERGE_ADMIN_PW=...              # 어드민 UI 패스워드 (미설정 시 /admin/** = 503)
JWT_SECRET=...                      # 32바이트 이상 랜덤 시크릿 (prod 필수)
NEARBY_PROVIDER=mock                # mock(기본) | kakao
KAKAO_REST_API_KEY=...              # NEARBY 실제 데이터 (provider=kakao 일 때만)
```

---

## 🎛️ 완성된 기능

**게스트 앱** (좌측 LNB 탭, 기능 플래그로 프로퍼티별 on/off)
- 🤖 **AI 다국어 챗봇** — 한/영/일/중 자연어 → 의도 파싱 → API 자동 호출 (Claude 프록시 + 룰 폴백)
- 🛎️ **어메니티 요청** — 품목 5종 + 수량 + 메모
- 🧹 **하우스키핑** — MU / DND / CLR 토글
- ⏰ **레이트 체크아웃** — 연장 시간 → 요금 자동 산정 → 2단계 확인
- 📍 **주변 안내** — 호텔 좌표 기준 반경 1km 5 카테고리 (음식점/카페/편의점/관광/약국)
- 🚗 **주차 차량 등록** — 차량번호/차종/메모 + 이력 카드

**CCS 스태프 시스템** (독립 모듈, `/staff` · `/runner` · `/admin/ccs`)
- 🧑‍🍳 **CCS 스태프 대시보드** — 부서별 태스크 피드, 실시간 WebSocket, 직원 간 요청 생성
- 🏃 **러너 PWA** — 모바일 전용, 스와이프 상태 전환, 홈화면 설치 지원
- 📊 **통계 위젯** — 오늘 접수/완료/평균 처리시간
- 🏢 **CCS 어드민** — 부서 CRUD + 요청 타입→부서 라우팅 규칙 편집

**어드민 UI** (`/admin/features`)
- 로그인 화면 (X-Admin-Token 검증, sessionStorage 보관, 401 시 자동 리다이렉트)
- 기능 플래그 토글 (iOS 스타일 스위치, sortOrd 편집, configJson 아코디언 + JSON 검증)
- 저장 완료 토스트 (2s fade)

**아키텍처**
- **디스패처 추상화** — `RequestDispatcher` 인터페이스 + 2 구현체(`CcsDispatcher` 기본 ON / `ExternalApiDispatcher` 옵션)
- **게스트/스태프 번들 완전 분리** — Vite 멀티 엔트리로 `index.html`(게스트)와 `staff.html`(스태프·관리자)를 **서로 다른 JS 번들**로 빌드. 게스트 태블릿은 스태프 코드를 단 한 줄도 다운로드하지 않음
- **기능 플래그** — `CONCIERGE_FEATURE` 테이블, 프로퍼티별 메뉴 on/off, 어드민에서 실시간 토글
- **게스트 JWT 인증** — `SecurityContextUtil.requirePrincipal()` 로 propCd/rsvNo 격리, 본인 데이터만 접근
- **요청→부서 자동 라우팅** — AMENITY/HK→하우스키핑, LATE_CO→프론트, PARKING→엔지니어링. `CcsTask` 생성 + WebSocket `/topic/ccs/dept/{deptCd}` 푸시

---

## 🧑‍🍳 CCS 스태프 시스템

게스트 요청이 자동으로 부서별 태스크로 라우팅되어 직원이 처리하는 독립 CCS(Communication Center System) 모듈. 기존 컨시어지 스택 위에 `com.daol.concierge.ccs` 패키지로 신규 탑재.

**기능**
- **스태프 로그인** — 게스트 JWT 와 별도 `type=STAFF` 클레임 발급, BCrypt 검증
- **부서별 대시보드** (`/staff`) — 태스크 피드, 상태별 탭(대기/진행/완료), 실시간 WebSocket 업데이트, 직원 간 요청 생성 모달
- **러너 PWA** (`/runner`) — 모바일 전용 경량 뷰, 스와이프/버튼으로 상태 전환, 홈화면 설치 지원
- **부서 로드 조회** — 부서원별 담당 건수 카드, 재배정 드롭다운

### 기본 시드 데이터

| 구분 | 코드 | 이름 |
|---|---|---|
| 부서 | HK | 하우스키핑 |
| 부서 | FR | 프론트 |
| 부서 | ENG | 엔지니어링 |
| 부서 | FB | 식음료 |

| loginId | 부서 | 초기 비밀번호 |
|---|---|---|
| hk1, hk2 | HK | test1234 |
| fr1, fr2 | FR | test1234 |
| eng1, eng2 | ENG | test1234 |
| fb1, fb2 | FB | test1234 |

### URL

| 경로 | 설명 |
|---|---|
| `/staff/login` | 스태프 로그인 |
| `/staff` | 부서 대시보드 (로그인 필수) |
| `/runner` | 러너 PWA (로그인 필수) |
| `/admin/ccs` | CCS 어드민 (부서/라우팅 규칙 편집) |

### 주요 엔드포인트

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/ccs/auth/login` | 스태프 로그인 → JWT 발급 |
| GET  | `/api/ccs/tasks?statusCd=` | 내 부서 태스크 목록 |
| PUT  | `/api/ccs/tasks/{id}/assign` | 태스크 재배정 |
| PUT  | `/api/ccs/tasks/{id}/status` | 태스크 상태 전환 |
| POST | `/api/ccs/tasks` | 직원 간 요청 생성 (STAFF_REQ) |
| GET  | `/api/ccs/dept/{deptCd}/load` | 부서원별 로드 조회 |
| GET  | `/api/ccs/stats/today?deptCd=` | 오늘 접수/완료/평균 처리시간 |
| GET  | `/api/concierge/admin/departments` | 어드민 — 부서 목록 |
| POST/PUT/DELETE | `/api/concierge/admin/departments` | 어드민 — 부서 CRUD |
| GET/PUT | `/api/concierge/admin/staff` | 어드민 — 직원 관리 |

### 요청 타입 → 부서 라우팅

| 요청 타입 | 라우팅 부서 | 비고 |
|---|---|---|
| AMENITY | HK | 어메니티 요청 |
| HK | HK | 하우스키핑 상태 변경 |
| LATE_CO | FR | 레이트 체크아웃 |
| PARKING | ENG | 주차 등록 |
| CHAT | — | 무시 (태스크 미생성) |
| NEARBY | — | 무시 (태스크 미생성) |

### WebSocket 토픽

| 토픽 | 용도 |
|---|---|
| `/topic/ccs/dept/{deptCd}` | 부서별 신규 태스크 브로드캐스트 |
| `/topic/ccs/staff/{staffId}` | 개인 배정 알림 |

WebSocket 연결: `ws://localhost:8080/ws-ccs` (STOMP)

### Dispatcher 기능 플래그

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `CONCIERGE_DISPATCHER_CCS` | `true` | 게스트 요청 → CCS 부서 태스크 라우팅 (유일한 기본 경로) |
| `CONCIERGE_DISPATCHER_EXTERNAL` | `false` | 커스텀 외부 REST 훅(옵션, 타사 호텔 시스템 연동용 stub) |

---

## 🔌 API 명세

Base: `http://localhost:8080/api`
응답 포맷 (PMS 스타일): `{ status, message, error }` + `Responses.MapResponse { map }` / `Responses.ListResponse { list, page }`

### `/api/gr` (게스트 요청)
| Method | Path | 설명 |
|---|---|---|
| GET  | `/reservation?rsvNo=`  | 예약 단건 조회 |
| GET  | `/reservation/list`    | 예약 목록 |
| GET  | `/amenity/items`       | 어메니티 품목 마스터 |
| GET  | `/amenity/list?rsvNo=` | 어메니티 요청 이력 |
| POST | `/amenity`             | 어메니티 요청 등록 |
| GET  | `/housekeeping?rsvNo=` | 현재 하우스키핑 상태 |
| POST | `/housekeeping`        | 상태 변경 |
| GET  | `/late-checkout?rsvNo=&reqOutTm=` | 레이트CO 요금 조회 |
| POST | `/late-checkout`       | 레이트CO 신청 |
| GET  | `/parking/list?rsvNo=` | 주차 등록 이력 |
| POST | `/parking`             | 주차 차량 등록 |

### `/api/concierge` (기능 플래그 + 주변 안내)
| Method | Path | 설명 |
|---|---|---|
| GET  | `/features`           | 게스트용 — `useYn=Y` 기능 목록 |
| GET  | `/nearby?category=`   | 주변 정보 (food/cafe/conv/tour/pharmacy) |
| GET  | `/admin/features?propCd=` | 어드민 — 전체 기능 (X-Admin-Token) |
| PUT  | `/admin/features?propCd=` | 어드민 — bulk upsert |

### `/api/ai` (LLM 프록시)
| Method | Path | 설명 |
|---|---|---|
| POST | `/chat`    | Claude API 프록시 (의도 파싱) |
| GET  | `/status`  | LLM 가용 여부 |

### `/api/ccs` (CCS 스태프 시스템)
| Method | Path | 설명 |
|---|---|---|
| POST | `/auth/login` | 스태프 로그인 → `type=STAFF` JWT 발급 |
| GET  | `/tasks?statusCd=` | 내 부서 태스크 목록 |
| POST | `/tasks` | 직원 간 요청 생성 (STAFF_REQ) |
| PUT  | `/tasks/{id}/assign` | 태스크 재배정 `{assigneeId}` |
| PUT  | `/tasks/{id}/status` | 태스크 상태 전환 |
| GET  | `/dept/{deptCd}/load` | 부서원별 담당 건수 |
| GET  | `/stats/today?deptCd=` | 오늘 접수/완료/평균 처리시간 |

### `/api/concierge/admin` (어드민 — CCS 추가분)
| Method | Path | 설명 |
|---|---|---|
| GET  | `/admin/departments` | 부서 목록 (X-Admin-Token) |
| POST | `/admin/departments` | 부서 등록 |
| PUT  | `/admin/departments/{deptCd}` | 부서 수정 |
| DELETE | `/admin/departments/{deptCd}` | 부서 삭제 |
| GET/PUT | `/admin/staff` | 직원 목록/수정 |

### 응답 코드 (ApiStatus)
| Code | 의미 |
|---|---|
| `0` | SUCCESS |
| `-10` | NOT_FOUND_USER |
| `-20` | INVALID_PASSWORD |
| `-30` | ACCESS_DENIED |
| `401` | BAD_REQUEST |
| `403` | FORBIDDEN |
| `404` | NOT_FOUND |
| `-500` | SYSTEM_ERROR |

### 샘플 데이터 (InvSeedRunner + INV_SEED.sql)
- 프로퍼티: `HQ` (서울시청 좌표 `37.5665, 126.9780`)
- 예약: `R2026041300001` (1205호/HONG GILDONG/ko_KR), `R2026041300002` (0807호/JOHN SMITH/en_US)
- 품목: `AM001~AM005` (수건/생수/비누/샴푸/칫솔세트)
- 기능 플래그 6건: AMENITY/HK/LATE_CO/CHAT/NEARBY/PARKING 전부 Y

---

## 🎨 PMS 스타일 일치 포인트

사내 이식 시 일관성을 위해 아래 규칙을 따름 (PMS core 인프라 이식 완료):

- Controller: `@Controller extends BaseController` + `@ResponseBody` + `@RequestMapping("/api/...")`
- 파라미터: `RequestParams requestParams` → `requestParams.getParams()` / `getString()` / `getInt()`
- Response: `Responses.MapResponse.of(map)` / `ListResponse.of(list)` / `ok()` / `ok(message)`
- 예외: `ApiException(ApiStatus, message)` → `BaseController @ExceptionHandler` 자동 처리
- DAO: `CommonDAO` (MyBatis SqlSession 래퍼, snake_case → camelCase 자동 변환) + Mapper 인터페이스
- 트랜잭션: `TxAdviceConfig` AOP — `insert*/update*/delete*/save*/proc*` 메서드 자동 트랜잭션
- 네이밍: camelCase 약어 (`rsvNo`, `roomNo`, `chkOutTm`, `hkStatCd`, `procStatCd`, `rateTpCd`, `addAmt`, `curCd`)
- 서비스: 한국어 주석만 (변수/메서드명은 영문)
- 헬퍼 최소화 (인라인 우선)
- DDL: 수동 관리 (INV 스키마, JPA 완전 제거)
- 들여쓰기: 탭

---

## 💡 프로젝트 정체성 (압축본)

**"버튼 탭 기반 컨시어지 태블릿 → 자연어 대화형으로 바꾼 버전"** + **타 PMS 이식 가능한 독립 제품**

### 차별화 포인트
1. **언어 장벽 제거** — 해외 관광객 대응, 메뉴 번역 유지보수 불필요
2. **롱테일 요청 커버** — "베개 하나 더", "방이 너무 추워요" 도 AI 가 적절한 티켓으로 라우팅
3. **자연스러운 UX** — 노인/비IT층도 말만 하면 됨, 레이트CO 전환율 상승 기대
4. **이식성** — 디스패처가 추상화돼 있어 호텔 운영 시스템이 바뀌어도 어댑터 1개만 추가하면 됨
5. **번들 격리** — 게스트 QR 태블릿에 스태프 코드가 내려가지 않음(Vite 멀티 엔트리), 역할 기반 정보 노출 최소화

### 핵심 결정
- 플랫폼: **Vue 3 + PWA** (게스트 1~2박 특성상 앱 설치 저항 = 전환율 사망 → QR 웹앱이 정답)
- DB: **MariaDB** (사내 PMS DB 직결, INV/PMS cross-schema)
- 인증: 게스트 JWT, 모든 데이터 `propCd`/`cmpxCd` 격리 (멀티 테넌시)
- 아키텍처: 컨시어지가 **자체 DB + 자체 어드민 UI + 자체 스태프 시스템(CCS) 의 주인**. 게스트 요청은 `CcsDispatcher` 를 통해 부서별 태스크로 라우팅되어 `/staff` 대시보드와 `/runner` PWA 에 실시간 푸시됨. 외부 PMS 브릿지 없음
- 배포 분리: `hotel.com/` 게스트 / `hotel.com/staff.html` 스태프·관리자 — 두 개의 독립된 JS 번들

---

## 🗺️ 상용화 1.0 로드맵 (2026-04-21 수립)

**비전**: 컨시어지(게스트앱) + CCS(스태프앱) 통합 제품을 **타 호텔에 판매 가능한 독립 SaaS** 로 완성. PMS 종속성은 선택적 어댑터로만 존재.

### 현재 자산 솔직 평가

| 견고한 것 ✅ | 부족한 것 ❌ |
|---|---|
| 멀티 테넌시 (propCd/cmpxCd) | 도메인 모듈 구조가 얕음 (`ccs/` 단일 패키지) |
| 기능 플래그 (프로퍼티별 on/off) | 스태프 UI 한국어 전용 |
| Dispatcher 추상화 | 핵심 도메인 테스트 커버리지 30% |
| 역할 기반 권한 (userTp) + WS 토픽 계층 | API 문서화 미완성 |
| 게스트/스태프 번들 분리 | 감사 로그 부재 |
| 게스트 앱 4개 국어 i18n | PMS 연동 어댑터 인터페이스 없음 |
| AI 자연어 라우팅 (차별점 #1) | 관리자 리포트 전무 |

### CCS 5대 본연 기능 구현 상태

| # | 기능 | 상태 | PMS 중복 |
|---|------|------|------|
| ① | **업무 에스컬레이션 (SLA)** | ✅ 2026-04-21 구현 | 없음 (CCS 고유) |
| ② | 분실물 센터 (Lost & Found) | 🗓️ Phase B | PMS_LOSTFOUND 있음 → PmsSyncAdapter 연동 |
| ③ | 고객 불만 관리 (VOC) | 🗓️ Phase B | PMS_CUST_VOC 있음 → PmsSyncAdapter 연동 |
| ④ | 대여 품목 (Rental) | 🗓️ Phase D | PMS_LOAN 있음 → PmsSyncAdapter 연동 |
| ⑤ | 당직 관리자 로그 | 🗓️ Phase D | 없음 (PMS Night Audit 은 자동마감만, 일지 X) |

> **스코프 아웃**: HSKP (플로어 그리드 / 룸메이드 / 청소 이력) / HES (Work Order) — PMS 본업 모듈에 이미 완성도 높게 구현 존재. CCS 는 필요 시 `PMS_ROOM_NUMBER.HK_STAT` 등을 **읽기 전용** 조회.

### Phase 별 상세 계획

#### Phase A — 도메인 골격 리팩토링 (2~3일) 🚧
**목적**: 새 기능 추가 전 뼈대 정리. 지금 `ccs/` 단일 패키지 구조는 기능 늘수록 결합도 급상승.

```
com.daol.concierge.ccs/
├─ (기존) routing/ task/ auth/ websocket/ admin/ stats/   — CCS 코어
├─ lostfound/     🆕 Service/Controller/Mapper/Dto/SyncAdapter
├─ voc/           🆕 동상
├─ rental/        🆕 동상
├─ duty/          🆕 동상
└─ sync/          🆕 PmsSyncAdapter 인터페이스 + DAOL 구현체 stub
```

각 도메인 폴더는 `ccs/task` 와 동일한 계층 구조. `SyncAdapter` 인터페이스는 feature flag (`concierge.pms.sync.enabled`) 로 제어.

#### Phase B — ② 분실물 + ③ 고객 불만 풀구현 (3~4일)
- **INV 스키마**: `CCS_LOSTFOUND`, `CCS_VOC` 신규 (CCS 자체 소유)
- **게스트 앱**: "분실물 신고" / "피드백" 탭 추가
- **AI 자연어 분류**: 게스트가 "엘리베이터에 시계 놓고 왔어요" 입력 → AI → `LOSTFOUND` 도메인 자동 라우팅
- **스태프 UI**: 분실물 매칭 리스트 / VOC 카테고리별 처리 대시보드 + 만족도 점수(1~5)
- **PmsSyncAdapter**: DAOL 배포용 stub — 기본 OFF, ON 시 `PMS_LOSTFOUND` / `PMS_CUST_VOC` 동기화

#### Phase C — 스태프 UI i18n 전면화 (1~2일)
현재 스태프/관리자 화면이 한국어 하드코딩 → 게스트 앱과 동일하게 `t()` 함수 + 4개 국어 사전(`ko`/`en`/`ja`/`zh`). 해외 호텔 파일럿 전제 필수 조건.

#### Phase D — ④ 대여 + ⑤ 당직 로그 풀구현 (2~3일)
- **대여**: `CCS_RENTAL_ITEM` (카탈로그) + `CCS_RENTAL_ORDER` (신청/반납). 게스트 앱 탭.
- **당직**: `CCS_DUTY_LOG` (교대 기록/이벤트 타임라인/인수인계). PMS Night Audit 완료 여부만 읽기 전용으로 표시.

#### Phase E — 관리자 리포트 + Swagger + 감사 로그 (2~3일)
- 일/주/월별 요청 통계 대시보드 (부서/소스타입/SLA 준수율)
- `@Operation` / `@Schema` annotation 전 엔드포인트
- 도메인별 audit 테이블 (누가 언제 뭘 변경했는지)

#### Phase F — 심사 대비 (1일)
- 시연 영상 7단계 스토리 촬영
- 랜딩 페이지 최종 폴리싱
- 전체 QA + 버그 픽스

### 타임라인 (심사 2026-05-20 기준)

| 주차 | Phase | 기간 |
|---|---|---|
| W1 (4/21~4/27) | A + B 시작 | 5~6일 |
| W2 (4/28~5/4) | B 완료 + C | 4~5일 |
| W3 (5/5~5/11) | D + E 시작 | 5일 |
| W4 (5/12~5/18) | E 완료 + F | 4~5일 |
| W5 (5/19~5/20) | 리허설 + 제출 | 2일 |

**총 11~16일 작업** (29일 중) — 버퍼 **13~18일** 확보.

### 설계 원칙

1. **CCS-first 데이터 주권** — 각 도메인 INV 스키마에 자체 테이블. PMS 동기화는 `PmsSyncAdapter` 어댑터로만.
2. **도메인 일관성** — 모든 도메인이 `Service/Controller/Mapper/Dto/SyncAdapter` 동일 구조.
3. **Feature flag 우선** — 새 기능은 `concierge.feature.<name>.enabled` 플래그 뒤에 숨김. 기본 OFF, 프로퍼티별 어드민에서 ON.
4. **다국어 우선** — 새 스트링은 반드시 i18n 사전에 등록.
5. **테스트 우선** — 도메인 서비스는 핵심 경로 유닛테스트.

## 🗓️ 진행 로그

### 2026-04-21 심야 — CCS 본연 기능 확장 로드맵 수립 + ① SLA 에스컬레이션 구현

**배경 정정**: 이전에 HSKP 플로어 그리드를 CCS 에 붙이려 했으나, WINPAC 모듈 구분 재확인 결과 **HSKP 는 PMS 본업 모듈**(15+ 테이블, 43 JSP, 25 코드그룹 이미 구현)이고 CCS 와 별개. CCS 본연의 기능은 WINPAC 기준 아래 5개:

#### CCS 5대 본연 기능 (WINPAC 벤치마크)

| # | 기능 | 상태 |
|---|------|------|
| **①** | **업무 에스컬레이션 (SLA Overdue)** | ✅ 2026-04-21 구현 |
| ② | 분실물 센터 (Lost & Found) | 🗓️ 예정 |
| ③ | 고객 불만 관리 (Complaint) | 🗓️ 예정 |
| ④ | 대여 품목 (Rental Items) | 🗓️ 예정 |
| ⑤ | 당직 관리자 로그 (Duty Manager Log) | 🗓️ 예정 |

> **스코프 아웃**: HSKP (객실 상태 그리드 / 룸메이드 / 청소 이력) / HES (Work Order / 예방정비) — PMS 본업 모듈로 이미 구현 존재. CCS 는 필요 시 PMS `PMS_ROOM_NUMBER.HK_STAT` 등을 **조회만** 한다.

#### ① SLA 에스컬레이션 — 구현 상세

**백엔드**
- `CcsSlaRules.java` 신규 — sourceType 별 기본 SLA 분:
  - `COMPLAINT: 10`, `AMENITY: 15`, `PARKING: 20`, `LATE_CO/CHAT: 30`, `HK_*: 45`, `STAFF_REQ: 60`, 기본 30
- `CcsTaskService.enrichSla(task)` — 응답 태스크에 `slaMin` / `elapsedMin` / `overdue` 필드 주입
- `listForDept` / `publish` 경로에서 자동 호출
- `publish` 가 `overdue=true` 일 때 **`/topic/ccs/esc/{propCd}`** 별도 토픽으로 브로드캐스트

**프론트**
- 태스크 카드 `.overdue` 클래스 — 빨강 테두리 + 박스섀도 펄스 애니메이션
- row-meta 에 SLA 타이머: `⏱ 10/15m` / `🔥 25/15m` (초과 시 빨강)
- 모달에 경과/기준/초과분 상세
- 관리자(userTp 00001~00003)만 `/topic/ccs/esc/{propCd}` 구독 — 우상단 **빨강 토스트** 자동 표시 (8초), 클릭 시 닫기

**스태프 번들**: 60.66 KB (전 대비 +0.66 KB — SLA 로직 + 토스트)

### 2026-04-21 저녁 — 메인보드 UX + 모바일 반응형 + 랜딩 호스트 설정

**9) 게스트 앱 홈(메인보드) 도입** — 기존엔 `/` 가 곧바로 `/amenity` 로 리다이렉트돼 투숙객이 "무엇이 있는지" 한눈에 못 보던 UX. 호텔 컨시어지 철학에 맞게 **럭셔리 메인보드** 신설.

- `HomeView.vue` 신규 — 세리프 Hero(환영 + 룸 배지 + 골드 디바이더) + 기능 타일 그리드(기능별 그라디언트 + 호버 애니메이션 + 화살표 이동)
- 라우트 `/` → `/home` 기본 착지. `/home` 는 `meta.home=true` (featureCd 검사 우회)
- `App.vue` LNB: 최상단에 🏠 홈 탭 + 브랜드 블록 전체를 홈 링크로 전환 — 클릭 하면 메인보드 복귀
- `i18n/ui.js`: `home.tagline` / `home.label` 추가 (ko/en/ja/zh)
- 모바일(< 720px): 기존 LNB 가로 스크롤 바 유지. 메인보드 2열 그리드 자동 반영

**10) 스태프 대시보드 모바일 반응형 + 러너 네비게이션**

- `StaffDashboardView`: `@media (max-width: 720px)` breakpoint 추가. 헤더 세로쌓기, 탭 가로 스크롤, 태스크 카드 패딩 축소, 모달 축소/재구성. iPhone 14 Plus (430px) 짤림 해소
- `RunnerView` top-bar 에 **그리드 아이콘 = 대시보드 이동** 버튼 추가 (로그아웃 옆). 러너 진입 후 대시보드로 복귀 불가하던 이슈 해소

**11) 랜딩 페이지 APP_HOST 동적 설정** — github.io 공개 호스팅 랜딩 페이지의 QR/링크가 `location.hostname + :5173` 으로 생성돼 `zeroheat0930.github.io:5173` 처럼 잘못된 URL 이 박히던 이슈.

- `landing/index.html` 스크립트 전면 재작성 — 우선순위: `?host=...` URL 쿼리 > `localStorage.concierge.host` > `localhost` 셀프 호스팅 > `DEFAULT_HOST` (기본값 `192.168.0.51:5173`, 심사 당일 PC LAN IP 로 업데이트)
- 모든 버튼 href + QR 데이터를 동일 APP_HOST 기준 동적 생성
- 하단에 "Demo target: {host}" hint 자동 표시 — 심사 전 육안 검증용

### 2026-04-21 오후 — systemic 에러 핸들링 + 상태 머신 보강

**7) 클라이언트 `unwrapOk` systemic fix** — 기존엔 `BaseController` 가 ApiException 도 HTTP 200 + body `{status:음수, message, error}` 로 내리는데 axios interceptor `unwrapOk` 가 status 필드를 보지 않고 data 를 통째로 리턴해서 호출자 catch 블록이 절대 안 타는 systemic 이슈였음. 로그인 버그(`상태=-20` 을 "서버 오류" 로 표시), 러너 "시작" 무반응 전부 같은 원인.

- `client.js:unwrapOk` 가 `status !== 0` 이면 `{status, message, map}` 으로 reject. 모든 CCS/gr/ai/admin 호출이 catch 블록으로 정상 에러 surface.
- 결과: 러너 "시작" 실패 시 "상태 변경 실패: ..." 메시지가 뜸, 로그인 틀린 비번 시 "비밀번호가 일치하지 않습니다" 정상 표시 (프론트 기존 status 체크 가지친 코드도 일관된 catch 블록으로 수렴).

**8) CcsTaskService 상태 머신 보강** — 러너 태스크가 `ASSIGNED` 상태로 배정돼있으면 "시작" 버튼이 `IN_PROG` 전이 시도했으나 기존 규칙은 `REQ → IN_PROG` 만 허용해 백엔드가 거부하던 이슈. `ASSIGNED → IN_PROG`, `ASSIGNED → CANCELED` 두 경로 허용. 러너 시작/완료 플로우 정상 동작 확인.

### 2026-04-21 오후 (윈도우 세션 — 역할 기반 WebSocket 토픽 + CCS UI 라벨)

**4) 역할 기반 WebSocket 토픽 아키텍처** — 관리자(dongjunkorea, dept=00000) 가 부서별 토픽(`/topic/ccs/dept/HK` 등) 만 publish 되는 기존 구조에선 실시간 푸시 못 받던 문제. PMS `USER_TP` 체계(00001 시스템 / 00002 프로퍼티 / 00003 컴플렉스 / 00004 일반 / 00007~00008 객실정비) 로 스코프 분기.

- **서버**: `CcsPrincipal` 에 `userTp` 필드 + `isAdminViewer()` 헬퍼 추가. `CcsJwtService.issue()/parse()` 에 `userTp` 클레임. `CcsAuthController` 응답 map 에 `userTp`, `propCd`, `cmpxCd` 포함. `CcsTaskService.publish()` 가 이제 **3중 발행** — `/topic/ccs/dept/{deptCd}` + `/topic/ccs/cmpx/{propCd}/{cmpxCd}` + `/topic/ccs/prop/{propCd}` + 기존 `/topic/ccs/staff/{assigneeId}`
- **프론트**: `StaffDashboardView` 가 `userTp` 기반으로 subscribe 할 토픽 결정 — 00001/00002 → `/prop/{propCd}`, 00003 → `/cmpx/{propCd}/{cmpxCd}`, 그 외 → `/dept/{deptCd}`. 관리자 역할은 "내가 받기"/"완료" 버튼 숨김 + 담당자 미배정 힌트 표시 (viewer-only UI)
- 디버그 로그 `[WS-dbg]` / `[WS-gate]` 제거. 에러 핸들러만 유지

**5) 디스패처 sourceType 버그 수정** — `CcsDispatcher:33` 이 모든 게스트 요청을 하드코딩 `"GUEST_REQ"` 로 저장해 AMENITY/LATE_CO/PARKING 구분 불가. `event.eventTp()` 로 교체

**6) CCS 대시보드 라벨/필드 수정**

- 서버 응답 key `rmNo` 와 Vue 템플릿 `roomNo` 불일치 → `rmNo` 로 통일 (카드/모달/러너 PWA 전부)
- `sourceLabel()` / `statusLabel()` 한국어 라벨 함수 추가 — `AMENITY → 어메니티`, `LATE_CO → 레이트체크아웃`, `PARKING → 주차`, `HK_MU → 객실정비요청`, `REQ → 대기`, `IN_PROG → 진행중`, `DONE → 완료`, `CANCELED → 취소됨` 등
- 객실 null 일 때 `"—"` 표시로 빈 "호" 방지

**빌드**: `mvn compile` BUILD SUCCESS / `npm run build` 211 modules 0 errors

**실동작 검증 (사내 MariaDB)**:
- `dongjunkorea` (userTp=00001 시스템관리자) 로그인 → WebSocket 101 Switching Protocols → STOMP CONNECTED → `SUBSCRIBE /topic/ccs/prop/0000000001` 확인
- 게스트 어메니티 "수건 3개" 요청 → 스태프 대시보드 2초 내 자동 등장 ✅
- 태스크 카드/모달: 객실 03010호, 유형 어메니티, 상태 대기 — 모두 한국어 라벨 정상

### 2026-04-21 (윈도우 세션 — WebSocket STOMP 연결 복구 + CCS 로그인 회귀 수정)

**1) WebSocket STOMP 연결 복구** — 커밋 `360e45c` 에서 "WebSocket 미연결" 이라 5초 폴링 fallback 쓰던 상태. 원인 진단 + 수정.

- **원인**: `SecurityConfig.java` 의 `.anyRequest().denyAll()` 이 `/ws-ccs` HTTP 업그레이드 요청을 차단. `/ws-ccs` 는 `/api/**` 하위도 아니고 어떤 `permitAll` 규칙에도 매칭 안 돼 fallback 차단에 걸림. stompjs 클라이언트 코드 자체는 정상이었음.
- **수정**: `SecurityConfig` 에 `requestMatchers("/ws-ccs/**").permitAll()` 추가. STOMP CONNECT-time JWT 검증은 `CcsStompAuthInterceptor` 에서 별도 처리하므로 HTTP 레이어는 열어도 안전.
- **폴링 간격 복원**: `StaffDashboardView.vue` / `RunnerView.vue` 의 `setInterval(load, 5000)` → `30000`. WebSocket 이 주 경로, 폴링은 연결 실패 시 백업.

**2) MyBatis SQL 별칭 제거 시도 → 원복** (`17b0e24` 커밋의 내용 자체를 되돌림)

- 시도: `map-underscore-to-camel-case: true` 활용해 `AS camelCase` 별칭을 일괄 제거하려 했음
- **원복 이유**: `resultType="map"` 환경에서 Map 키의 camelCase 변환이 불안정. 사내 DB 실행 시 `user.get("userPw")` 가 null 반환하며 로그인 회귀 발생. PMS 원본이 모든 컬럼에 AS 별칭을 박아둔 게 바로 이 때문이었음
- 결과: INV_SQL.xml / PMS_SQL.xml 을 17b0e24 이전 상태로 복원 (AS camelCase 별칭 유지)

**3) CCS 로그인 회귀 수정** (BCrypt 평문 분기 + 프론트 status 핸들링)

- **백엔드 (`CcsAuthController`)**: 기존 `BCryptPasswordEncoder.matches()` 가 평문 저장된 `PMS_USER_MTR.USER_PW` 만나면 `IllegalArgumentException` → HTTP 500. BCrypt 포맷(`$2a`/`$2b`/`$2y`) 감지 시 BCrypt, 아니면 평문 동등 비교로 분기. PMS 기존 계정과 호환 유지, 미래 자체 CONCIERGE_STAFF 테이블 + BCrypt 전환 시에도 깨지지 않음
- **프론트 (`StaffLoginView.vue`)**: `BaseController.handleApiException` 이 ApiException 도 HTTP 200 + `{status:음수, ...}` 로 내려주는데, 기존 `submit()` 이 try 블록에서 `res.status` 검사 없이 `!map.token` 만 보고 전부 "서버 오류" 로 처리. `res.status !== 0` 분기 추가하여 `-20 (INVALID_PASSWORD) → "비밀번호가 일치하지 않습니다"` 등 정확한 메시지 노출. 미지의 코드는 `서버 오류 (코드번호)` 로 디버깅 용이하게
- **검증**: 사내 MariaDB (`211.34.228.191:3336`) 로 실제 `dongjunkorea` 계정 로그인 성공 → `/staff` 진입 → `selectTasksByDept` 30건 조회 확인

**빌드**: `mvn compile -o` BUILD SUCCESS / `npm run build` 211 modules 0 errors

**남은 런타임 검증**: 브라우저 DevTools Network → WS 탭 에서 `/ws-ccs` `101 Switching Protocols` + STOMP CONNECTED 확인. 게스트 요청 1건 → 스태프 대시보드 30s 내 즉시 업데이트되면 WS 작동.

**주의**: 프론트 `StaffLoginView.vue` 의 `PROP_CD = '0000000010'` 하드코딩은 데드코드 — 백엔드(`CcsAuthController`) 가 request param 을 무시하고 `PMS_USER_MTR` 로우에서 propCd/cmpxCd 를 직접 읽음. 추후 청소 예정.

### 2026-04-16 (맥북 야간 세션 2 — 상용화 보안 수정 + 테스트 + UI 완성)

**완료**:
- **상용화 Critical 보안 6건 수정**:
  - DB 비밀번호 하드코딩 제거 (환경변수 필수)
  - 어드민 인증 timing-safe 비교 (`MessageDigest.isEqual()`)
  - AI 챗 Rate Limiting (`AiRateLimiter`, 20req/min per guest)
  - WebSocket STOMP 인증 (`CcsStompAuthInterceptor`, CONNECT 시 JWT 검증)
  - CORS prod 제한 (`concierge.cors.allowed-origins` 환경변수)
  - 응답 포맷 통일 (SecurityConfig/AdminAuthInterceptor resCd→status)
- **Vue 프론트 응답 포맷 마이그레이션** — 17개 파일 `resCd/resMsg` → `status/message`
- **게스트 UI 폴리싱**:
  - `LoadingSpinner` 공용 컴포넌트 + 전 뷰 적용
  - 상태 코드 노출 제거, 토스트 3초 자동 소실 통일
  - 모바일 반응형 수정 (AmenityView 그리드, HK 버튼 스택)
  - 터치 타겟 min-height 48px, App.vue 게스트 정보 동적화
- **스태프 대시보드 컴포넌트 완성**:
  - StatsWidget 렌더 (오늘 접수/완료/평균 처리시간)
  - DeptLoadPanel 접이식 렌더 (부서원 로드 현황)
  - StaffRequestModal "새 요청" 버튼 + 모달 트리거
- **AdminCcsView CRUD 완성** — 부서 추가/수정/삭제 인라인 폼, useYn 토글, 스태프 positionCd/배지
- **유닛 테스트 26건 신규** (0건→26건):
  - AiRateLimiter, JwtService, RequestParams, Responses, CcsRoutingRuleDefault

**빌드**: `mvn test` 26건 PASS / `npm run build` 123 modules, 0 errors

### 2026-04-16 (맥북 야간 세션 — PMS MVC 인프라 이식 + JPA 완전 제거)

**목표**: 회사 개발자가 소스 열었을 때 PMS와 동일한 패턴으로 바로 이해할 수 있도록 MVC 인프라 통일.

**완료**:
- **JPA 완전 제거** — Entity 14개 + Repository 11개 + SeedDataRunner 삭제 (26파일, -1241줄)
  - `spring-boot-starter-data-jpa` 의존성 제거
  - `application.yml` 에서 `jpa.*` 설정 전부 제거 (dev/prod)
  - `spring-boot-starter-aop` 명시 추가 (JPA transitive 의존성 대체)
- **PMS Core 인프라 이식** — 7개 클래스 신규 생성:
  - `BaseController` — `@ControllerAdvice`, 통합 예외 핸들링, `ok()`/`ok(message)` 응답 빌더
  - `RequestParams` + `RequestParamsArgumentResolver` — Map 기반 파라미터 래퍼 + 자동 파싱
  - `ApiResponse` / `ApiStatus` / `ApiError` / `ApiException` — PMS 스타일 응답 체계 (`{status, message, error}`)
  - `PageableVO` — 페이징 VO
  - `WebConfig` — ObjectMapper + RequestParams 리졸버 등록
- **BizException → ApiException 전환** — 9개 서비스/컨트롤러 마이그레이션
- **CcsResponse 제거** — `Responses.MapResponse` 통일
- **GlobalExceptionHandler 제거** — `BaseController` 로 흡수
- **컨트롤러 13개 PMS 스타일 전환**:
  - `@RestController` → `@Controller extends BaseController`
  - `@GetMapping`/`@PostMapping` → `@RequestMapping(method=...)`
  - `@RequestParam`/`@RequestBody` → `RequestParams requestParams`
  - 메서드별 `@ResponseBody` 추가
- **Lombok + spring-data-commons** 의존성 추가

**빌드**: `mvn compile` — 58 sources, 0 errors

**변경 규모**: 53 files changed, +436 -1,677 lines

### 2026-04-16 (회사 세션 — JPA→MyBatis 전면 전환 + DAOL DB 직결)

**목표**: W1-1 cmpxCd 전면 도입 + 데이터 접근을 PMS 스타일 MyBatis XML 매퍼로 통일.

**결정**:
- JPA 엔티티/Repository 방식 → **MyBatis XML 매퍼 + `Map<String, Object>`** 패턴으로 전면 전환 (PMS 소스와 동일 패턴)
- H2 완전 비활성화 (pom.xml 주석, test 프로파일 주석)
- PMS 테이블은 `PMS.*` cross-schema 읽기, 컨시어지 데이터는 `INV.*` 7개 테이블

**완료**:
- `mybatis-spring-boot-starter` 3.0.3 추가, `mapper-locations: classpath:mapper/**/*_SQL.xml`, `map-underscore-to-camel-case: true`
- `PmsMapper` + `PMS_SQL.xml` — PMS_RESERVATION/PMS_COMPLEX/PMS_USER_MTR 읽기 쿼리 (6개)
- `InvMapper` + `INV_SQL.xml` — INV 7개 테이블 CRUD 쿼리 (25개+)
- **Auth 레이어**: `GuestPrincipal` 에 `cmpxCd` 추가, `JwtService` cmpxCd 클레임, `AuthService` → PmsMapper
- **GrService**: JPA 6개 Repository → PmsMapper + InvMapper. 하우스키핑은 별도 테이블 대신 `CCS_TASK(HK_*)` 로 통합
- **FeatureService**: JPA → InvMapper, `listForGuest`/`listForAdmin`/`upsertBulk` 에 cmpxCd 파라미터 추가
- **NearbyController**: `ConciergeProperty` → InvMapper `CONCIERGE_PROPERTY_EXT` (lat/lng)
- **CCS 전체**: `CcsTaskService`/`CcsTaskController`/`CcsStatsController`/`CcsDeptLoadController` → InvMapper
- **CcsAuthController**: `CcsStaff` JPA → `PmsMapper.selectUser` (PMS_USER_MTR 직접 인증)
- **CcsAdminController**: 부서 목록은 PMS_USER_MTR DEPT_CD 그룹핑으로 추출
- **CcsDispatcher** + **RequestEvent**: cmpxCd 전파
- 기존 JPA 엔티티/Repository 는 dead code 로 잔존 (다음 정리)

**PMS 유틸리티 이식**:
- `CommonDAO` — PMS 의 `toCamelCase()` Map 키 자동 변환 래퍼. 추후 SQL 별칭(AS camelCase) 제거하고 이 DAO 경유로 전환
- `TxAdviceConfig` — AOP 트랜잭션 자동 적용 (`insert*/update*/delete*/save*/proc*` 메서드). 추후 `@Transactional` 제거 가능
- `commons-lang3` 의존성 추가

**남은 것**:
- ~~기존 JPA 엔티티/Repository 삭제 정리~~ ✅
- ~~spring-boot-starter-data-jpa 의존성 제거~~ ✅
- ~~PMS MVC 인프라 이식 (BaseController, RequestParams 등)~~ ✅
- ~~Vue 프론트엔드 응답 포맷 대응 (resCd → status 변경 반영)~~ ✅
- ~~상용화 보안 Critical 6건 수정~~ ✅
- ~~유닛 테스트 신규 작성 (26건)~~ ✅
- ~~AdminCcsView CRUD 완성~~ ✅
- ~~게스트 UI 폴리싱 (로딩/모바일/토스트/터치)~~ ✅
- ~~스태프 대시보드 컴포넌트 렌더 (Stats/DeptLoad/RequestModal)~~ ✅
- ~~SQL 별칭(AS camelCase) 제거~~ — 2026-04-21 시도했으나 `resultType="map"` 환경에서 Map 키 casing 회귀로 원복. AS 별칭 유지가 맞음 (PMS 스타일과도 일치)
- ~~@Transactional 제거 → TxAdviceConfig AOP 로 대체~~ ✅ (이전 세션 완료 확인)
- ~~AmenityRequest 다건 요청 구조 변경 확인 (1 item per row)~~ ✅ (이미 품목당 1행 INSERT)
- 부팅 + 실제 쿼리 동작 검증 (사내망 필요)
- 스태프 비밀번호 해싱 (PMS 평문 → 독립 인증 테이블 검토)
- AdminCcsView 부서 CRUD 백엔드 엔드포인트 추가 (POST/PUT/DELETE)
- ~~WebSocket 실시간 연결 (현재 폴링 → STOMP 클라이언트)~~ ✅ (2026-04-21, SecurityConfig 원인 수정)
- Health Check 엔드포인트 (Spring Actuator)
- API 문서 (Swagger/OpenAPI)

**접속**: `jdbc:mariadb://211.34.228.191:3336/INV` (dev 프로파일 기본값), `dongjunkorea` 계정. 사내망/VPN 필수.

**완료된 것**:
- `application.yml` dev 프로파일 → 사내 MariaDB 직결 + `ddl-auto=none`
- `INV` 스키마에 **7개 테이블 수동 DDL 생성**:
  - `CONCIERGE_PROPERTY_EXT` — (PROP_CD, CMPX_CD) 복합 PK, lat/lng/nearbyRadius (PMS_COMPLEX 에 없는 좌표만)
  - `CONCIERGE_FEATURE` — (PROP_CD, CMPX_CD, FEATURE_CD) 복합 PK
  - `CONCIERGE_AMENITY_ITEM` — (PROP_CD, CMPX_CD, ITEM_CD) 복합 PK
  - `CONCIERGE_AMENITY_REQ` — REQ_NO bigint PK + (PROP_CD, CMPX_CD, RESV_NO) 인덱스
  - `CONCIERGE_PARKING` — 동일 구조 + CAR_NO / PMS_SYNC_YN
  - `CONCIERGE_LATE_CO` — 동일 구조 + REQ_OUT_TM / ADD_AMT
  - `CCS_TASK` — TASK_ID(varchar) PK + 부서/상태/담당자 인덱스
- `SchemaProbeRunner` 신규 — 부팅 시 PMS/INV 테이블 컬럼 덤프 (엔티티 재매핑용 일회성)
- **실제 스키마 확정** (probe 결과):
  - `PMS_RESERVATION` 137컬럼, PK `(PROP_CD, CMPX_CD, RESV_NO)`, RESV_NO varchar(10)
  - `PMS_COMPLEX` 57컬럼, PK `(PROP_CD, CMPX_CD)`, lat/lng 없음 (→ EXT 테이블 확정)
  - `PMS_PROPERTY` 22컬럼, PK `PROP_CD`, PROP_FULL_NM
  - `PMS_USER_MTR` 27컬럼, PK `USER_ID` **단일**(!) — 글로벌 유니크라 스태프 JWT 에 userId 하나로 충분
  - `PMS_CUST_MGMT` 80컬럼

**다음 작업 (이어서 해야 할 것)**:
1. PMS 읽기전용 엔티티 3개 신규: `PmsReservation`, `PmsComplex`, `PmsUserMtr` (+ `PmsProperty` 선택)
2. 기존 `Reservation` / `ConciergeProperty` 엔티티 **삭제** — PMS 쪽으로 대체
3. 6개 컨시어지 엔티티 재작성 (INV DDL 매핑, 복합 PK `@IdClass`, 감사필드 REG_USER/REG_DT/MOD_USER/MOD_DT)
   - `AmenityRequest` → table `CONCIERGE_AMENITY_REQ`, reqNo `Long` + `@GeneratedValue(IDENTITY)` 검토
   - `AmenityItem` → `CONCIERGE_AMENITY_ITEM`, 복합 PK `(propCd, cmpxCd, itemCd)`
   - `ParkingRegistration` → `CONCIERGE_PARKING`
   - `LateCheckoutRequest` → `CONCIERGE_LATE_CO`
   - `ConciergeFeature` → `CONCIERGE_FEATURE`, `ConciergeFeatureId` 에 cmpxCd 추가
   - `CcsTask` → `CCS_TASK`, cmpxCd 컬럼 이미 있음 (길이만 5 → 10 조정)
4. 신규 `PropertyExt` 엔티티 — `CONCIERGE_PROPERTY_EXT` (lat/lng/nearbyRadius)
5. Repository/Service/Controller 전역 시그니처에 `cmpxCd` 파라미터 추가
6. `JwtService` / `GuestPrincipal` / `SecurityContextUtil` 에 cmpxCd 클레임
7. `SeedDataRunner` 재작성 — 컨시어지 시드만, PMS 쪽은 건드리지 않음 (`propCd='0000000010'`, `cmpxCd='00001'` 실제 값)
8. `SchemaProbeRunner` 삭제 (일회성 툴)
9. `V2`/`V3`/`V4` Flyway 스크립트 아카이브 + `V5__inv_schema.sql` 신규 (실제 DDL과 일치)

**주의**:
- REQ_NO bigint 채번 전략 미확정 — MariaDB `AUTO_INCREMENT` 로 가는 쪽이 유력
- `PMS_USER_MTR.USE_YN`, `USER_WK_STAT` 필터링 필요 (재직 스태프만)
- 현재 `gr_*`/`concierge_*` 로컬 테이블 참조하는 Repository/Service 컴파일 에러 대량 발생 예정 — 한번에 몰아서 수정

### 2026-04-15 (맥북 밤 세션 — UI 고도화: StaffShell + 디자인 토큰 + 로그인 폴리싱)

**배경**: PMS 연동을 제거하고 나서 스태프/관리자 번들이 뷰별로 자체 상단 헤더만 쓰고 있어 내비게이션 일관성이 떨어졌고, 로그인 화면은 기능 동작 수준에 머물러 있었음. 심사 영상에서 시연 임팩트가 약할 것으로 판단해 전체적인 시각/인터랙션 완성도를 끌어올림.

**핵심 변경**:
- **디자인 토큰 centralize** — `src/assets/tokens.css` 신규. 브랜드 팔레트/뉴트럴/상태 컬러/radius/shadow/spacing/typography/motion 을 CSS 커스텀 프로퍼티로 일괄 정의해 두 번들이 공유
- **`StaffShell.vue` 레이아웃 신규** — 스태프/관리자 공용 좌측 LNB
  - 그라디언트 브랜드 헤더 (`DAOL CCS`, 로고 블록)
  - 스태프/관리자 그룹별 분리된 내비게이션 (sessionStorage 토큰 기반 조건부 렌더)
  - 하단 유저 정보(아바타 이니셜 + 이름 + 역할) + SVG 로그아웃 아이콘
  - 모바일(< 720px) 에서는 상단 가로 바로 자동 재배치
- **`StaffApp.vue` 전환** — 라우트별 셸 결정:
  - `/staff`, `/admin/*` → `StaffShell` 로 감싸기
  - `/runner` → 자체 모바일 PWA 셸 유지(감싸지 않음)
  - `*/login` → 가운데 정렬된 플레인 셸 + 방사 그라디언트 배경
- **뷰에서 중복 헤더 제거** — 내비/로그아웃이 셸로 이동해 뷰에는 페이지 타이틀만 남김
  - `StaffDashboardView.vue` — `.head` 블록 제거, `page-head` 타이틀 + 부제로 단순화
  - `AdminFeaturesView.vue` — 툴바의 로그아웃 버튼 제거
- **로그인 화면 재작성** — `StaffLoginView` / `AdminLoginView`
  - 그라디언트 로고 블록 + 브랜드 타이틀 + tagline
  - 라이즈 인 애니메이션(`@keyframes rise`)
  - 로딩 버튼 안에 인라인 스피너
  - 스태프 로그인: 데모 계정 4종(hk1/fr1/eng1/fb1) 빠른 입력 `<details>` 패널
  - 관리자 로그인: `CONCIERGE_ADMIN_PW` 환경변수 안내 힌트
- **페이지 전환** — 게스트/스태프 App 양쪽에 `<transition name="page">` + `out-in` 모드 적용, 8px 이동 + opacity 페이드
- **글로벌 스타일** — `main.css` 에 `@import tokens.css`, 브랜드 focus ring, 선택 색상, 스크롤바 폴리싱

**빌드 결과**:
- `npm run build` — 115 modules, 0 errors
- 게스트: `main-*.js` 22.8 KB / `main-*.css` 13.7 KB
- 스태프: `staff-*.js` 22.5 KB / `staff-*.css` 23.0 KB
- shared: `_plugin-vue_export-helper-*.js` 140.2 KB

### 2026-04-15 (맥북 저녁 세션 — 게스트/스태프 경계 정리 + PMS 연동 제거)

**배경**: 스태프 로그인 후에도 게스트 LNB(어메니티/HK/레이트/챗/주차)가 왼쪽에 계속 보이고, 단일 JS 번들이 스태프/관리자 코드를 게스트 태블릿까지 내려보내고 있던 문제. 추가로 CCS 가 완성된 이상 PMS KOK_EVENT 브릿지는 이중 경로라 CCS 단일 경로로 재정리.

**커밋**:
- `6d18a46` fix(ui): 스태프/관리자 라우트에서 게스트 LNB 제거 — `App.vue` 를 `isGuestRoute` 플래그로 조건부 셸 분기, 게스트 부트(`authenticateGuest`+`loadFeatures`) 도 게스트 경로일 때만 실행
- `dd934b7` feat(build): Vite 멀티 엔트리로 게스트/스태프 번들 완전 분리 — `index.html` → `main.js` → 게스트 라우트만, `staff.html` → `main-staff.js` → 스태프+관리자 라우트만. 트리쉐이킹으로 상대편 코드가 번들에 포함되지 않음
- (이번 커밋) feat(dispatcher): PMS KOK_EVENT 브릿지 + `PmsCarRegistryAdapter` 완전 제거 — CCS 를 게스트 요청의 유일한 전달 경로로 확정

**변경 내역**:
- `api_server/src/main/java/com/daol/concierge/dispatcher/DaolKokEventDispatcher.java` — 삭제
- `api_server/src/main/java/com/daol/concierge/pms/PmsCarRegistryAdapter.java` + `pms/` 패키지 디렉토리 — 삭제
- `api_server/src/main/java/com/daol/concierge/gr/service/GrService.java` — `PmsCarRegistryAdapter` import/필드/호출 블록 3 곳 제거, 디스패처 파이프라인만 남김
- `api_server/src/main/java/com/daol/concierge/ccs/routing/CcsDispatcher.java` — 멀티 테넌시 설정을 `pms.*` → `concierge.tenant.*` 로 rename (PMS 의미 분리)
- `api_server/src/main/resources/application.yml` — `pms:` 블록 + `concierge.dispatcher.daol` 플래그 제거, `concierge.tenant:` 블록 신규
- `scripts/PmsProbe*.java` — 삭제 (사내 PMS dev DB 탐색 유틸, 이제 불필요)
- `README.md` — "사내 PMS 수정 기록" 섹션 통째 제거, 환경변수/디스패처 플래그 표 정리, 아키텍처 설명을 CCS 중심으로 재작성, 진행 로그 갱신

**결과**:
- 게스트 요청은 `CcsDispatcher` 로만 전달 → `CcsTask` 생성 → 부서별 라우팅 → WebSocket 푸시. PMS 호출 경로 0
- 게스트 번들(`main-*.js` 22.6 KB / CSS 11.6 KB) 안에 스태프·관리자 뷰 코드 포함 0
- 스태프 번들(`staff-*.js` 18.1 KB / CSS 12.9 KB) 는 `/staff.html` 진입 시에만 다운로드
- 공용 청크(`_plugin-vue_export-helper-*.js` 139.7 KB) 는 한 번만 다운로드

**검증**:
- `npm run build` — 113 modules transformed, 0 errors, 두 엔트리(`index.html` + `staff.html`) 정상 출력
- Java 변경분은 정적 검증(이 맥북엔 JDK/Maven 미설치) — 삭제/수정 후 `grep -r` 로 PMS 참조 0 확인, 남은 `CcsDispatcher` 의 `@Value` 는 신규 `concierge.tenant.*` 키와 일치
### 2026-04-15 (Ralph 세션 — CCS 다올버전 풀 구축)

**목표**: AI 경연대회 본질 → 상용화 고려 X → CCS-lite 초경량 뷰 확장

**결과**: US-001~014 14개 스토리 완료, 74 Java 소스 + Vue 107 모듈

**핵심 변경**:
- **Dispatcher refactor** — `List<RequestDispatcher>` 다중 디스패처 구조, 3개 기능 플래그 (CCS/DAOL/EXTERNAL), `InternalOnlyDispatcher` 삭제
- **CCS 패키지 신규** — `com.daol.concierge.ccs.*` (domain/repo/service/auth/routing/task/websocket/stats/admin) 14개 Java 소스
- **WebSocket 실시간 푸시** — `/ws-ccs` STOMP 엔드포인트, 부서별·개인별 토픽
- **Runner PWA** — 모바일 전용 뷰, 스와이프 완료, 홈화면 설치 manifest
- **부서/스태프 관리 어드민** — `/admin/ccs` 부서 CRUD + 라우팅 규칙 편집

**빌드**: Maven BUILD SUCCESS (74 sources) / `npm run build` OK (107 modules)

### 2026-04-15 (윈도우 세션) — 독립 제품화 + 전기능 완성

**오전: 디스패처 추상화 + 기능 플래그 + 어드민 골격**
- PMS `KOK_EVENT` 실시간 파이프라인 전체 분석 (`/api/mkiosk/event` → `setKokEvent()` → Redis pub → STOMP `/topic` → `frame.js:WS_MSG()` → 팝업)
- PMS 3줄 수정으로 우회 로그인 없이 `/api/mkiosk/event` 호출 가능하게 (수정 1)
- 신규 패키지 `com.daol.concierge.dispatcher` — `RequestDispatcher` 인터페이스 + `DaolKokEventDispatcher`/`ExternalApiDispatcher`/`InternalOnlyDispatcher` (기본 `internal`)
- 신규 패키지 `com.daol.concierge.feature` — `ConciergeFeature`/`ConciergeProperty` 엔티티, `FeatureService`, 게스트/어드민 컨트롤러, `AdminAuthInterceptor`
- Vue LNB: 하드코딩 → `featureStore` + `v-for` 동적 렌더, `router.beforeEach` 가드
- 기존 `PmsKokEventClient` 삭제 → `DaolKokEventDispatcher` 로 흡수

**오후: NEARBY + 어드민 UI 폴리싱**
- 신규 패키지 `com.daol.concierge.nearby` — `NearbyPlace` record, `NearbyProvider` 인터페이스, `MockNearbyProvider`(기본, 서울시청 인근 실제 상호) + `KakaoNearbyProvider`(`havingValue="kakao"`)
- `ConciergeProperty` 에 `lat`/`lng` 추가, `SeedDataRunner` 가 HQ 에 `37.5665, 126.9780` 시드
- `NearbyView.vue` — 5 카테고리 탭 + 카드 리스트 + 카테고리별 캐싱
- `AdminLoginView.vue` 신규 + `AdminFeaturesView.vue` 전면 재작성: `window.prompt` 제거, 카드 레이아웃, iOS 토글 스위치 (순수 CSS, 44×24 트랙), `configJson` 아코디언 + blur JSON 검증, 저장 토스트 fade-out 2s

**저녁: PARKING MVP + 랜딩 갱신 + PMS 차량 연동**
- 신규 엔티티 `ParkingRegistration` + 레포, `GrService.insertParkingReq` + `getParkingList` + dispatcher 연동
- `GrController` 에 `GET/POST /api/gr/parking*` 추가
- `ParkingView.vue` — 등록 폼(차량번호/차종/메모) + 카드 이력 리스트
- **PMS 수정 2**: `/api/ko/client/registerConciergeCar` 엔드포인트 신규 (ClientController + KoService + SecurityConfig) — PMS_CAR_NO 테이블 직접 등록
- **우리쪽 `PmsCarRegistryAdapter`** — `concierge.dispatcher=daol` 일 때만 활성화, `GrService.insertParkingReq` 에서 optional 호출 (`@Autowired(required=false)`), PMS 통신 실패해도 게스트 접수 성공 유지
- 랜딩페이지 (`landing/index.html`) — 기능 6개로 확장 + 신규 "아키텍처 포인트" 섹션 추가

**배포 시도 — OCI Ampere A1 Out of capacity (미해결)**
- Always Free 인스턴스 A1.Flex 2/12 춘천 리전 가입 완료, 재고 고갈로 반복 실패
- Cloud Shell `oracle_macro.sh` 백그라운드 루프(`nohup`, 180s 간격) 가동 중 — 새벽 시간대 잡히면 이어서 배포
- 대체안: Cloudflare Tunnel + 상시구동 PC (미결정)

**빌드**: Maven BUILD SUCCESS (53 sources) / `npm run build` OK (99 modules, 167 KB)

### 2026-04-14 (윈도우 세션, 압축) — 상용화 로드맵 US-001~007 완료
- US-001 LLM 프록시, US-002 CORS/baseURL, US-003 JPA+H2, US-004 게스트 JWT, US-005 멀티 프로퍼티, US-006 Docker, US-007 Capacitor — 전부 ✅
- PostgreSQL → **MariaDB** 교체 (회사 PMS 스택 일치)
- `bash api_server/smoke-test.sh` 27개 체크 PASS

### 2026-04-13 (맥북 세션, 압축) — AI 챗봇 + PWA + 대시보드
- `feat/ai-chat-concierge` 브랜치 AI 챗봇 풀구현 (룰 기반 13/13 유닛테스트)
- 이후 오늘 오전에 `ChatFab` 플로팅 버튼 → `ChatView` 탭으로 흡수, `DashboardView` 제거(어드민 UI 로 대체)

---

## 📋 남은 것

> **현황 (2026-04-15)**: US-001~014 전부 완료. CCS 풀 구축 완료.
> 심사 영상은 **게스트 앱 + 스태프 CCS-lite 풀 스토리**로 촬영 예정.

### 완료된 항목 ✅
- ✅ Staff / Department 엔티티 + 레포
- ✅ Task 테이블 + 상태 머신 (REQ→ASSIGNED→IN_PROG→DONE/CANCELED)
- ✅ 라우팅 규칙 (AMENITY→HK, LATE_CO→FR, PARKING→ENG)
- ✅ Staff 로그인 + JWT (type=STAFF 클레임)
- ✅ WebSocket 실시간 푸시 (/ws-ccs STOMP)
- ✅ `/staff` 웹 대시보드
- ✅ `/runner` 러너 PWA
- ✅ 부서장 화면 (부서원 로드 + 재배정)
- ✅ 통계 위젯 (오늘 접수/완료/평균 처리시간)
- ✅ 어드민 UI `/admin/ccs` (부서/라우팅 규칙)
- ✅ E2E README 문서화

### 남은 것
1. **카카오 REST API 키** 발급 → `NEARBY_PROVIDER=kakao` (실제 주변 데이터)
2. **배포** — OCI Ampere 또는 Hetzner CX22 (€4.50/월) + Docker compose + duckdns
3. **시연 영상 촬영** — 게스트 → CCS 풀 스토리 7단계
4. **시연 스토리보드 1단계 QR 출력** — 체크인 시점 게스트 JWT 가 박힌 QR 이미지 엔드포인트/화면 (현재 미구현, 영상 촬영 전 필수)
5. **`staff.html` PWA 메타 보강** — `runner-manifest.json` link, apple-touch-icon, apple-mobile-web-app 메타 (러너 PWA 홈화면 설치 가능해야 함)
6. **smoke-test.sh 회귀 보강** — CCS/Nearby/Parking/Admin 엔드포인트 케이스 추가
7. **nginx 배포 가드** — `/staff.html` 경로에 사내 IP 화이트리스트 또는 BasicAuth/Cloudflare Access

### 시연 영상 스토리보드 (7단계)
1. 프론트데스크 체크인 완료 → 세션 자동 생성 + QR 출력
2. 게스트 QR 스캔 → 본인 이름/방번호로 앱 자동 진입 (다국어 자동 선택)
3. 게스트 "수건 2개 주세요" 자연어 입력 → AI 의도 파싱
4. **요청이 하우스키핑 부서에 다이렉트 라우팅** → 러너 PWA 에 푸시 알림
5. 러너가 스와이프 완료 → 스태프 대시보드 상태 업데이트
6. 주차 차량 등록 → 엔지니어링 부서로 라우팅 (부서별 라우팅 강조)
7. 체크아웃 → 게스트 앱 자동 종료 화면

### 스코프 아웃 (의식적으로 안 만듦)
- ❌ SLA 타이머 + 자동 에스컬레이션
- ❌ 직원간 채팅 (개발2팀 영역)
- ❌ 음성/무전 연동
- ❌ 유료 결제 PG
- ❌ 고급 분석 대시보드

### 장기 과제 (심사 후)
- 멀티 프로퍼티 온보딩 플로우 (여러 호텔 자동 등록)
- 외부 운영 시스템 REST 훅 어댑터 (`CONCIERGE_DISPATCHER_EXTERNAL=true` 경로 활용)
- 직원간 채팅
- SLA + 에스컬레이션
- 유료 결제 PG
