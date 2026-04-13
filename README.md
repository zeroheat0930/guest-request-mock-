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

## 📝 커밋 히스토리 메모
- `92899e7` Init: Node(Express) + Flutter 초기 구성
- `bbc1dae` refactor: Flutter/Node 제거, Vue3 + Spring Boot(PMS 스타일)로 재구성
