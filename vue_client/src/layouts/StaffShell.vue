<template>
	<div class="ccs-shell">
		<aside class="ccs-lnb">
			<div class="brand">
				<div class="logo">🧑‍🍳</div>
				<div class="text">
					<div class="name">DAOL CCS</div>
					<div class="sub">Communication Center</div>
				</div>
			</div>

			<nav class="nav">
				<div class="group" v-if="hasStaff">
					<div class="group-label">스태프</div>
					<router-link to="/staff" class="item">
						<span class="ic">📋</span><span class="label">대시보드</span>
					</router-link>
				</div>

				<div class="group" v-if="hasAdmin">
					<div class="group-label">관리자</div>
					<router-link to="/admin/features" class="item">
						<span class="ic">⚙️</span><span class="label">기능 관리</span>
					</router-link>
					<router-link to="/admin/ccs" class="item">
						<span class="ic">👥</span><span class="label">스태프 관리</span>
					</router-link>
					<router-link to="/admin/qr" class="item">
						<span class="ic">📷</span><span class="label">QR 생성</span>
					</router-link>
				</div>
			</nav>

			<div class="foot">
				<div class="user">
					<div class="avatar">{{ initial }}</div>
					<div class="who">
						<div class="name">{{ displayName }}</div>
						<div class="role">{{ roleLabel }}</div>
					</div>
				</div>
				<button class="logout" @click="logout" title="로그아웃">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
						<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
						<polyline points="16 17 21 12 16 7" />
						<line x1="21" y1="12" x2="9" y2="12" />
					</svg>
				</button>
			</div>
		</aside>

		<main class="ccs-main">
			<slot />
		</main>
	</div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const staffToken = ref(null);
const adminToken = ref(null);
const staffInfo = ref({});

function refreshAuth() {
	try {
		staffToken.value = sessionStorage.getItem('ccs.token');
		adminToken.value = sessionStorage.getItem('concierge.adminToken');
		const raw = sessionStorage.getItem('ccs.staff');
		staffInfo.value = raw ? JSON.parse(raw) : {};
	} catch {
		staffInfo.value = {};
	}
}

onMounted(refreshAuth);
watch(() => route.fullPath, refreshAuth);

const hasStaff = computed(() => !!staffToken.value);
const hasAdmin = computed(() => !!adminToken.value);

const displayName = computed(() => {
	if (staffInfo.value?.staffNm) return staffInfo.value.staffNm;
	if (hasAdmin.value) return '관리자';
	return '—';
});

const initial = computed(() => {
	const n = displayName.value;
	return n && n !== '—' ? n.slice(0, 1) : '·';
});

const roleLabel = computed(() => {
	if (hasStaff.value && hasAdmin.value) return 'Staff · Admin';
	if (hasStaff.value) {
		const d = staffInfo.value?.deptCd;
		return d ? `${d} Staff` : 'Staff';
	}
	if (hasAdmin.value) return 'Admin';
	return '';
});

function logout() {
	try {
		sessionStorage.removeItem('ccs.token');
		sessionStorage.removeItem('ccs.staff');
		sessionStorage.removeItem('concierge.adminToken');
	} catch {}
	// 스태프가 있으면 스태프 로그인으로, 아니면 어드민 로그인으로.
	const target = hasStaff.value ? '/staff/login' : '/admin/login';
	staffToken.value = null;
	adminToken.value = null;
	router.replace(target);
}
</script>

<style scoped>
.ccs-shell {
	display: flex;
	min-height: 100vh;
	background: var(--c-bg);
}

/* ═══ LNB ═══ */
.ccs-lnb {
	width: 248px;
	background: linear-gradient(180deg, var(--c-brand-900), #0b1f3b);
	color: #fff;
	display: flex;
	flex-direction: column;
	flex-shrink: 0;
	box-shadow: 4px 0 24px rgba(11, 31, 59, 0.18);
}

.brand {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: var(--sp-6) var(--sp-5);
	border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.brand .logo {
	font-size: 32px;
	width: 44px;
	height: 44px;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.08);
	border-radius: var(--r-md);
}
.brand .text .name { font-size: var(--fs-lg); font-weight: 800; letter-spacing: 0.3px; }
.brand .text .sub {
	font-size: var(--fs-xs);
	opacity: 0.55;
	letter-spacing: 0.6px;
	text-transform: uppercase;
	margin-top: 2px;
}

.nav {
	flex: 1;
	padding: var(--sp-4) var(--sp-3);
	overflow-y: auto;
	display: flex;
	flex-direction: column;
	gap: var(--sp-5);
}
.group { display: flex; flex-direction: column; gap: 4px; }
.group-label {
	font-size: 10px;
	text-transform: uppercase;
	letter-spacing: 1.2px;
	opacity: 0.4;
	padding: 0 var(--sp-3) var(--sp-2);
}
.item {
	display: flex;
	align-items: center;
	gap: var(--sp-3);
	padding: 12px 14px;
	border-radius: var(--r-md);
	color: #cfd8dc;
	text-decoration: none;
	font-size: var(--fs-md);
	font-weight: 600;
	transition: background var(--t-fast) var(--ease-out), transform var(--t-fast) var(--ease-out);
}
.item .ic { font-size: 18px; width: 22px; text-align: center; }
.item:hover {
	background: rgba(255, 255, 255, 0.06);
	color: #fff;
}
.item.router-link-active {
	background: linear-gradient(135deg, var(--c-brand-500), var(--c-accent-500));
	color: #fff;
	box-shadow: var(--sh-brand);
}

.foot {
	padding: var(--sp-4) var(--sp-4) var(--sp-5);
	border-top: 1px solid rgba(255, 255, 255, 0.08);
	display: flex;
	align-items: center;
	gap: var(--sp-3);
}
.user { display: flex; align-items: center; gap: var(--sp-3); flex: 1; min-width: 0; }
.avatar {
	width: 36px;
	height: 36px;
	border-radius: var(--r-pill);
	background: linear-gradient(135deg, var(--c-brand-500), var(--c-accent-400));
	color: #fff;
	font-weight: 800;
	font-size: var(--fs-md);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
}
.who { min-width: 0; }
.who .name {
	font-size: var(--fs-sm);
	font-weight: 700;
	color: #fff;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.who .role {
	font-size: var(--fs-xs);
	opacity: 0.55;
	margin-top: 2px;
}
.logout {
	width: 36px;
	height: 36px;
	display: flex;
	align-items: center;
	justify-content: center;
	background: rgba(255, 255, 255, 0.06);
	color: #cfd8dc;
	border: none;
	border-radius: var(--r-sm);
	cursor: pointer;
	transition: background var(--t-fast) var(--ease-out);
}
.logout:hover { background: var(--c-err-500); color: #fff; }

/* ═══ Main ═══ */
.ccs-main {
	flex: 1;
	padding: var(--sp-8) var(--sp-8);
	overflow: auto;
	min-width: 0;
}

/* ═══ Mobile (< 720px) — LNB 을 상단 가로 바로 변환 ═══ */
@media (max-width: 720px) {
	.ccs-shell { flex-direction: column; width: 100%; max-width: 100vw; overflow-x: hidden; }
	.ccs-lnb {
		width: 100%;
		max-width: 100%;
		flex-direction: row;
		flex-wrap: wrap;
		box-shadow: 0 4px 12px rgba(11, 31, 59, 0.18);
	}
	.brand { flex: 0 0 100%; padding: var(--sp-4) var(--sp-5); min-width: 0; }
	.nav {
		flex: 1 1 100%;
		flex-direction: row;
		overflow-x: auto;
		padding: var(--sp-3);
		gap: var(--sp-2);
		max-width: 100%;
	}
	.group { flex-direction: row; gap: var(--sp-1); }
	.group-label { display: none; }
	.item { padding: 10px 14px; white-space: nowrap; }
	.foot {
		flex: 0 0 100%;
		padding: var(--sp-3) var(--sp-4);
		gap: var(--sp-2);
		justify-content: space-between;
		border-top: 1px solid rgba(255, 255, 255, 0.08);
		min-width: 0;
	}
	.user { min-width: 0; overflow: hidden; }
	.avatar { width: 32px; height: 32px; font-size: var(--fs-sm); }
	.logout { flex-shrink: 0; width: 36px; height: 36px; }
	.ccs-main { padding: var(--sp-4); width: 100%; max-width: 100%; overflow-x: hidden; }
}
</style>
