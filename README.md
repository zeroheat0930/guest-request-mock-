# 🏨 다올 컨시어지 (Guest Request)

호텔 투숙객이 QR/태블릿으로 요청을 보내면 **회사 PMS 로 실시간 전달**되는 독립 제품 프로젝트.
공모전 Mock 에서 시작해, 디스패처 추상화·기능 플래그 시스템을 갖춘 **타 PMS 이식 가능한 상용 수준**으로 진화.

---

## 📦 구성

```
guest-request-mock/
 ├ api_server/    ← Spring Boot 3.2 / Java 17 / JPA (H2 dev, MariaDB prod)
 ├ vue_client/    ← Vue 3 + Vite + Vue Router (태블릿/폰 겸용)
 ├ landing/       ← QR 랜딩 페이지 (순수 HTML, 심사 제출용)
 └ scripts/       ← PMS dev DB 탐색 유틸 (PmsProbe*.java)
```

### 기술 스택
| 레이어 | 스택 |
|---|---|
| 프론트 | Vue 3, Vite, Vue Router, Axios, PWA |
| 백엔드 | Spring Boot 3.2, Java 17, JPA, Jackson, java.net.http |
| 저장소 | H2 파일 모드 (dev) / MariaDB (prod, 회사 PMS 스택과 일치) |
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
ANTHROPIC_API_KEY=...           # AI 챗봇 활성화 (비어있으면 룰 기반 폴백)
CONCIERGE_DISPATCHER=internal   # internal(기본) | daol | external
PMS_BASE_URL=http://localhost:8090
PMS_PROP_CD=...
PMS_CMPX_CD=...
CONCIERGE_ADMIN_PW=...          # 어드민 UI 패스워드 (미설정 시 /admin/** = 503)
KAKAO_REST_API_KEY=...          # NEARBY 실제 데이터 (미설정 시 mock)
NEARBY_PROVIDER=mock            # mock(기본) | kakao
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

**어드민 UI** (`/admin/features`)
- 로그인 화면 (X-Admin-Token 검증, sessionStorage 보관, 401 시 자동 리다이렉트)
- 기능 플래그 토글 (iOS 스타일 스위치, sortOrd 편집, configJson 아코디언 + JSON 검증)
- 저장 완료 토스트 (2s fade)

**아키텍처**
- **디스패처 추상화** — `RequestDispatcher` 인터페이스 + 3 구현체(`daol` / `external` / `internal`, `@ConditionalOnProperty`)
- **기능 플래그** — `CONCIERGE_FEATURE` 테이블, 프로퍼티별 메뉴 on/off, 어드민에서 실시간 토글
- **게스트 JWT 인증** — `SecurityContextUtil.requirePrincipal()` 로 propCd/rsvNo 격리, 본인 데이터만 접근
- **PMS 실시간 연동** — 디스패처=daol 일 때 `KOK_EVENT` 발행 + (주차는) `PMS_CAR_NO` 등록까지 이중 연동

---

## 🔌 API 명세

Base: `http://localhost:8080/api`
응답 포맷 (PMS 스타일): `{ resCd, resMsg, map }`

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

### 응답 코드
| Code | 의미 |
|---|---|
| `0000` | SUCCESS |
| `9001` | 필수값 누락 |
| `9002` | 코드 오류 |
| `9003` | 최대수량 초과 |
| `9010` | 프로퍼티 좌표 미설정 (NEARBY) |
| `9102` | 권한 없음 |
| `9404` | 리소스 없음 |
| `9999` | 미처리 예외 |

### 샘플 데이터 (SeedDataRunner)
- 프로퍼티: `HQ` (서울시청 좌표 `37.5665, 126.9780`)
- 예약: `R2026041300001` (1205호/HONG GILDONG/ko_KR), `R2026041300002` (0807호/JOHN SMITH/en_US)
- 품목: `AM001~AM005` (수건/생수/비누/샴푸/칫솔세트)
- 기능 플래그 6건: AMENITY/HK/LATE_CO/CHAT/NEARBY/PARKING 전부 Y

---

## 🔧 사내 PMS 수정 기록 ⚠️

> **중요**: 아래는 회사 PMS 리포(`C:\DAOL\PMS`)에 반영한 수정사항이다.
> 이 리포에는 포함되지 않지만 **사내 배포 시 동일한 패치를 적용**해야 컨시어지가 정상 동작한다.
> 전부 **기존 로직 영향 0** — 새 엔드포인트 추가 + null 안전 분기만 있음.

### 수정 1 — KOK_EVENT 브릿지 (2026-04-15)
**목적**: 컨시어지 요청을 프론트데스크 팝업(`axToast`/`kok-chat.jsp`)으로 실시간 전달.

| 파일 | 변경 |
|---|---|
| `com/daol/pms/SecurityConfig.java` | `ignorePagesPost` 배열에 `"/api/mkiosk/event"` 추가 (permitAll 1줄) |
| `com/daol/pms/cc/service/CcService.java` | `setKokEvent()` 의 `SessionUtils.getCurrentUser()` 에 null 체크 + `userId` param fallback (`"CONCIERGE"`) |

**영향**: 기존 호출자 3곳(`KokMngtController`, `MkioskKokMngtController`, `MkioskApiController`)은 전부 로그인 세션이 있어 `user != null` 분기로 들어감 → 동작 100% 동일. 오히려 NPE 방어 추가.

### 수정 2 — PMS_CAR_NO 차량 등록 엔드포인트 (2026-04-15)
**목적**: 컨시어지에서 등록한 차량이 프론트데스크 차량 관리 모달에 그대로 나타나게.

| 파일 | 변경 |
|---|---|
| `com/daol/pms/SecurityConfig.java` | `ignorePagesPost` 에 `"/api/ko/client/registerConciergeCar"` 추가 |
| `com/daol/pms/ko/ClientController.java` | `POST /registerConciergeCar` 엔드포인트 신규 (6줄) |
| `com/daol/pms/ko/service/KoService.java` | `registerConciergeCar(Map)` 메서드 신규 — `carTp=RM`, `reprCarYn=N`, `procTp=I`, `userId=CONCIERGE` 기본값 채우고 기존 `ko.setKokCehckInCarNo` 매퍼 재사용 (12줄) |

**영향**: 완전 신규 메서드/엔드포인트. 기존 `setKokCehckInCarNo` 매퍼 재사용만 함. 기존 호출 경로 건드리지 않음.

### 배포 체크리스트
- [ ] 위 4개 파일 패치
- [ ] PMS 재빌드/재배포
- [ ] 우리 `application.yml` 에 `CONCIERGE_DISPATCHER=daol` + `PMS_BASE_URL` + `PMS_PROP_CD` + `PMS_CMPX_CD` 설정
- [ ] 스모크: 우리 컨시어지에서 어메니티 요청 → PMS 프론트데스크 `axToast` 확인
- [ ] 스모크: 주차 등록 → PMS 차량 관리 모달 조회 → 등록된 차량 확인

---

## 🎨 PMS 스타일 일치 포인트

사내 이식 시 일관성을 위해 아래 규칙을 따름:

- Controller: `@Controller + @ResponseBody + @RequestMapping("/api/...")`
- Response: `Responses.MapResponse.of(map)` / `ListResponse.of(list)` / `ok()` / `fail(cd, msg)`
- 예외: `BizException(resCd, resMsg)` → `GlobalExceptionHandler` 변환
- 네이밍: camelCase 약어 (`rsvNo`, `roomNo`, `chkOutTm`, `hkStatCd`, `procStatCd`, `rateTpCd`, `addAmt`, `curCd`)
- 서비스: 한국어 주석만 (변수/메서드명은 영문)
- 헬퍼 최소화 (인라인 우선)
- DDL 파일 금지 → JPA `ddl-auto=update` 또는 Flyway 마이그레이션
- 들여쓰기: 탭

---

## 💡 프로젝트 정체성 (압축본)

**"버튼 탭 기반 컨시어지 태블릿 → 자연어 대화형으로 바꾼 버전"** + **타 PMS 이식 가능한 독립 제품**

### 차별화 포인트
1. **언어 장벽 제거** — 해외 관광객 대응, 메뉴 번역 유지보수 불필요
2. **롱테일 요청 커버** — "베개 하나 더", "방이 너무 추워요" 도 AI 가 적절한 티켓으로 라우팅
3. **자연스러운 UX** — 노인/비IT층도 말만 하면 됨, 레이트CO 전환율 상승 기대
4. **이식성** — 디스패처 어댑터 1개만 갈아끼우면 타사 PMS 연동 가능

### 핵심 결정
- 플랫폼: **Vue 3 + PWA** (게스트 1~2박 특성상 앱 설치 저항 = 전환율 사망 → QR 웹앱이 정답)
- DB: **MariaDB** (회사 PMS 스택 일치) + H2 파일 모드(dev)
- 인증: 게스트 JWT, 모든 엔티티 `propCd` 컬럼 격리 (멀티 테넌시)
- 아키텍처: 컨시어지가 **자체 DB + 자체 어드민 UI 의 주인**, PMS 는 어댑터 대상일 뿐

---

## 🗓️ 진행 로그

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

## 📋 남은 것 (2026-04-16 ~ 05-20 심사, 5주 일정)

> **결정 (2026-04-15 막바지)**: AI 경연대회가 본질, 상용화는 나중 문제.
> 개발2팀 직원호출앱과 역할 겹침을 인지하고도 **CCS-lite 를 직접 구축**하기로 확정.
> "더 잘 만들면 그쪽이 API 연동한다" 입장. 심사 영상은 **게스트 앱 + 스태프 CCS-lite 풀 스토리**로 촬영.

### W1 (4/16~4/22) — 기반 리팩토링 + 체크인/체크아웃 풀 플로우
1. **cmpxCd 전면 도입** — 엔티티 6개 + 복합 PK + JWT + SecurityContextUtil + Seed `('0000000010','00001')` + V5 마이그레이션
2. **AI → PMS 실연동 스모크** — `env.local.bat` 에 `CONCIERGE_DISPATCHER=daol` + PMS_* 추가, 챗봇 "수건 2개" → PMS axToast 확인
3. **체크인 감지 → 세션 자동 발급** — PMS 체크인 hook 또는 PMS_RESERVATION 주기 폴링
4. **QR 생성 엔드포인트** — `GET /api/concierge/session/qr?rsvNo=` → PNG
5. **객실 태블릿 room-based auth** — roomNo 기반 장기 토큰, WebSocket 푸시로 자동 전환
6. **체크아웃 → 세션 블랙리스트** + `CheckedOutView.vue`

### W2~W3 (4/23~5/6) — CCS-lite 풀타임 2주 ⭐
7. **Staff / Department 엔티티** — 자체 DB, PMS SY 유저 재사용 X
8. **Task 테이블 + 상태 머신** — `REQ → ASSIGNED → IN_PROGRESS → DONE/CANCELED`
9. **라우팅 규칙** — 요청 타입 → 부서 매핑 (`AMENITY→HK`, `LATE_CO→FR`, `PARKING→ENG`). configJson 편집 가능
10. **Staff 로그인 + JWT** — 게스트 JWT 와 별도 인증 모델
11. **WebSocket 실시간 푸시** — 부서별 토픽 `/topic/staff/{deptCd}`
12. **`/staff` 웹 대시보드** — 요청 피드 / 필터 / 할당 / 완료
13. **`/runner` 러너 PWA** — 모바일, 스와이프 완료, 브라우저 푸시알림
14. **부서장 화면** — 부서원 로드 + 수동 재배정
15. **통계 위젯** — 접수/완료/평균 처리 시간
16. **어드민 UI "부서/라우팅 규칙" 탭 추가**
17. **E2E 테스트** — 게스트→스태프 풀 시나리오

### W4 (5/7~5/13) — UI 리디자인 + 시연 준비
18. **어드민 UI 리디자인** — 레퍼런스 확인 필요 (Linear/Vercel/Notion/Shadcn)
19. **LNB 푸터 동적화** — 하드코딩 → authBootstrap 바인딩
20. **rsv-select 데모 토글** — `import.meta.env.DEV` 가드
21. **랜딩페이지 폴리싱** — GitHub Pages 배포 + 시연 영상 iframe
22. **시연 영상 촬영** — 7단계 풀 스토리
23. **카카오 REST API 키** 발급 → `NEARBY_PROVIDER=kakao`

### W5 (5/14~5/20) — 배포 안정화 + 리허설
24. **배포** — OCI Ampere 잡히면 / 아니면 Hetzner CX22 (€4.50/월) + Docker compose + duckdns
25. **버그픽스 + 리허설** — 실제 심사 환경 시뮬레이션

### 스코프 아웃 (의식적으로 안 만듦)
- ❌ SLA 타이머 + 자동 에스컬레이션
- ❌ 직원간 채팅 (개발2팀 영역)
- ❌ 음성/무전 연동
- ❌ 유료 결제 PG
- ❌ 고급 분석 대시보드

### 시연 영상 스토리보드 (7단계 확정)
1. 프론트데스크 체크인 완료 → 우리 백엔드 세션 자동 생성 + QR 출력
2. 게스트가 QR 스캔 → 본인 이름/방번호로 앱 자동 진입 (다국어 자동 선택)
3. 게스트가 "수건 2개 주세요" 자연어 입력 → AI 의도 파싱
4. **요청이 하우스키핑 부서에 다이렉트 라우팅** → 러너 PWA 에 푸시 알림
5. 러너가 스와이프 완료 → 스태프 대시보드 상태 업데이트 → 게스트 앱에도 "처리 완료" 알림
6. 주차 차량 등록 → 엔지니어링 부서로 라우팅 (부서별 라우팅 강조)
7. 체크아웃 → 게스트 앱 자동 종료 화면

### 장기 과제 (심사 후)
- `PmsComplexSyncJob` — 여러 호텔 자동 등록
- 직원간 채팅 (개발2팀 영역이면 연동만)
- SLA + 에스컬레이션
- 유료 결제 PG
