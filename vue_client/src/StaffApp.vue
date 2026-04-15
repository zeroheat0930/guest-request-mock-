<template>
	<div class="staff-app-shell">
		<router-view />
	</div>
</template>

<script setup>
/*
 * 스태프/관리자 번들 공용 셸.
 * 각 뷰(StaffDashboardView/RunnerView/AdminFeaturesView/AdminCcsView/*LoginView) 가
 * 자체 상단 헤더·로그아웃·탭을 이미 포함하고 있어 여기서는 추가 내비를 얹지 않음.
 * 게스트 번들의 LNB/feature store/게스트 토큰 부트스트랩은 일절 import 되지 않음.
 */
</script>

<style>
html, body, #app { height: 100%; margin: 0; }
body {
	font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans KR", sans-serif;
	background: #f5f7fa;
	color: #1a202c;
}
</style>

<style scoped>
.staff-app-shell {
	min-height: 100vh;
	background: #f5f7fa;
	padding: 24px 28px;
	box-sizing: border-box;
	overflow: auto;
}
@media (max-width: 720px) {
	.staff-app-shell { padding: 16px; }
}
</style>
