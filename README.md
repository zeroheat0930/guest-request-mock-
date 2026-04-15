# 🏨 다올 컨시어지 Mock (Guest Request)

대회/데모용 **투숙객 요청 컨시어지** Mock 프로젝트.
회사 PMS 스타일(응답 포맷, 네이밍 규칙, Controller/Service 레이어)을 흉내 내서
집/맥북에서 이어서 작업할 수 있도록 구성.

---

## 📦 구성

```
guest-request-mock/
 ├ api_server/    ← Spring Boot 3.2 / Java 17 (인메모리 Mock)
 └ vue_client/    ← Vue 3 + Vite + Axios (태블릿 지향 UI)
```

### 기술 스택
| 레이어 | 스택 | 비고 |
|---|---|---|
| 프론트 | Vue 3, Vite, Vue Router, Axios | 회사 신규 프로젝트 스타일 |
| 백엔드 | Spring Boot 3.2, Java 17 | PMS `RegCardMgmtController` 패턴 복제 |
| 저장소 | 인메모리 (`GrService` 내부 `Map`/`List`) | 재시작 시 초기화. DDL 파일 없음 |

---

## 🚀 로컬 실행

### 1. 백엔드 (터미널 1)
```bash
cd api_server
mvn spring-boot:run
# → http://localhost:8080
```

### 2. 프론트 (터미널 2)
```bash
cd vue_client
npm install
npm run dev
# → http://localhost:5173
```

CORS는 `WebConfig.java`에서 `localhost:5173`, `localhost:4173` 허용됨.

---

## 🔌 API 명세

Base: `http://localhost:8080/api/gr`
응답 포맷 (PMS 스타일): `{ resCd, resMsg, map }`

| Method | Path | 설명 |
|---|---|---|
| GET  | `/reservation?rsvNo=`  | 예약 단건 조회 (perUseLang 포함) |
| GET  | `/reservation/list`    | 예약 목록 조회 |
| GET  | `/amenity/items`       | 어메니티 품목 마스터 조회 |
| GET  | `/amenity/list?rsvNo=` | 어메니티 요청 목록 |
| POST | `/amenity`             | 어메니티 요청 등록 |
| GET  | `/housekeeping?rsvNo=` | 하우스키핑 현재 상태 |
| POST | `/housekeeping`        | 상태 변경 (`MU`/`DND`/`CLR`) |
| GET  | `/late-checkout?rsvNo=&reqOutTm=` | 레이트 체크아웃 가능 여부 + 요금 |
| POST | `/late-checkout`       | 레이트 체크아웃 신청 |

### 응답 코드
| Code | 의미 |
|---|---|
| `0000` | SUCCESS |
| `9001` | 필수값 누락 |
| `9002` | 코드 오류 |
| `9003` | 최대수량 초과 |
| `9404` | 예약 없음 |
| `9999` | 미처리 예외 |

### 샘플 데이터
- 예약: `R2026041300001` (1205호 / HONG GILDONG / ko_KR), `R2026041300002` (0807호 / JOHN SMITH / en_US)
- 품목: `AM001~AM005` (수건/생수/비누/샴푸/칫솔세트)

### 레이트 체크아웃 요금 테이블
| 연장 시간 | 요금 |
|---|---|
| +1~2h | 무료 (FREE) |
| +3~5h | 50,000 KRW (HALF) |
| +6~8h | 100,000 KRW (FULL) |
| +9h~  | 불가 |

---

## 📱 태블릿/앱 배포

### PWA (권장)
현재 코드 그대로 `npm run build` → `dist/`를 정적 서빙하면 동작.
태블릿 크롬에서 "홈 화면에 추가" → 풀스크린 앱처럼 사용 가능.
(manifest.json + service worker는 아직 미포함, 아래 Todo 참고)

### Capacitor 네이티브 랩핑
네이티브 앱 스토어 배포가 필요하면:
```bash
cd vue_client
npm install @capacitor/core @capacitor/cli @capacitor/android @capacitor/ios
npx cap init
npx cap add android
npx cap add ios
npm run build && npx cap sync
```

---

## 🤖 AI 다국어 채팅 컨시어지 (구현 완료)

`feat/ai-chat-concierge` 브랜치에서 다음이 구현됨:

- **플로팅 챗봇 (`ChatFab.vue`)** — 우측 하단 떠 있는 버튼 → 360px 채팅창
- **자연어 → 의도 파싱 (`src/chat/intent.js`)**
  - 기본: **룰 기반** (한/영/일/중 키워드 사전, API 키 불필요, 오프라인 동작)
  - LLM 모드: `.env.local`에 `VITE_ANTHROPIC_API_KEY` 넣으면 Claude API 호출, 실패 시 룰로 자동 폴백
- **다국어 메시지 사전 (`src/i18n/messages.js`)** — ko_KR/en_US/ja_JP/zh_CN, 예약의 `perUseLang`에 자동 매칭
- **예약 메타 API** — `GET /api/gr/reservation`, `GET /api/gr/reservation/list` (백엔드 신규)
- **레이트 체크아웃 2단계 확인 플로우** — 챗봇이 요금 보여주고 "네/yes/はい/是" 받으면 실제 신청

### LLM 모드 활성화

```bash
cp vue_client/.env.local.example vue_client/.env.local
# .env.local에 키 입력
```

### 룰 기반 동작 예시 (13/13 단위테스트 통과)

| 입력 | 호출되는 API | payload |
|---|---|---|
| `수건 2개 가져다 주세요` | `POST /amenity` | `{itemList:[{itemCd:"AM001",qty:2}]}` |
| `タオルを2枚ください` | `POST /amenity` | `{itemList:[{itemCd:"AM001",qty:2}]}` |
| `请给我两瓶水` | `POST /amenity` | `{itemList:[{itemCd:"AM002",qty:2}]}` |
| `please bring three towels` | `POST /amenity` | `{itemList:[{itemCd:"AM001",qty:3}]}` |
| `방해금지 부탁` / `do not disturb` | `POST /housekeeping` | `{hkStatCd:"DND"}` |
| `방 청소 해주세요` | `POST /housekeeping` | `{hkStatCd:"MU"}` |
| `2시간 늦게 나갈 수 있나요?` | `GET /late-checkout` | `{reqOutTm:"1300"}` |
| `extend checkout to 2pm` | `GET /late-checkout` | `{reqOutTm:"1400"}` |

## 📊 대시보드 + PWA (구현 완료)

- **`/dashboard` 라우트** — 어메니티 요청 폴링(3/5/10초 선택), 신규 요청 노란색 플래시
- **PWA manifest** — `public/manifest.json` + `icon.svg` 추가, `npm run build` 후 `dist/` 정적 서빙하면 태블릿 "홈 화면에 추가" 가능
- Apple `apple-mobile-web-app-*` 메타태그 + `theme-color` 지정

---

## 🍎 맥북에서 이어서 할 작업 (Todo)

### ⭐ 핵심: AI 다국어 채팅 컨시어지 기능

아래 프롬프트를 맥북 Claude에 그대로 붙여넣으면 이어서 작업 가능:

> **[autopilot]** 방금 가져온 Vue 3 클라이언트 코드에 **'AI 다국어 채팅 컨시어지'** 기능을 얹을 거야.
>
> **1. 챗봇 UI 추가**
> 모든 화면 우측 하단에 떠 있는 플로팅 챗봇 버튼을 만들고, 누르면 깔끔한 채팅창(Chat Window)이 열리게 해줘. `App.vue`에 `<ChatFab />` 컴포넌트로 얹는 방식이 깔끔할 듯.
>
> **2. AI 의도 파악 및 API 자동 호출 로직 (핵심 ⭐)**
> 유저가 자연어로 입력하면 (예: *"내일 2시간 늦게 나갈 수 있나요?"*, *"タオルを2枚ください"*, *"请给我两瓶水"*),
> 프론트엔드에서 **LLM(Claude API 또는 GPT API)** 을 호출해서 아래 구조의 JSON으로 파싱:
> ```json
> {
>   "intent": "amenity | housekeeping | late_checkout | chat",
>   "reply": "사용자에게 보여줄 답변 텍스트",
>   "payload": { ... API 호출에 필요한 파라미터 ... }
> }
> ```
> - `intent === 'chat'` → 단순 질문이므로 `reply`만 채팅창에 출력
> - `intent === 'amenity'` → `payload.itemList` 사용해서 `requestAmenity()` 호출 (품목코드는 `AM001~AM005`)
> - `intent === 'housekeeping'` → `payload.hkStatCd` (`MU`/`DND`/`CLR`) 사용해서 `updateHousekeeping()` 호출
> - `intent === 'late_checkout'` → `checkLateCheckout()` → 가능하면 `requestLateCheckout()` 호출
>
> 기존 `src/api/client.js`의 함수들을 그대로 재사용할 것.
>
> **3. 화면 갱신**
> API 호출이 성공하면 채팅창에 *"✅ 요청이 프론트 데스크로 전달되었습니다 (요청번호: XXX)"* 형태로 띄워줘. 실패 시 `resCd`와 `resMsg` 노출.
>
> **4. 다국어**
> 현재 예약의 `perUseLang` 값(ko_KR/en_US/ja_JP/zh_CN)에 따라 LLM system prompt에 언어 지정. 어떤 언어로 입력해도 응답은 예약 언어로.

### 부가 Todo
- [ ] `vue_client/public/manifest.json` 추가 + PWA 아이콘 → 태블릿 홈화면 설치
- [ ] `vite-plugin-pwa` 붙여서 오프라인 캐시
- [ ] 프론트 대시보드 뷰 (`DashboardView.vue`): 3~5초 폴링으로 `amenity/list` 호출해서 신규 요청 실시간 표시
- [ ] LLM API 키는 `.env.local`에 `VITE_LLM_API_KEY`로 넣고 gitignore 확인
- [ ] Capacitor로 Android APK 뽑아보기 (선택)

---

## 🎨 PMS 스타일 일치 포인트

이 프로젝트는 실제 회사 소스가 아닌 **스타일만 모방**한 Mock이지만,
집에서 이어 작업할 때 일관성을 위해 아래 규칙을 따름:

- Controller: `@Controller + @ResponseBody + @RequestMapping("/api/...")`, `BaseController` 상속 대신 단독 구성
- Response: `Responses.MapResponse.of(map)` / `Responses.ListResponse.of(list)` / `Responses.ok()` / `Responses.fail(cd, msg)`
- 예외 처리: `BizException(resCd, resMsg)` → `GlobalExceptionHandler`에서 변환
- 네이밍: camelCase 약어 (`rsvNo, roomNo, chkOutTm, hkStatCd, procStatCd, rateTpCd, addAmt, curCd`)
- 서비스 레이어: 한국어 주석만 사용 (변수/메서드명은 영문)
- 헬퍼 메소드 최소화 (인라인 우선)
- DDL 파일(`schema.sql` 등) 금지 → 인메모리 시드로 대체
- 들여쓰기: 탭

---

## 💡 프로젝트 배경 & 결정사항 (대화 히스토리)

맥북/집에서 이어갈 때 컨텍스트 안 잃어버리려고 그동안의 논의 정리.

### 🎯 이 프로젝트의 정체성
**"버튼 탭 기반 컨시어지 태블릿 → 자연어 대화형으로 바꾼 버전"**

기존 호텔 객실 태블릿은 이런 식:
> 메뉴 → 어메니티 → 수건 → +/- 2개 → 확인 버튼

이 프로젝트가 만드는 건:
> 챗봇에 *"수건 2개랑 생수 좀 주세요"* 라고 말하면 → AI가 의도 파악 → 적절한 API 자동 호출 → 프론트 데스크에 전달

**홈화면의 3개 카드(어메니티/정비/레이트체크아웃)는 사실 Fallback UI**고,
진짜 주인공은 **우측 하단 플로팅 챗봇**. 심사 시연도 챗봇으로 하는 게 임팩트 최대.

### 🏆 차별화 포인트 (임원/심사위원 어필용)
1. **언어 장벽 제거** — 중국/일본/동남아 관광객 급증 대응, 메뉴 번역 유지보수 불필요
2. **메뉴에 없는 롱테일 요청 커버** — *"베개 하나 더"*, *"방이 너무 추워요"* 도 AI가 하우스키핑 티켓으로 라우팅
3. **압도적 UX** — 노인/비IT층도 말만 하면 됨. 레이트 체크아웃(매출 직결) 전환율 상승 기대
4. **대화 로그 데이터** — FAQ/서비스 개선 인사이트 자산화

### ⚙️ 기술 스택 결정 과정
- **초기**: Node(Express) + Flutter Web → 버림
- **이유**: 회사 신규 프로젝트가 Vue3 + Java(Spring Boot)라 "수상 후 사내 이식" 고려 시 스택 일치가 유리
- **최종**: Vue3(Vite) + Spring Boot 3.2 / Java 17 + 인메모리 Mock
- **앱 배포**: 네이티브 아님. **PWA로 태블릿/폰 "홈화면에 추가"** 가 가장 빠름. 필요 시 Capacitor로 랩핑해서 APK/IPA 뽑기 가능

### 🤔 실제 PMS 이식 관련 판단
대표님이 **개발 IT팀 본인**이라 기술적으로는 전혀 빡세지 않음:
- 하우스키핑 상태: `RoomStatusChangeController` 계열 재사용
- 레이트 체크아웃: `ChkOutDelayMgmtController` 거의 그대로
- 어메니티: 신규 테이블 1~2개만 추가
- 모듈당 2~3일, MVP 2~3주 스프린트 가능

**그러나 프로토타입 단계에선 이식 불필요** — 확정:
- 시연할 때 심사위원은 뒷단이 PMS든 Mock이든 모름
- Mock 장점: 네트워크 끊겨도 동작 / 보안 리스크 0 / 실데이터 오염 0 / 맥북 단독 완결
- **실제 이식은 수상 → 사내 파일럿 승인 후** 로드맵

### 🚧 실제 이식 시 걸림돌 (미래 참고)
*코드가 아니라 주변 인프라가 본체*
1. **게스트 인증** — 직원 세션(`SessionUser.propCd/cmpxCd`) 전제인 PMS에 예약번호 기반 단기 토큰 레이어 필요 (`sy` 모듈에 `GuestTokenController` 신규, 예약번호+체크인일자+생년월일 → JWT, 체크아웃까지 만료)
2. **네트워크** — 객실 태블릿/게스트 폰에서 사내 PMS 접근 → API Gateway 또는 DMZ 프록시
3. **프론트 데스크 알림** — `CustMessageMgmtController` 또는 기존 알림 큐 연동
4. **멀티 프로퍼티** — `propCd`/`cmpxCd` 필터링 누락 시 체인 단위 오작동
5. **보안팀 리뷰** — 외부망 → 내부 PMS 직접 호출 구조라 필수
6. **파일럿 호텔 섭외** — 태블릿 실제로 깔 곳
7. **게스트 대면 UX/디자인** — 대충 만들면 컴플레인 직격

### 🎨 PMS 스타일 모방 근거
`src/main/java/com/daol/pms/ht/rs/RegCardMgmtController.java` 를 레퍼런스로 잡음:
- `@Controller + @ResponseBody + @RequestMapping("/api/ht/rs/...")`
- `extends BaseController`, `Responses.ListResponse.of()` / `MapResponse.of()`
- `RequestParams requestParams` + `getParams()` 패턴
- 한국어 javadoc, 탭 들여쓰기

이 프로젝트는 infra(`BaseController`, `CommonDAO`, `SessionUser`)까지 복제하면 과해서 **경량 미니 버전**만 구현. 수상 후 실제 이식할 땐 회사 infra에 얹으면 됨.

### 📱 앱 배포 3가지 옵션 (논의 결과)
| 옵션 | 난이도 | 추천 상황 |
|---|---|---|
| **PWA** | 최저 (10분) | 대회/데모/사내 파일럿 — 이걸로 충분 |
| **Capacitor** | 중 (반나절) | 카메라/푸시 등 네이티브 기능 필요 시 |
| **Tauri/Electron** | 중상 | 데스크탑 앱까지 필요 시 |

### 🙅 지켜야 할 코딩 규칙 (사내 관례)
- 서비스 레이어에 한국어 변수/메서드명 금지 (주석만 OK)
- 컨트롤러는 얇게 (로직은 서비스로)
- 헬퍼/유틸 private 메서드 만들지 말고 인라인 처리
- DDL 파일(`schema.sql`, `create_table.sql` 등) 프로젝트에 포함 금지
- properties 파일 수정 시 반드시 `mode: append`, overwrite 금지
- CRLF 줄바꿈 유지 (SVN 프로젝트 기준, 이 레포는 Git이라 경고만 뜸)

---

## 🛠️ 상용화 로드맵 (2026-04-14 결정)

> **전환**: 대회/데모용 Mock → **실제 상용 제품 소스로 육성**.
> Vue + PWA 스택 유지 (게스트가 "앱 설치 없이 QR 찍고 바로 사용"이 핵심 UX라 네이티브 전환은 역효과).
> 미래에 BLE/NFC 등 네이티브 기능 필요해지면 **Capacitor 랩핑**으로 해결.

### 작업 순서

| # | 항목 | 예상 | 상태 |
|---|---|---|---|
| 1 | **LLM 호출을 Spring Boot 프록시로 이동** (키 숨기기) | 40분 | ✅ 완료 |
| 2 | **API baseURL/CORS 정리 + 폰 동작 확인** | 15분 | ✅ 완료 |
| 3 | **DB 도입** (JPA + H2 MYSQL 호환 → MariaDB 운영 경로) | 반나절 | ✅ 완료 |
| 4 | **게스트 토큰 인증 레이어** (예약번호+체크인일자+생년월일 → JWT) | 1일 | ✅ 완료 |
| 5 | **`propCd` 멀티프로퍼티 스키마** (체인 격리) | 반나절 | ✅ 완료 |
| 6 | **Docker + CI/CD + 환경 분리** (dev/stage/prod 프로파일) | 1일 | ✅ 완료 |
| 7 | **Capacitor 랩핑** (앱스토어 존재감 필요 시 선택) | 반나절 | ✅ 완료 |

검증: `bash api_server/smoke-test.sh` — 27개 체크 (unauth 401 → 토큰 발급 → 자기 예약 조회 → 타인 예약 9102 → 멀티 프로퍼티 격리 → AI 프록시 한/영 → 401 가드) 전부 PASS.

### 핵심 아키텍처 결정사항

- **플랫폼**: **Vue 3 + PWA**로 상용화 진행. Flutter/네이티브 전환 검토했으나 폐기.
  - 이유 1: 게스트 1~2박 체류 특성상 **앱 설치 저항 = 전환율 사망**. QR → 웹앱이 유일한 정답.
  - 이유 2: 회사 스택(Vue + Spring Boot) 일관성 유지 → 사내 이식 스토리 방어.
  - 이유 3: 객실 태블릿 / 게스트 폰 / 호텔 프론트 PC를 **단일 코드베이스**로 커버 가능.
  - 이유 4: iOS 16.4+에서 PWA 푸시 지원 완료. 홈화면 설치 UX도 네이티브와 동등 수준.
  - 이전에 Flutter/Node로 갔다가 돌아온 전례(`bbc1dae`) 재확인 — 같은 실수 반복 금지.

- **LLM 호출 구조**: ✅ `POST /api/ai/chat` 서버 프록시 경유 (US-001). 키는 `ANTHROPIC_API_KEY`
  환경변수로만 로드되고 프론트 번들에는 없음.

- **데이터 영속성**: ✅ Spring Data JPA (US-003). dev 는 H2 파일 모드(MySQL/MariaDB 호환),
  prod 는 **MariaDB** — 회사 PMS 가 MariaDB 를 쓰므로 스택 일치.

- **멀티 테넌시**: ✅ 모든 엔티티에 `propCd` 컬럼, 모든 쿼리가 JWT principal 의 `propCd` 로 격리
  (US-005). 향후 PMS 이식 시 `cmpxCd` 만 추가하면 됨 — 실제 `PMS_RESERVATION` 는 `(PROP_CD, CMPX_CD)` 2-레벨.

### 상용화 전 미결 이슈

- ✅ ~~LLM 시스템 프롬프트 튜닝~~ — US-001 에서 "방이 너무 추운데요" → `housekeeping` 라우팅 확인됨
- ⏳ 대회 시연 환경(ngrok or Vercel) vs 실제 운영 환경(사내 인프라) 배포 분리
- ⏳ 프론트 데스크 알림 채널 (현재는 등록만 되고 알림 없음)
- ⏳ i18n 메시지 사전 DB화 (현재 하드코딩)
- ⏳ **PMS dev 서버 Level 1 미러링** — 다음 세션에서 이어서 (아래 "2026-04-14 진행 로그" 참고)

---

## 🗓️ 진행 로그

### 2026-04-15 (윈도우 세션 — 컨시어지 독립 제품화 + 배포 시도)

오늘의 핵심은 **"컨시어지를 DAOL PMS 종속 제품이 아니라, 타 PMS 로 이식 가능한 독립 제품으로 재설계"** 다.

**1. PMS 키오스크 이벤트 브릿지 (실제 연동 길 확보)**
- PMS(`C:\DAOL\PMS`) 를 read-only 로 탐색 → 키오스크 요청이 프론트데스크 팝업으로 뜨는 경로 전체 파악:
  - 인바운드: `POST /api/mkiosk/event` → `CcService.setKokEvent()` → `KOK_EVENT` 테이블 insert
  - 푸시: Redis publish → STOMP `/topic/{propCd}/{cmpxCd}` → 프론트데스크 `frame.js:WS_MSG()` 에서 `axToast.push()` / `window.open('/jsp/cc/kok-chat.jsp')`
  - 즉, 우리는 **`/api/mkiosk/event` POST 한 방**만 날리면 PMS 기존 팝업 파이프라인이 그대로 동작함
- 처음엔 `/kiosk/login` 토큰 경로로 가려 했으나, `setKokEvent()` 가 `SessionUtils.getCurrentUser()` 에 강제 의존해서 세션 없으면 NPE → 우회 불가
- **PMS 소스 3줄 수정** (사내 PMS 리포, 미커밋):
  1. `SecurityConfig.java` `ignorePagesPost` 에 `/api/mkiosk/event` 추가
  2. `CcService.setKokEvent()` 에 `SessionUser` null 체크 + `userId` param fallback (`"CONCIERGE"`)
  - 기존 호출자 3곳(KokMngtController, MkioskKokMngtController, MkioskApiController)은 항상 세션 컨텍스트라 영향 0 — 오히려 NPE 방어가 추가됨

**2. 독립 제품 아키텍처 + 디스패처 추상화 (가장 큰 구조 변경)**

> *"컨시어지를 단독으로 팔면 어드민 설정/DB 를 PMS 안에 둘 수 없다. 확장성 생각하면 자체 DB + 자체 어드민 UI 가 주인이고, PMS 는 여러 백엔드 중 하나여야 한다."* — 오늘 세션의 결론

- **새 패키지 `com.daol.concierge.dispatcher`** — `RequestDispatcher` 인터페이스 + 구현체 3종
  - `DaolKokEventDispatcher` — 사내 PMS `/api/mkiosk/event` 브릿지 (기존 `PmsKokEventClient` 로직 이관)
  - `ExternalApiDispatcher` — 타 PMS REST stub (future)
  - `InternalOnlyDispatcher` — no-op, **기본값** (`matchIfMissing=true`) → dev/test/standalone 에서 환경변수 없이 그대로 돌아감
  - `@ConditionalOnProperty(concierge.dispatcher)` 로 기동 시 하나만 활성화
- `PmsKokEventClient.java` **삭제** (`DaolKokEventDispatcher` 로 흡수)
- `GrService` 의 3개 훅(어메니티/하우스키핑/레이트체크아웃)이 `RequestDispatcher.dispatch(RequestEvent)` 로 통일 호출

**3. 기능 플래그 시스템 (`com.daol.concierge.feature`)**

- 새 엔티티 2종:
  - `ConciergeFeature` (`CONCIERGE_FEATURE` 테이블, PK=`(propCd, featureCd)`, 컬럼: `useYn`, `sortOrd`, `configJson`, `updUser`, `updDt`)
  - `ConciergeProperty` (`CONCIERGE_PROPERTY` 테이블, propCd PK + 이름/타임존/기본언어)
- 기능 코드 6종: `AMENITY` / `HK` / `LATE_CO` / `CHAT` / `NEARBY` / `PARKING`
- **게스트 API** `GET /api/concierge/features` — JWT 에서 propCd 뽑아 `useYn='Y'` 인 기능만 반환
- **어드민 API** `GET/PUT /api/concierge/admin/features?propCd=` — `X-Admin-Token` 헤더 검증 (`CONCIERGE_ADMIN_PW` 환경변수, 비어있으면 503)
- `AdminAuthInterceptor` + `AdminWebMvcConfig` 로 어드민 경로만 게이트
- `SeedDataRunner` 가 기동 시 `propCd=HQ` 에 6개 feature 시드 (idempotent, `existsById` 체크)
- `V2__concierge_feature.sql` — Flyway 대비 DDL 플레이스홀더 (프로덕션 MariaDB `ddl-auto=validate` 용)

**4. 프론트: 동적 LNB + 어드민 화면**

- `vue_client/src/features/featureStore.js` — 앱 마운트 시 `/api/concierge/features` 1회 호출, 반응형 `features` Map + `enabledSortedFeatures()` 컴퓨티드
- `App.vue` LNB: 하드코딩 4탭 → **`v-for` 동적 렌더** (`FEATURE_META` 에 아이콘/라우트 매핑)
- `router/index.js` `beforeEach` 가드 — 비활성 기능 라우트 직접 접근 시 첫 활성 탭으로 리다이렉트
- 신규 뷰 3개: `AdminFeaturesView.vue` (어드민 토글 테이블, `sessionStorage` 에 `X-Admin-Token` 저장), `NearbyView.vue` / `ParkingView.vue` (placeholder "준비 중")

**5. application.yml 새 블록**
```yaml
concierge:
  dispatcher: ${CONCIERGE_DISPATCHER:internal}   # internal | daol | external
pms:
  base-url / prop-cd / cmpx-cd / kok-cd   # dispatcher=daol 일 때만 사용
admin:
  password: ${CONCIERGE_ADMIN_PW:}        # 비어있으면 /api/concierge/admin/** = 503
```

**6. 빌드 검증**
- `mvn -o compile` BUILD SUCCESS (45 sources)
- `vue_client` `npm run build` OK (97 modules, 152 KB)
- 기존 dev 프로파일(H2 파일) 로 변경 없이 그대로 기동 가능 — dispatcher 기본 `internal` 덕분

**7. 배포 시도 — OCI Ampere A1 Out of capacity (미해결)**
- Oracle Cloud Always Free (춘천 리전) 가입 → `VM.Standard.A1.Flex` 2 OCPU / 12 GB 생성 시도
- **Capacity 고갈** — 한국 주간 시간대엔 거의 항상 품절
- 임시 대체로 `VM.Standard.E2.1.Micro` (1C/1GB) 생성 → `dnf update` 가 OOM 으로 뻗음 → 인스턴스 응답 없음 → terminate
- Cloud Shell 에서 A1.Flex 재시도 루프 스크립트를 짜려 했으나, 복붙 과정에서 `\` 연속행/heredoc 이 모두 깨지는 경험
- **결론**: 배포는 다음 세션으로 미루고, Ampere 재고 풀릴 때(새벽/저녁) 재시도하거나, 사내/집 상시구동 PC + Cloudflare Tunnel 로 우회 검토

**남은 할 일 (다음 세션)**
- [ ] Docker 이미지화 + `docker-compose.yml` (api + mariadb) — 인스턴스 잡히면 1분 안에 배포
- [ ] Cloudflare Tunnel 또는 duckdns 무료 도메인 연결
- [ ] `/admin/features` 화면 폴리싱 (현재는 프롬프트로 토큰 입력)
- [ ] NEARBY / PARKING placeholder → 실제 기능 구현
- [ ] 어드민 인증 PW → 복수 계정 + 감사로그
- [ ] `featureStore` 가 `JJU` 프로퍼티 게스트용 feature 시드 확장

**파일 변경 요약**

신규:
- `api_server/src/main/java/com/daol/concierge/feature/` — 엔티티 2 / 레포 2 / FeatureService / FeatureController / FeatureAdminController / AdminAuthInterceptor / AdminWebMvcConfig / ConciergeFeatureId
- `api_server/src/main/java/com/daol/concierge/dispatcher/` — RequestDispatcher 인터페이스 / RequestEvent / DaolKokEventDispatcher / ExternalApiDispatcher / InternalOnlyDispatcher
- `api_server/src/main/resources/db/migration/V2__concierge_feature.sql`
- `vue_client/src/features/featureStore.js`
- `vue_client/src/views/AdminFeaturesView.vue` / `NearbyView.vue` / `ParkingView.vue` / `ChatView.vue`

수정:
- `gr/service/GrService.java` — `RequestDispatcher` 주입, 3개 훅 교체
- `gr/service/SeedDataRunner.java` — 기능 플래그 6건 시드 추가
- `core/config/SecurityConfig.java` — `/api/concierge/admin/**` 허용
- `application.yml` — concierge / pms / admin 블록
- `App.vue` — LNB 동적 렌더
- `router/index.js` — feature 가드 + placeholder 라우트

삭제:
- `api_server/.../pms/PmsKokEventClient.java` (→ DaolKokEventDispatcher 로 흡수)
- `vue_client/src/components/ChatFab.vue`
- `vue_client/src/views/DashboardView.vue` / `HomeView.vue`

---

**(오후 이어서) NEARBY 기능 + 어드민 UI 폴리싱**

**NEARBY (주변 안내)** — Provider 추상화 + 카테고리 탭 UI
- 호텔 좌표 기준 반경 1km 안의 음식점/카페/편의점/관광지/약국 5개 카테고리
- `com.daol.concierge.nearby` 패키지:
  - `NearbyPlace` (Java record DTO), `NearbyProvider` 인터페이스
  - `MockNearbyProvider` (`@ConditionalOnProperty matchIfMissing=true`) — 서울시청 인근 실제 상호 기반 하드코딩(무교동 북어국집, 프릳츠커피, GS25 서울시청점, 덕수궁, 온누리약국 시청점 등)
  - `KakaoNearbyProvider` (`havingValue="kakao"`) — `dapi.kakao.com/v2/local/search/category.json` 호출, 5s 타임아웃, 키 미설정/실패 시 empty list (폴백)
  - `NearbyController` — `GET /api/concierge/nearby?category=food|cafe|conv|tour|pharmacy`
- `ConciergeProperty` 에 `lat`/`lng` 컬럼 추가, `SeedDataRunner` 가 HQ 를 `37.5665, 126.9780` (서울시청) 으로 시드 + NEARBY 플래그 Y 로 전환
- `application.yml` 새 블록: `nearby.provider` / `nearby.default-radius-m` / `kakao.rest-api-key`
- 프론트 `NearbyView.vue` — 5개 카테고리 탭 + 카드 리스트(도보 N분 / tel 링크 / 카카오맵 열기) + 카테고리별 캐싱으로 탭 전환 즉시
- `api/client.js` — `conciergeClient` axios 인스턴스 + `fetchNearby(category)` 추가
- `V3__property_latlng.sql` — 프로덕션 MariaDB 마이그레이션 플레이스홀더
- **카카오 REST API 키 전환 방법**: `KAKAO_REST_API_KEY=...` + `NEARBY_PROVIDER=kakao` 환경변수 두 줄로 mock → 실제 데이터

**어드민 UI 폴리싱** — prompt() 제거 + 로그인 화면 + iOS 스위치
- **신규 `AdminLoginView.vue`** — 가운데 카드, 패스워드 autofocus, Enter 제출, 401/503/기타 에러 메시지 분기. 성공하면 sessionStorage 에 `concierge.adminToken` 저장 후 `/admin/features` 이동
- **`router/index.js`** — `/admin/login` 라우트 추가, `beforeEach` 가드에서 admin 라우트 접근 시 토큰 체크 → 없으면 자동 리다이렉트 (게스트 기능 플래그 가드와 분리)
- **`AdminFeaturesView.vue` 전면 재작성**:
  - `window.prompt` 완전 제거. 토큰은 sessionStorage 에서만 읽음. 없으면 즉시 로그인 리다이렉트
  - 테이블 → **카드 레이아웃** 전환 (아이콘, 한/영 이름, 사용 토글, 순서 입력, `▼ 고급 설정` 아코디언)
  - **iOS 스타일 토글 스위치** — 순수 CSS, 44×24px 트랙, 20×20 thumb, `#cbd5e0 → #1a3a6e` 전환, `cubic-bezier(0.4, 0, 0.2, 1)` 0.25s
  - **`configJson` 편집기** — 모노스페이스 textarea, blur 시 JSON 검증, 오류 있으면 저장 차단
  - 저장 성공 시 **fade-out 토스트** "저장 완료" (2초 후 자동 소멸, 외부 라이브러리 없음)
  - 로그아웃 버튼 / 401 응답 시 자동 로그인 리다이렉트
  - `sortOrd` 로 정렬 표시 (편집 중엔 재정렬 안 해서 튀지 않음)

**배포 시도 (계속)**
- Cloud Shell 에서 `oracle_macro.sh` 루프를 `nohup` 으로 백그라운드 실행 (180초 간격)
- Ampere A1.Flex 용량은 한국 낮 시간대 여전히 고갈. 새벽 시간대 재시도 예정
- 코드는 원격에 푸시되어 있어 인스턴스 잡히는 시점에 바로 배포 가능

**빌드 검증 (오후)**
- `mvn -o compile` BUILD SUCCESS
- `npm run build` OK (99 modules, 1.28s)

### 2026-04-14 (윈도우 세션)
- ✅ 윈도우 PC에 **포터블 JDK 17 + Maven 3.9.14** 격리 설치 (`C:\tools\...`, 시스템 PATH 무영향)
  — 회사 PMS 개발 환경과 완전 분리
- ✅ `api_server/run.bat` 추가: 그 셸에서만 `JAVA_HOME`/`PATH` override, `env.local.bat` (gitignore) 자동 로드
- ✅ Spring Boot 백엔드 라이브 검증 완료 (`localhost:8080`, 1.3초 기동)
- ✅ Vue 프론트 `localhost:5173` 기동, 챗봇 end-to-end 동작 확인
- ✅ **Claude Haiku 4.5 LLM 모드 활성화**: Anthropic 크레딧 충전 + 키 세팅
  — *"어떤 기능이 있니?"*, 일본어 `タオルを2枚ください`, 레이트 체크아웃 2단계 플로우 전부 동작 확인
- ✅ **상용화 로드맵 7단계 전부 구현**:
    1. Spring Boot `/api/ai/chat` 프록시로 LLM 키 서버 측 은닉
    2. 런타임 baseURL + LAN/ngrok CORS 패턴 → 폰 브라우저 대응 완료
    3. JPA + H2(파일 모드, MYSQL 호환) + MariaDB prod 프로파일(회사 PMS 스택 일치), `SeedDataRunner` 자동 시드
    4. `POST /api/auth/guest-token` + jjwt HS256, `JwtAuthFilter`, `SecurityContextUtil`
    5. `GrService` 전 조회/삽입이 JWT 의 `propCd` 로 격리 (JJU 제주 프로퍼티로 교차 접근 차단 검증)
    6. 멀티스테이지 Dockerfile(api/web) + `docker-compose.yml` + GitHub Actions CI (api build → vue build → 이미지 빌드 캐시)
    7. Capacitor 8 설치, `android/` 플랫폼 scaffold, `npm run cap:android` 원샷 스크립트
- ✅ `smoke-test.sh` 27개 체크 통과 (비인증 401 가드 + 멀티 프로퍼티 격리 포함)
- ✅ **Architect 리뷰 → 하드닝** (`9b63e7e`): prod 기동 시 JWT_SECRET 미설정이면 fail-fast
  (`JwtSecretGuard`), `/h2-console` 은 dev 프로파일 한정, `/api/auth/**` → `/api/auth/guest-token` 만 공개,
  `anyRequest().denyAll()` 안전 디폴트, `AiChatService` 방어적 auth 재확인, postgres 5432 포트 노출 제거
- ✅ **PostgreSQL → MariaDB 교체** (`f85e25d`): 회사 PMS 스택 일치
  - pom.xml 드라이버 교체, H2 호환 모드 MYSQL 로 전환, `docker-compose.yml` 을 `mariadb:11` 로
  - dev 로컬은 여전히 Docker 불필요 (H2 파일 모드)

---

### 📋 다음 세션 이어받기: **PMS dev 서버 Level 1 미러링**

오늘 세션 말미에 PMS 레포(`C:\DAOL\PMS`)를 read-only 로 탐색해서 실제 dev 서버 접속 정보와
`PMS_RESERVATION` 스키마를 확인함. 아래는 다음 세션에서 바로 이어받기 위한 요약.

**Level 1 정의**: 기동 시 PMS dev MariaDB 에 read-only 로 붙어 실제 예약 2~3건을 우리 mock 의
`gr_reservation` 로 미러링 → 데모에서 *"이 예약은 PMS dev 에 실제로 존재합니다"* 라고 보여주는 단계.
쓰기는 여전히 mock H2 / MariaDB 로만. 리스크 최소.

**PMS dev 서버 접속 정보** (`C:\DAOL\PMS\.idea\workspace.xml` + `datasource-dev.properties` 에서 추출)

| 항목 | 값 |
|---|---|
| Host | `211.34.228.191` |
| Port | `3336` |
| DB | `PMS` |
| 계정 | `DBE` / `PMS` / `PMS_API` (전부 Jasypt 암호화) |
| Jasypt key | `DAOLVISION` (`PBEWithMD5AndDES`) — `JasyptConfig.java:15` 하드코딩 |

**실제 스키마 요약** (`PMS_RESERVATION`, 113+ 컬럼) — 우리가 쓸 7개

| mock 필드 | 실제 컬럼 |
|---|---|
| `rsvNo` | `RESV_NO` |
| `propCd` | `PROP_CD` (**PK 는 `PROP_CD` + `CMPX_CD` 복합키**) |
| (신규 추가 필요) | `CMPX_CD` — 우리 entity 에 `cmpxCd` 필드 추가해야 함 |
| `roomNo` | `RM_NO` |
| `perNm` | `PER_NM` (평문) |
| `chkInDt` | `ARR_DT` |
| `chkOutDt` | `DEP_DT` |
| `chkOutTm` | `DEP_HOUR` |
| `roomTpCd` | `RM_TP_CD` |
| `perUseLang` | **❓ PMS_RESERVATION 에 없음** — `PMS_CUST_MGMT` 조인 필요 or 기본값 `ko_KR` |

필터 조건: `PROP_CD=? AND CMPX_CD=? AND USE_YN='Y' AND RESV_STAT IN ('R','A','I') LIMIT 5`

**남은 블로커 3개**
1. **계정 확보** — 권장: DBA 에게 `concierge_ro` 같은 read-only 계정 요청
   (대안: 기존 PMS 계정의 ENC 비번을 jasypt 키로 복호화 → 비추, 감사 로그 섞임)
2. **네트워크 도달** — `Test-NetConnection -ComputerName 211.34.228.191 -Port 3336` 로 확인
   (사내망/VPN 필요할 가능성 큼)
3. **대상 프로퍼티 선정** — 데모에 쓸 `PROP_CD` / `CMPX_CD` 한 조합

**다음 세션 프롬프트 템플릿**

> "guest-request-mock 이어서. README 의 'PMS dev 서버 Level 1 미러링' 섹션 읽고,
> PMS dev 에 3336 포트 도달되는지 확인했으니 (도달 O / X) 이제 `PmsMirrorService` 짜서
> 기동 시 5건 미러링하는 코드 추가해줘. 계정은 `<user>` / `<pw>` / PROP_CD=`<prop>` CMPX_CD=`<cmpx>`."

### 2026-04-13 (맥북 세션)
- ✅ `feat/ai-chat-concierge` 브랜치에 AI 챗봇 + 대시보드 + PWA 풀구현 커밋
- ✅ Vue 클라이언트: `npm install` + `vite build` 통과, dev 서버 정상 구동 확인
- ✅ 룰 기반 의도 파서 13/13 단위테스트 통과 (한/영/일/중)
- ⚠️ Spring 백엔드 라이브 검증 보류 — 맥북에 **JDK/Maven 미설치** 상태

---

## 📝 커밋 히스토리 메모
- `92899e7` Init: Node(Express) + Flutter 초기 구성
- `bbc1dae` refactor: Flutter/Node 제거, Vue3 + Spring Boot(PMS 스타일)로 재구성
- `8a4a65f` docs: README 추가 (구조/실행/맥북 이어서 할 작업 Todo)
- `df77b9e` docs: README에 프로젝트 배경/결정사항/대화 히스토리 추가
- `61d8389` feat: AI 다국어 채팅 컨시어지 + 대시보드 + PWA
