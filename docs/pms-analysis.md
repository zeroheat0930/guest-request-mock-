# PMS 로그인/유저/부서/권한 구조 분석

**분석 일자**: 2026-04-15  
**대상 시스템**: DAOL PMS (Spring Boot + MyBatis XML + JSP)  
**패키지**: `com.daol.pms`

---

## 1. 로그인 플로우

### 엔드포인트 및 필터 구조

**POST 엔드포인트:**
- `/login` - 웹 기반 로그인 (PostLoginFilter 처리)
- `/api/login` - API 기반 로그인 (LoginFilter 처리)
- `/kiosk/login` - 키오스크 로그인 (LoginFilter 처리, 별도 분석됨)

**파일 위치 및 라인:**
- `SecurityConfig.java` (라인 40-42): 로그인 경로 상수 정의
- `SecurityConfig.java` (라인 103-116): 필터 설정 및 엔드포인트 등록
- `LoginFilter.java` (라인 28-77): API 로그인 필터
- `PostLoginFilter.java` (라인 28-80): 웹 로그인 필터

### 인증 방식: 토큰 기반 (JWT)

PMS는 세션 기반이 아닌 **JWT 토큰 기반 인증**을 사용합니다.

**플로우:**
1. 사용자가 `userId`/`userPw` 제출
2. `LoginFilter` / `PostLoginFilter`의 `attemptAuthentication()` (라인 43-48)에서 `UsernamePasswordAuthenticationToken` 생성
3. `LocalDaoAuthenticationProvider`에서 검증 (라인 8-19)
4. 인증 성공 시 `SessionUser` 객체 생성
5. `TokenAuthenticationService.addAuthentication()` (라인 52-56)에서 JWT 토큰 생성
6. **쿠키에 저장** (웹) 또는 **DB 저장** (API) - `TokenAuthenticationService.setUserEnvironments()` 라인 58-68

**토큰 만료:**
- 실환경: 3시간 (`tokenExpiry()` 라인 45-46)
- 개발환경: 24시간

### SessionUser 클래스

`com.daol.pms.core.security.SessionUser` (라인 17-160)

**핵심 필드:**
```java
private String userId;           // 로그인 ID
private String userPw;           // 비밀번호 (BCrypt 해시)
private String tempUserPw;       // 임시 비밀번호
private String userNm;           // 사용자명
private String userTp;           // 사용자 유형 (00001=어드민, 00002=마니저, 기타)
private String propCd;           // 자산 코드
private String cmpxCd;           // 컴플렉스 코드 (00000=전체)
private String deptCd;           // 부서 코드
private String locale;           // 지역 설정
private List<String> authorityList;  // 권한 목록 (ROLE_xxx 형식)
private String loginType;        // 로그인 타입 (/login 또는 /api/login)
```

---

## 2. 사용자/스태프 데이터 모델

### 메인 테이블: PMS_USER_MTR

**쿼리 파일:** `CO_SQL.xml` (라인 10-68)  
**쿼리 ID:** `getLoginUserInfo`

**핵심 컬럼 (조회 쿼리에서):**

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `USER_ID` | VARCHAR | 사용자 로그인 ID |
| `USER_NM` | VARCHAR | 사용자명 |
| `USER_PW` | VARCHAR | **BCrypt 해시 비밀번호** |
| `TEMP_USER_PW` | VARCHAR | 임시 비밀번호 (초기화 시 사용) |
| `USER_TP` | VARCHAR | 사용자 유형 ('00001'=어드민, '00002'=매니저, 등) |
| `PROP_CD` | VARCHAR | 자산 코드 ('0000000000'=시스템 관리자) |
| `CMPX_CD` | VARCHAR | 컴플렉스 코드 ('00000'=전체) |
| `DEPT_CD` | VARCHAR | 부서 코드 |
| `USER_LOCALE` | VARCHAR | 사용자 언어 설정 (예: ko_KR) |
| `USER_POS_LVL` | VARCHAR | 직급/포지션 레벨 |
| `MLTI_CMPX_YN` | CHAR(1) | 멀티 컴플렉스 사용 여부 (Y/N) |
| `USER_WK_STAT` | VARCHAR | 사용자 근무 상태 ('00001'만 로그인 허용) |
| `USE_YN` | CHAR(1) | 사용 여부 (Y=활성, N=비활성) |
| `PW_CHG_DT` | DATETIME | 비밀번호 변경 일자 |
| `EXP_PW_CHG_DT` | DATETIME | 비밀번호 만료 예정일 |

**비밀번호 해시 알고리즘:** BCryptPasswordEncoder (strength 11)  
**파일 위치:** `SecurityConfig.java` 라인 152-154

```java
public BCryptPasswordEncoder bCryptPasswordEncoder() {
    return new BCryptPasswordEncoder(11);
}
```

### 비밀번호 검증 로직

`LocalUserDetailsService.loadUserByUsername()` (라인 31-110):
- `userPw` 필드 → 실제 비밀번호 저장
- `tempUserPw` 필드 → 임시 비밀번호로 로그인한 경우 사용
- `LocalDaoAuthenticationProvider` (라인 10-18): 먼저 `userPw` 확인, 실패 시 `tempUserPw` 확인

### 추가 보안 필드

- `OTP_USE_YN`: OTP 인증 사용 여부
- `LOGIN_FAIL_CNT` (PMS_USER_LOGIN 테이블): 로그인 실패 횟수 추적
- `loginPassYn`: 로그인 잠금 여부 확인 (30분 잠금, 설정 가능)

---

## 3. 부서 코드 시스템

### 부서 정의 테이블: PMS_DIVISION

**쿼리 파일:** `CO_SQL.xml` (라인 655-675)  
**쿼리 ID:** `getDivisionList`

**컬럼:**
- `DEPT_CD`: 부서 코드
- `DEPT_NM`: 부서명
- `PROP_CD`: 자산 코드 (부서는 자산별로 스코프됨)
- `DEPT_TEL`: 부서 전화 (암호화 저장)
- `USE_YN`: 사용 여부

### 부서 코드 예시

쿼리에서 확인된 코드들:
- `'00000'` - 전체/기본값 (시스템 관리자)
- `'RM'` - 객실 부서 (Room)
- `'HK'` - 객실 관리 부서 (Housekeeping, 추정)

### 부서명 함수

**함수:** `FN_DIVISION_NM(PROP_CD, DEPT_CD)`  
쿼리 라인 24: `FN_DIVISION_NM(A.PROP_CD, A.DEPT_CD) DEPT_NM`

- Property와 Department Code 조합으로 부서명 조회
- 자산별로 부서 코드가 격리됨 (스코프: `PROP_CD + DEPT_CD`)

### 부서별 메뉴 권한

**테이블:** `PMS_DIVISION_MENU`  
쿼리 라인 314-316: 부서별 메뉴 권한 조회

```xml
LEFT JOIN PMS_DIVISION_MENU F
ON C.MENU_ID = F.MENU_ID
AND F.DEPT_CD = #{deptCd}
```

---

## 4. 역할/권한 모델

### 사용자 유형 (User Type) 기반 권한

PMS는 **테이블 기반 역할이 아닌 `USER_TP` 필드 기반** 권한 모델을 사용합니다.

**사용자 유형:**
- `'00001'` - **시스템 관리자** (super admin)
  - 모든 메뉴에 접근 가능
  - 별도 권한 체크 없음 (쿼리 라인 371-386)
  
- `'00002'` - **운영 관리자** (property/complex manager)
  
- 기타 유형 - **일반 사용자** (스태프)

**파일 위치:** `CO_SQL.xml` 라인 360-450 (`getAuthMenuInfo`)

### 3계층 권한 체크

메뉴 접근 권한은 **3가지 출처에서 우선순위로 확인됨:**

1. **PMS_USER_MENU** - 개별 사용자 메뉴 권한
2. **PMS_USER_TP_MENU** - 사용자 유형별 메뉴 권한
3. **PMS_DIVISION_MENU** - 부서별 메뉴 권한

**권한 결합 로직 (라인 376-380):**
```xml
DECODE(A.AUTH_CHK_YN, 'Y', 
  DECODE(IFNULL(D.AUTH_YN, 'N'), 'Y', 'Y', 
    DECODE(IFNULL(E.AUTH_YN, 'N'), 'Y', 'Y', 
      IFNULL(F.AUTH_YN, 'N'))), 'Y') AUTH_YN
```

→ 사용자 > 사용자타입 > 부서 순으로 OR 조건

### 메뉴 권한 필드

`PMS_MENU_MGMT` 테이블에서 메뉴별로:
- `AUTH_CHK_YN` - 권한 체크 필요 여부 (Y/N)
- `NEW_YN` - 신규 등록 권한
- `SAVE_YN` - 저장 권한
- `DEL_YN` - 삭제 권한
- `EXC_YN` - 실행 권한
- `PRT_YN` - 인쇄 권한

**쿼리 라인:** 337-346 (getAuthMenuInfo에서 권한별로 조건부 반환)

### 권한 확인 방법

**런타임 권한 확인:**
```java
// SessionUser.hasRole(String role) 라인 112-114
public boolean hasRole(String role) {
    return authorityList.stream()
        .filter(a -> a.equals("ROLE_" + role))
        .findAny().isPresent();
}

// 권한 추가
public void addAuthority(String role) {  // 라인 108-110
    authorityList.add("ROLE_" + role);
}
```

**메뉴 접근 체크:**
- `TokenAuthenticationService.getAuthentication()` (라인 140-146)
- 메뉴 권한 조회 후 `AUTH_YN = 'N'`이면 `AccessDeniedException` 발생

**부서 스코프 접근 제어:**
- 개별 서비스에서 `user.getDeptCd()` 비교로 데이터 필터링
- 예: 특정 부서의 작업만 조회 가능

---

## 5. 재사용 vs 신규 구축

### CCS(손님 요청 관리 시스템) 개발 시 패턴 매칭 가이드

| 영역 | PMS 패턴 | CCS 대응 전략 |
|------|---------|--------------|
| **인증 방식** | JWT 토큰 + BCrypt(11) | **재사용**: 동일한 JWT 토큰 방식 + BCrypt 해시 적용 |
| **사용자 테이블** | `PMS_USER_MTR` (PROP_CD, CMPX_CD, DEPT_CD) | **일부 재사용**: `USER_ID`, `USER_PW`, `TEMP_USER_PW`, `DEPT_CD` 컬럼 명명 규칙 채택; `PROP_CD`는 CCS에 맞게 조정 |
| **비밀번호 정책** | 임시 비밀번호 + 만료일 관리 | **재사용**: `TEMP_USER_PW`, `EXP_PW_CHG_DT` 필드 동일하게 구현 |
| **부서 코드** | `DEPT_CD` (PROP_CD 스코프) | **재사용**: 부서 코드 포맷과 저장 방식 채택 |
| **권한 모델** | 사용자타입 기반 (`USER_TP`) + 테이블 조인 | **신규 구축**: 간단한 역할 테이블(`CCS_USER_ROLE`) 설계; PMS의 3계층 권한은 복잡하므로 2계층(사용자-역할, 역할-메뉴)으로 단순화 |
| **메뉴/권한** | 3가지 테이블 조인 (사용자, 타입, 부서) | **신규 구축**: `CCS_ROLE`, `CCS_ROLE_MENU`, `CCS_PERMISSION` 테이블만 사용 |
| **세션/토큰** | JWT + Cookie (웹) / DB (API) | **재사용**: JWT 토큰 구조 및 쿠키 저장 방식 적용 |
| **부서 스코프** | 부서별 메뉴 권한 조회 | **재사용**: 부서별 데이터 필터링 로직(예: `WHERE DEPT_CD = ?`) 채택 |
| **로그인 잠금** | PMS_USER_LOGIN 테이블로 실패 추적 | **재사용**: 유사한 `CCS_LOGIN_ATTEMPT` 테이블로 로그인 실패 횟수 추적 |
| **다국어 지원** | USER_LOCALE + COOKIE 저장 | **신규 구축**: CCS의 조직 요구사항에 맞는 언어 설정 구현 |

### 권장 간단한 권한 모델 (CCS용)

```
CCS_USER
├─ user_id (PK)
├─ user_pw (BCrypt 해시)
├─ user_nm
├─ dept_cd (FK → CCS_DEPT)
├─ role_id (FK → CCS_ROLE)
└─ use_yn

CCS_ROLE
├─ role_id (PK)
├─ role_nm (예: "MANAGER", "STAFF")
└─ use_yn

CCS_ROLE_PERMISSION
├─ role_id (FK)
├─ permission_cd (예: "TASK_VIEW", "TASK_CREATE")
└─ grant_yn (Y/N)
```

이 모델은 PMS보다 **훨씬 간단**하면서도 부서별 권한 제어 가능합니다.

---

## 요약

PMS는 **JWT 토큰 기반 인증**, **BCrypt 비밀번호 해싱**, **3계층 권한 체계**(사용자-유형-부서)를 사용합니다. CCS 개발 시 JWT 토큰, BCrypt 해시, 부서 코드 체계는 재사용하되, 복잡한 3계층 권한은 간단한 역할-권한 2계층 모델로 단순화하여 구현하는 것을 권장합니다.
