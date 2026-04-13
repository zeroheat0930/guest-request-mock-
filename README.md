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

## 🗓️ 진행 로그

### 2026-04-13 (맥북 세션)
- ✅ `feat/ai-chat-concierge` 브랜치에 AI 챗봇 + 대시보드 + PWA 풀구현 커밋
- ✅ Vue 클라이언트: `npm install` + `vite build` 통과, dev 서버 정상 구동 확인
- ✅ 룰 기반 의도 파서 13/13 단위테스트 통과 (한/영/일/중)
- ⚠️ Spring 백엔드 라이브 검증 보류 — 맥북에 **JDK/Maven 미설치** 상태
- 📌 다음 세션: `brew install maven` → `mvn spring-boot:run`으로 백엔드 띄우고
  http://localhost:5173 에서 우측 하단 💬 챗봇 실제 호출 흐름 확인

---

## 📝 커밋 히스토리 메모
- `92899e7` Init: Node(Express) + Flutter 초기 구성
- `bbc1dae` refactor: Flutter/Node 제거, Vue3 + Spring Boot(PMS 스타일)로 재구성
- `8a4a65f` docs: README 추가 (구조/실행/맥북 이어서 할 작업 Todo)
- `df77b9e` docs: README에 프로젝트 배경/결정사항/대화 히스토리 추가
- `61d8389` feat: AI 다국어 채팅 컨시어지 + 대시보드 + PWA
