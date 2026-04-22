import { computed, ref } from 'vue';

/**
 * 관리자 화면 공통 컨텍스트.
 *
 *  - staff  = sessionStorage.ccs.staff  (로그인 직후 JWT 응답 — 본인 계정의 기본 범위)
 *  - ctx    = sessionStorage.ccs.context (PropertyContextView 에서 선택한 "지금 관리할 호텔")
 *
 * 모든 관리자 뷰는 여기서 꺼낸 propCd / cmpxCd 를 쿼리 파라미터로 사용.
 *
 * 역할 기반 스코프
 *   SYS_ADMIN  (00001): 전체 프로퍼티/컴플렉스 선택 가능
 *   PROP_ADMIN (00002): 본인 propCd 고정 + 하위 컴플렉스 선택 가능
 *   CMPX_ADMIN (00003): 본인 propCd+cmpxCd 완전 고정 (context 자동 세팅)
 *   그 외 STAFF: 관리 컨텍스트 접근 불가 (라우터 가드가 차단)
 */
export function useAdminContext() {
	const staff = ref(readStaff());
	const ctxData = ref(readContext());

	function readStaff() {
		try { return JSON.parse(sessionStorage.getItem('ccs.staff') || '{}'); } catch { return {}; }
	}
	function readContext() {
		try { return JSON.parse(sessionStorage.getItem('ccs.context') || '{}'); } catch { return {}; }
	}
	function refresh() { staff.value = readStaff(); ctxData.value = readContext(); }
	function setContext(propCd, cmpxCd) {
		const c = { propCd, cmpxCd };
		sessionStorage.setItem('ccs.context', JSON.stringify(c));
		ctxData.value = c;
	}
	function clearContext() {
		sessionStorage.removeItem('ccs.context');
		ctxData.value = {};
	}

	const userTp = computed(() => staff.value?.userTp || '');
	const isSystemAdmin   = computed(() => userTp.value === '00001');
	const isPropertyAdmin = computed(() => userTp.value === '00002');
	const isComplexAdmin  = computed(() => userTp.value === '00003');
	const isAdmin         = computed(() => isSystemAdmin.value || isPropertyAdmin.value || isComplexAdmin.value);

	// 로그인 계정 자체의 기본 범위 (변경 불가)
	const myPropCd = computed(() => staff.value?.propCd || '');
	const myCmpxCd = computed(() => staff.value?.cmpxCd || '');

	// "지금 관리 중인" 호텔 — ctx 선택값 우선, 없으면 로그인 기본값
	const propCd = computed(() => ctxData.value?.propCd || myPropCd.value);
	const cmpxCd = computed(() => ctxData.value?.cmpxCd || myCmpxCd.value);

	const hasContext = computed(() => !!(ctxData.value?.propCd && ctxData.value?.cmpxCd));

	const canPickProperty = computed(() => isSystemAdmin.value);
	const canPickComplex  = computed(() => isSystemAdmin.value || isPropertyAdmin.value);

	const roleLabelKey = computed(() => {
		if (isSystemAdmin.value)   return 'role.sysAdmin';
		if (isPropertyAdmin.value) return 'role.propAdmin';
		if (isComplexAdmin.value)  return 'role.cmpxAdmin';
		return 'role.staff';
	});

	return {
		staff, refresh,
		staffId: computed(() => staff.value?.staffId || ''),
		staffNm: computed(() => staff.value?.staffNm || ''),
		myDeptCd: computed(() => staff.value?.deptCd || ''),
		userTp,
		isSystemAdmin, isPropertyAdmin, isComplexAdmin, isAdmin,
		myPropCd, myCmpxCd,
		propCd, cmpxCd,
		hasContext,
		canPickProperty, canPickComplex,
		roleLabelKey,
		setContext, clearContext,
	};
}
