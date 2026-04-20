#!/usr/bin/env bash
# 상용화 로드맵 진행하면서 GR/Auth/AI API 리그레션 확인용 스모크 테스트.
# 사용: bash api_server/smoke-test.sh [host]
#   기본 호스트는 localhost:8080. LAN 테스트 시 예: bash smoke-test.sh 192.168.0.51:8080
#
# US-004 부터 모든 /api/gr/** 엔드포인트가 JWT 필수이므로 제일 먼저 /api/auth/guest-token 으로
# 토큰을 받아 이후 모든 호출에 Authorization 헤더로 첨부한다.

set -u

HOST="${1:-localhost:8080}"
BASE="http://${HOST}/api"
FAIL=0

check() {
	local label="$1"
	local out="$2"
	local expected="$3"
	if echo "$out" | grep -q "$expected"; then
		echo "PASS  $label"
	else
		echo "FAIL  $label"
		echo "      got: $out"
		FAIL=$((FAIL + 1))
	fi
}

echo "=== Concierge API smoke test against ${BASE} ==="

# ---------- 0. 인증 없는 요청은 401 이어야 함 ----------
CODE_NO_AUTH=$(curl -s -o /dev/null -w "%{http_code}" "${BASE}/gr/reservation?rsvNo=R2026041300001")
check "unauth /gr/reservation → 401" "$CODE_NO_AUTH" "^401$"

# ---------- 1. 게스트 토큰 발급 ----------
AUTH=$(curl -sf -X POST "${BASE}/auth/guest-token" \
	-H 'content-type: application/json' \
	-d '{"rsvNo":"R2026041300001","chkInDt":"20260413","birthDt":"19800101"}')
check "POST /auth/guest-token" "$AUTH" '"resCd":"0000"'
check "  - perNm HONG GILDONG" "$AUTH" '"perNm":"HONG GILDONG"'
TOKEN=$(echo "$AUTH" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
	echo "FAIL  JWT not extracted"
	FAIL=$((FAIL + 1))
	exit 1
fi
AUTH_H="Authorization: Bearer ${TOKEN}"

# 잘못된 자격증명은 9101
WRONG=$(curl -s -X POST "${BASE}/auth/guest-token" \
	-H 'content-type: application/json' \
	-d '{"rsvNo":"R2026041300001","chkInDt":"20260413","birthDt":"19990101"}')
check "POST /auth/guest-token (wrong birthDt) → 9101" "$WRONG" '"resCd":"9101"'

# ---------- 2. 예약 ----------
R1=$(curl -sf -H "$AUTH_H" "${BASE}/gr/reservation?rsvNo=R2026041300001")
check "GET /gr/reservation (self)" "$R1" '"perNm":"HONG GILDONG"'
check "  - perUseLang ko_KR"       "$R1" '"perUseLang":"ko_KR"'
check "  - roomNo 1205"            "$R1" '"roomNo":"1205"'

# 남의 예약 조회는 9102 이어야 함
OTHER_CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH_H" "${BASE}/gr/reservation?rsvNo=R2026041300002")
OTHER_BODY=$(curl -s -H "$AUTH_H" "${BASE}/gr/reservation?rsvNo=R2026041300002")
check "GET /gr/reservation (other guest) → resCd 9102" "$OTHER_BODY" '"resCd":"9102"'

R2=$(curl -sf -H "$AUTH_H" "${BASE}/gr/reservation/list")
check "GET /gr/reservation/list (only mine, totCnt 1)" "$R2" '"totCnt":1'

# ---------- 3. 어메니티 ----------
R3=$(curl -sf -H "$AUTH_H" "${BASE}/gr/amenity/items")
check "GET /gr/amenity/items (AM001)" "$R3" '"itemCd":"AM001"'

R4=$(curl -sf -H "$AUTH_H" -X POST "${BASE}/gr/amenity" \
	-H 'content-type: application/json' \
	-d '{"rsvNo":"R2026041300001","roomNo":"1205","itemList":[{"itemCd":"AM001","qty":2}],"reqMemo":"smoke test"}')
check "POST /gr/amenity (AM001 x2)" "$R4" '"resCd":"0000"'

R5=$(curl -sf -H "$AUTH_H" "${BASE}/gr/amenity/list?rsvNo=R2026041300001")
check "GET /gr/amenity/list (includes itemCd AM001)" "$R5" '"itemCd":"AM001"'

# ---------- 4. 하우스키핑 ----------
R6=$(curl -sf -H "$AUTH_H" -X POST "${BASE}/gr/housekeeping" \
	-H 'content-type: application/json' \
	-d '{"rsvNo":"R2026041300001","hkStatCd":"MU","reqMemo":"smoke test"}')
check "POST /gr/housekeeping MU" "$R6" '"hkStatCd":"MU"'

R7=$(curl -sf -H "$AUTH_H" "${BASE}/gr/housekeeping?rsvNo=R2026041300001")
check "GET /gr/housekeeping (latest=MU)" "$R7" '"hkStatCd":"MU"'

# ---------- 5. 레이트 체크아웃 ----------
R8=$(curl -sf -H "$AUTH_H" "${BASE}/gr/late-checkout?rsvNo=R2026041300001&reqOutTm=1300")
check "GET /gr/late-checkout (+2h FREE)" "$R8" '"rateTpCd":"FREE"'

R9=$(curl -sf -H "$AUTH_H" -X POST "${BASE}/gr/late-checkout" \
	-H 'content-type: application/json' \
	-d '{"rsvNo":"R2026041300001","reqOutTm":"1300","addAmt":0}')
check "POST /gr/late-checkout" "$R9" '"resCd":"0000"'

# ---------- 6. AI 프록시 ----------
R10=$(curl -sf "${BASE}/ai/status")
check "GET /ai/status (public)" "$R10" '"enabled":true'

R11=$(curl -sf -H "$AUTH_H" -X POST "${BASE}/ai/chat" \
	-H 'content-type: application/json' \
	-d '{"text":"please bring me three towels","ctx":{"rsvNo":"R2026041300001","roomNo":"1205","chkOutTm":"1100","perUseLang":"en_US"}}')
check "POST /ai/chat (AM001 x3)" "$R11" '"itemCd":"AM001"'

# 인증 없이 AI chat 은 401
AI_NO_AUTH=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE}/ai/chat" \
	-H 'content-type: application/json' \
	-d '{"text":"hi","ctx":{}}')
check "POST /ai/chat unauth → 401" "$AI_NO_AUTH" "^401$"

# ---------- 7. 멀티 프로퍼티 격리 (US-005) ----------
# JJU 프로퍼티 게스트로 인증
AUTH_JJU=$(curl -sf -X POST "${BASE}/auth/guest-token" \
	-H 'content-type: application/json' \
	-d '{"rsvNo":"R2026041300003","chkInDt":"20260413","birthDt":"19900505"}')
check "POST /auth/guest-token (JJU guest)" "$AUTH_JJU" '"resCd":"0000"'
check "  - JJU perNm KIM MINJI"            "$AUTH_JJU" '"perNm":"KIM MINJI"'
TOKEN_JJU=$(echo "$AUTH_JJU" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
AUTH_H_JJU="Authorization: Bearer ${TOKEN_JJU}"

# JJU 게스트는 자기 예약만 조회 가능
JJU_SELF=$(curl -sf -H "$AUTH_H_JJU" "${BASE}/gr/reservation?rsvNo=R2026041300003")
check "GET /gr/reservation (JJU self)" "$JJU_SELF" '"roomNo":"2010"'

# HQ 예약은 9102 로 차단
JJU_TO_HQ=$(curl -s -H "$AUTH_H_JJU" "${BASE}/gr/reservation?rsvNo=R2026041300001")
check "GET /gr/reservation (JJU → HQ) blocked 9102" "$JJU_TO_HQ" '"resCd":"9102"'

# 반대 방향도 확인
HQ_TO_JJU=$(curl -s -H "$AUTH_H" "${BASE}/gr/reservation?rsvNo=R2026041300003")
check "GET /gr/reservation (HQ → JJU) blocked 9102" "$HQ_TO_JJU" '"resCd":"9102"'

# JJU 게스트가 자기 어메니티 요청을 등록
JJU_AM=$(curl -sf -H "$AUTH_H_JJU" -X POST "${BASE}/gr/amenity" \
	-H 'content-type: application/json' \
	-d '{"rsvNo":"R2026041300003","roomNo":"2010","itemList":[{"itemCd":"AM002","qty":1}],"reqMemo":"smoke JJU"}')
check "POST /gr/amenity (JJU self)" "$JJU_AM" '"resCd":"0000"'

# 그 결과는 JJU 게스트의 list 에만 보이고 HQ 게스트의 list 엔 안 보여야 함
JJU_LIST=$(curl -sf -H "$AUTH_H_JJU" "${BASE}/gr/amenity/list?rsvNo=R2026041300003")
check "GET /gr/amenity/list (JJU sees JJU req, AM002)" "$JJU_LIST" '"itemCd":"AM002"'

HQ_LIST=$(curl -sf -H "$AUTH_H" "${BASE}/gr/amenity/list?rsvNo=R2026041300001")
if echo "$HQ_LIST" | grep -q 'smoke JJU'; then
	echo "FAIL  HQ list leaked JJU data"
	FAIL=$((FAIL + 1))
else
	echo "PASS  HQ list does not contain JJU data"
fi

echo
if [ "$FAIL" -eq 0 ]; then
	echo "All smoke checks passed."
	exit 0
else
	echo "${FAIL} smoke check(s) failed."
	exit 1
fi
