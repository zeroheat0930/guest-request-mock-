# 다올 컨시어지 — 로컬 LAN 데모 셋업

심사(2026-05-20) 시연용 **노트북 + 회사 사무실 LAN** 운영 매뉴얼. 클라우드 배포 없이 노트북 한 대를 서버로 두고, 같은 무선 AP 의 태블릿/휴대폰에서 게스트 / 스태프 / 러너 화면을 띄운다.

> 배포 결정 (2026-04-27): 외부 네트워크에서 회사 DB(`211.34.228.191:3336`) 직접 접속 불가, OCI 춘천 VCN 외 인프라 없음. 따라서 **사내망에서 노트북을 서버화** 가 가장 단순하고 신뢰 가능. 클라우드 호스팅은 심사 후 트랙.

---

## 1. 사전 준비물

| 항목 | 비고 |
|---|---|
| 노트북 (Windows) | 백엔드+프론트+관리자 콘솔 동시 구동 |
| 태블릿 | 게스트 앱 (객실 가정) — iPad 또는 안드로이드 태블릿 |
| 휴대폰 | 러너 PWA — 안드로이드/iOS 무관 |
| 사내 WiFi 또는 모바일 핫스팟 | **3 기기 모두 같은 서브넷** 이어야 함 |
| 시연용 QR 인쇄물 | 객실/로비 시뮬레이션용 (이 문서 §5.4 참조) |
| 사내망 접근 | 회사 DB(`211.34.228.191:3336`) 가 보이는 환경 — VPN 또는 사무실 |

> **핫스팟 주의**: 일부 통신사 핫스팟은 클라이언트 격리(AP isolation) 가 켜져 있어 노트북 ↔ 태블릿 직통 통신이 차단된다. 가능하면 사내 무선 AP 사용. 부득이 핫스팟 쓸 경우 시연 전에 반드시 §3 의 ping 테스트로 검증.

---

## 2. 노트북 IP 고정 + 확인

### 2.1 LAN IP 확인 (Windows)

```
ipconfig
```

`Wireless LAN adapter Wi-Fi:` 섹션의 `IPv4 주소` 값을 메모. 이하 문서에서 `<LAN_IP>` 로 표기. (예: `192.168.0.42`)

### 2.2 IP 고정 옵션

| 방식 | 권장도 | 비고 |
|---|---|---|
| 사내 DHCP 예약 | ★★★ | IT팀에 MAC 주소 알려서 동일 IP 고정 — 가장 안전 |
| Windows 수동 IP 지정 | ★★ | 어댑터 속성 → IPv4 → 정적 IP. 게이트웨이/DNS 같이 박아야 함 |
| 매번 ipconfig 로 확인 | ★ | 시연 직전 IP 가 바뀌면 §5.3 의 frontend env 까지 다시 박아야 함 |

### 2.3 Windows 방화벽 인바운드 허용

```powershell
# 관리자 PowerShell
New-NetFirewallRule -DisplayName "Concierge API" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "Concierge Vite" -Direction Inbound -LocalPort 5173 -Protocol TCP -Action Allow
```

(또는 윈도우 방화벽 GUI 에서 8080, 5173 인바운드 TCP 허용)

---

## 3. 네트워크 연결 검증 (T-30분)

3 기기 모두 같은 WiFi 에 붙은 후, 휴대폰/태블릿 브라우저에서:

```
http://<LAN_IP>:8080/actuator/health
```

→ `{"status":"UP"}` 응답 확인. 안 보이면:

1. 같은 SSID 인지 재확인
2. 서브넷 일치 확인 (`192.168.0.x` 끼리 / `192.168.1.x` 끼리)
3. 노트북 방화벽 (§2.3)
4. AP isolation (§1 핫스팟 주의)

---

## 4. 서버 기동 순서

### 4.1 환경변수 (`api_server/env.local.bat`)

회사 DB 직결 + AI + 어드민 패스워드. 회사 환경에서 시연하면 PMS_USER_MTR / PMS_PROPERTY 가 진짜 데이터로 붙는다.

```bat
@echo off
set SPRING_PROFILES_ACTIVE=dev
set DB_USER=<사내 DB 계정>
set DB_PASSWORD=<사내 DB 비번>
set ANTHROPIC_API_KEY=<Claude 키>
set CONCIERGE_TENANT_PROP_CD=<운영 PROP_CD>
set CONCIERGE_TENANT_CMPX_CD=<운영 CMPX_CD>
set JWT_SECRET=<32바이트 이상 랜덤>
set CONCIERGE_DISPATCHER_CCS=true
set NEARBY_PROVIDER=mock
```

> `CONCIERGE_TENANT_PROP_CD` / `CONCIERGE_TENANT_CMPX_CD` 는 시연 호텔의 실 운영 PMS_PROPERTY 코드로 박아야 한다. `application.yml` 기본값(`0000000001` / `00001`) 은 로컬 시드 전용 — 사내 시연 시에는 반드시 환경변수로 override.

### 4.2 백엔드

```bash
cd api_server
./run.bat
```

기동 완료 로그(`Started ConciergeApplication`) 확인.

### 4.3 프론트

```bash
cd vue_client
npm run dev -- --host
```

Vite 가 두 줄을 출력 — `Local: http://localhost:5173/` 와 `Network: http://<LAN_IP>:5173/`. 네트워크 줄이 §2.1 의 IP 와 일치하는지 확인.

> 프론트는 `VITE_API_BASE` 를 안 박아도 된다. `client.js:resolveApiBase()` 가 `window.location.hostname` 에서 자동 추출 → 게스트가 `http://<LAN_IP>:5173` 으로 접속하면 axios 가 `http://<LAN_IP>:8080/api` 로 자동 호출. CORS 도 dev 모드에서 `192.168.*` / `10.*` / `172.16~31.*` 모두 허용 상태 (`SecurityConfig.corsConfigurationSource`).

---

## 5. 기기별 접속 URL

`<LAN_IP>` 자리에 §2.1 값 박는다.

### 5.1 게스트 태블릿 (객실 시뮬)

```
http://<LAN_IP>:5173/?rsvNo=<유효 예약번호>
```

`rsvNo` 는 **체크인 일자가 오늘인 PMS_RSV** 행이어야 게스트 JWT(72h) 가 유효. §6.1 참조.

### 5.2 스태프 / 관리자 노트북 브라우저

```
http://<LAN_IP>:5173/staff.html
```

→ `/staff/login` 으로 자동 이동. SYS_ADMIN 또는 PROP_ADMIN 계정으로 로그인.

> ⚠️ DEMO_SCRIPT.md 에 남아있는 `admin/admin` 표기는 stale. Phase G 이후 X-Admin-Token 폐기 + 스태프 JWT 단일화. 관리자 진입은 `USER_TP ∈ {00001,00002,00003}` 인 PMS_USER_MTR 계정 로그인 후 호텔 선택(`/staff/context`) → 관리자 메뉴 노출.

### 5.3 러너 휴대폰 (모바일 PWA)

```
http://<LAN_IP>:5173/staff.html#/runner
```

스태프 로그인 후 `/runner` 진입. 홈화면 추가하면 PWA 로 설치됨.

### 5.4 시연용 QR 사전 발급

시연 동선 단축을 위해 **3장의 QR** 을 종이로 미리 인쇄해두면 좋음:

| 카드 | 인코딩 URL | 용도 |
|---|---|---|
| 객실 QR (게스트) | `http://<LAN_IP>:5173/?rsvNo=<오늘 체크인 예약번호>` | 시연 STEP 1 |
| 스태프 QR (노트북 큰 화면) | `http://<LAN_IP>:5173/staff.html` | 보조 |
| 러너 QR (휴대폰) | `http://<LAN_IP>:5173/staff.html#/runner` | 시연 STEP 2~6 푸시 시각화 |

QR 생성은 `/admin/qr` (QrGeneratorView) 에서 가능. PROP/CMPX 선택 후 객실번호 입력하면 호텔별 진입 URL 로 인코딩.

---

## 6. 데모 DB 시드 점검

회사 DB(`211.34.228.191:3336/INV` + `PMS`) 에 직결된 상태에서 시연 직전 다음 4가지 확인.

### 6.1 게스트 예약 (PMS_RSV)

- **체크인 일자가 오늘** 인 예약번호가 최소 1건 있어야 게스트 JWT 발급 가능
- 없으면 운영팀에게 더미 예약 1건 요청하거나 PMS 본업 화면에서 등록
- 빠른 확인: `SELECT RSV_NO, GUEST_NM, ROOM_NO, CHK_IN_DT FROM PMS.PMS_RSV WHERE PROP_CD=:propCd AND CHK_IN_DT = CURDATE() LIMIT 5;` (`:propCd` 자리에 §4.1 에서 박은 운영 코드)

### 6.2 스태프 / 관리자 계정 (PMS_USER_MTR)

- SYS_ADMIN(`USER_TP='00001'`) 1명 — 시연용 (전체 권한)
- PROP_ADMIN(`USER_TP='00002'`) 1명 — 호텔 선택 플로우 시연용
- 부서별 스태프 — HK / FR / ENG / FB 각 1명 이상 (USE_YN='Y')
- 비번이 BCrypt 인코딩 상태인지 확인 (USER_PW 컬럼)

### 6.3 부서 마스터 (PMS_DIVISION)

- 운영 `PROP_CD` 행에 `HK / FR / ENG / FB` (또는 사내 코드체계) 가 `DEPT_NM` 한국어로 채워져 있어야 함
- 없으면 `AdminCcsView` 의 직원 목록에서 부서명이 코드로 찍힘

### 6.4 기능 플래그 (INV.CONCIERGE_FEATURE)

- 운영 `(propCd, cmpxCd)` 에 행이 0건이어도 OK — `AdminFeaturesView` 가 카탈로그 9종을 default `useYn='N'` 으로 노출 (Phase G+ 픽스, a2d8530)
- 시연 전 `AMENITY / HK / LATE_CO / NEARBY / PARKING / CHAT / LOSTFOUND / VOC / RENTAL` 9개 모두 `useYn='Y'` 로 토글 후 저장

### 6.5 물품 카탈로그 (INV.CCS_RENTAL_ITEM)

- 최소 5종(우산/충전기/어댑터/다리미/기타) `STOCK_AVAILABLE > 0` 상태
- 시연 STEP 4 의 재고 감소/복구 시각 효과를 위해 5/5 같은 정수 재고 권장

---

## 7. T-5 분 시연 직전 체크리스트

```
□ 노트북 §2.1 IP 변동 없음 재확인
□ 백엔드 /actuator/health = UP
□ 프론트 Vite Network URL 표시 정상
□ 태블릿/휴대폰에서 §3 ping 테스트 OK
□ 스태프 노트북 브라우저 — 관리자 계정 로그인 + 호텔 선택 완료 + StaffDashboardView 진입
□ 러너 휴대폰 — 부서 스태프(예: HK 부서원) 로그인 + /runner 진입
□ 게스트 태블릿 — QR 미스캔 (시연 STEP 1 에서 라이브 시연)
□ AdminFeaturesView — 9개 기능 모두 ON
□ AdminRentalView 카탈로그 탭 — 우산 재고 5/5 표시
□ AdminVocView — 빈 상태 (URGENT 토스트 시연 전)
□ ANTHROPIC_API_KEY 유효 — `/api/ai/status` 200 + `available:true`
□ 시계 — 첫 90초 시연 시각 결정
□ 폴백 demo.mp4 (landing 페이지) 별도 탭에 미리 띄워둠
```

---

## 8. 트러블슈팅

| 증상 | 원인 후보 | 해결 |
|---|---|---|
| 모바일 브라우저에서 `<LAN_IP>:5173` 안 열림 | 방화벽 / AP isolation / 다른 서브넷 | §2.3 → §3 → §1 핫스팟 주의 |
| 게스트 진입 후 `-30 인증 필요` | rsvNo 가 오늘 체크인 아님 / propCd 불일치 | §6.1 |
| AdminCcsView 직원 목록 0건 | PMS_USER_MTR 가 없음 / `USE_YN='N'` / `myUserTp` 보다 위 등급만 존재 | 6.2 / `AdminRoles.canManageUser` 위계 확인 |
| AI 응답 대신 룰 폴백 | `ANTHROPIC_API_KEY` 누락 또는 401 | env 재투입 후 백엔드 재기동 |
| WebSocket 푸시 안 옴 | `ws://<LAN_IP>:8080/ws-ccs` 차단 / 프록시 | §2.3 방화벽, ngrok 쓰는 경우 ws 도 프록시되는지 확인 |
| `NEARBY` 호출이 mock 좌표 | `NEARBY_PROVIDER=kakao` 미설정 — 시연용은 mock 으로 충분 | 무시 |

---

## 9. 보완 — DEMO_SCRIPT.md 와의 관계

- 이 문서: **시연 환경 셋업** (인프라/네트워크/시드)
- `docs/DEMO_SCRIPT.md`: **시연 7단계 시나리오** (대사/조작 순서)

DEMO_SCRIPT 의 다음 표기는 stale 이므로 시연 시 따르지 말 것:

- "스태프 로그인: `fr001 / fr001`" → 실제 시드는 `fr1 / test1234` (로컬 dev) 또는 PMS_USER_MTR 마스터 (사내)
- "`admin / admin` (관리자)" → 폐기됨. 스태프 JWT + USER_TP 로 단일화
- "객실 03010" → 사내 PMS 의 실제 객실번호로 교체
- "X-Admin-Token 3 계층" 답변 → "스태프 JWT + 메뉴별 ROLE_GRANT 2 계층" 으로 갱신

(DEMO_SCRIPT 의 시나리오 흐름 자체는 유효 — 계정/객실 숫자만 환경에 맞춰 갈아끼우면 됨)
