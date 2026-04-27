<template>
	<div class="admin">
		<div class="head">
			<h2>🔐 {{ t('admin.roleGrant.title') }}</h2>
			<div class="bar">
				<span class="ctx-chip">🏨 {{ ctx.hotelNm.value || (ctx.propCd.value + ' / ' + ctx.cmpxCd.value) }}</span>
				<button class="ghost" @click="loadStaff" :disabled="busy">🔍 {{ t('admin.roleGrant.refresh') }}</button>
				<button class="ghost" @click="goBack">{{ t('admin.ccs.back') }}</button>
			</div>
			<p class="hint">{{ t('admin.roleGrant.hint') }}</p>
		</div>

		<div v-if="err" class="err">{{ err }}</div>
		<div v-if="msg" class="success">{{ msg }}</div>

		<div class="layout">
			<!-- 좌: 사용자 목록 -->
			<section class="users">
				<div class="section-head">
					<h3>{{ t('admin.roleGrant.userList') }}</h3>
					<input v-model="filter" class="search-input" :placeholder="t('admin.roleGrant.search')" />
				</div>
				<div class="user-list">
					<button
						v-for="u in filteredStaff"
						:key="u.staffId"
						:class="['user-row', { active: selected?.staffId === u.staffId }]"
						@click="selectUser(u)"
					>
						<div class="user-name">{{ u.staffNm || u.loginId }}</div>
						<div class="user-meta">
							<span class="role-chip">{{ roleLabel(u.userTp) }}</span>
							<span class="dept">{{ u.deptNm || u.deptCd || '-' }}</span>
						</div>
					</button>
					<div v-if="!filteredStaff.length" class="dim">{{ t('admin.ccs.empty') }}</div>
				</div>
			</section>

			<!-- 우: 메뉴 권한 그리드 -->
			<section class="grid">
				<div class="section-head">
					<h3>
						{{ t('admin.roleGrant.menuList') }}
						<span v-if="selected" class="for-user">— {{ selected.staffNm || selected.loginId }}</span>
					</h3>
					<button v-if="selected" class="btn-save" @click="save" :disabled="saving || !dirty">
						💾 {{ saving ? t('admin.roleGrant.saving') : t('admin.roleGrant.save') }}
					</button>
				</div>

				<div v-if="!selected" class="empty-state">
					{{ t('admin.roleGrant.selectUser') }}
				</div>

				<div v-else class="menu-grid">
					<div v-for="m in MENUS" :key="m.code" class="menu-row">
						<div class="menu-info">
							<span class="menu-icon">{{ m.icon }}</span>
							<span class="menu-name">{{ t(m.labelKey) }}</span>
							<span class="menu-code">{{ m.code }}</span>
						</div>
						<label class="switch">
							<input type="checkbox"
								:checked="grants[m.code] === 'Y'"
								@change="toggle(m.code, $event.target.checked)" />
							<span class="slider"></span>
						</label>
					</div>
				</div>
			</section>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import { API_BASE } from '../api/client.js';
import { t } from '../i18n/ui.js';
import { useAdminContext } from '../composables/useAdminContext.js';
import { GRANTABLE_MENUS } from '../admin/menus.js';

const TOKEN_KEY = 'ccs.token';
const router = useRouter();
const ctx = useAdminContext();

const MENUS = GRANTABLE_MENUS;

const USER_TP_I18N = {
	'00001': 'role.sysAdmin',
	'00002': 'role.propAdmin',
	'00003': 'role.cmpxAdmin',
	'00004': 'role.normalUser',
	'00005': 'role.pos',
	'00007': 'role.hkMgr',
	'00008': 'role.hkUser'
};
function roleLabel(userTp) {
	return t(USER_TP_I18N[userTp] || 'role.staff');
}

const staffList = ref([]);
const selected = ref(null);
const grants = ref({});         // { menuCd: 'Y'|'N' }
const initialGrants = ref({});  // 비교용 — dirty 판정
const filter = ref('');
const busy = ref(false);
const saving = ref(false);
const err = ref('');
const msg = ref('');

function getToken() {
	try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}
function gotoLogin() {
	try { sessionStorage.removeItem(TOKEN_KEY); } catch {}
	router.replace('/staff/login');
}
function goBack() { router.push('/admin/features'); }

const filteredStaff = computed(() => {
	const q = filter.value.trim().toLowerCase();
	// 권한 부여 대상은 PROP_ADMIN(00002) / CMPX_ADMIN(00003) 만. SYS_ADMIN 본인은 제외, 일반 STAFF 도 제외.
	const candidates = staffList.value.filter(u => u.userTp === '00002' || u.userTp === '00003');
	if (!q) return candidates;
	return candidates.filter(u =>
		(u.staffNm || '').toLowerCase().includes(q)
		|| (u.loginId || '').toLowerCase().includes(q)
		|| (u.deptNm || '').toLowerCase().includes(q)
	);
});

const dirty = computed(() => {
	for (const m of MENUS) {
		if ((grants.value[m.code] || 'N') !== (initialGrants.value[m.code] || 'N')) return true;
	}
	return false;
});

async function loadStaff() {
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	err.value = ''; msg.value = '';
	busy.value = true;
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		const params = { propCd: ctx.propCd.value, cmpxCd: ctx.cmpxCd.value };
		const res = await axios.get(`${API_BASE}/concierge/admin/staff`, { params, headers, timeout: 8000 });
		staffList.value = res.data?.map?.list || res.data?.list || [];
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `${t('admin.roleGrant.loadFail')}: ${e.response?.data?.message || e.message}`;
	} finally {
		busy.value = false;
	}
}

async function selectUser(u) {
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	err.value = ''; msg.value = '';
	selected.value = u;
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		const res = await axios.get(`${API_BASE}/concierge/admin/role-grant`,
			{ params: { userId: u.staffId }, headers, timeout: 8000 });
		const list = res.data?.map?.grants || [];
		const next = {};
		for (const r of list) next[r.menuCd] = r.granted === 'Y' ? 'Y' : 'N';
		// 카탈로그에 없는 메뉴는 'N' 으로 보정
		for (const m of MENUS) if (!(m.code in next)) next[m.code] = 'N';
		grants.value = next;
		initialGrants.value = { ...next };
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `${t('admin.roleGrant.loadFail')}: ${e.response?.data?.message || e.message}`;
	}
}

function toggle(menuCd, checked) {
	grants.value[menuCd] = checked ? 'Y' : 'N';
}

async function save() {
	if (!selected.value || saving.value || !dirty.value) return;
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	err.value = ''; msg.value = '';
	saving.value = true;
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		const payload = { userId: selected.value.staffId, grants: grants.value };
		await axios.put(`${API_BASE}/concierge/admin/role-grant/bulk`, payload, { headers, timeout: 8000 });
		initialGrants.value = { ...grants.value };
		msg.value = t('admin.roleGrant.saved');
		setTimeout(() => { msg.value = ''; }, 2500);
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `${t('admin.roleGrant.saveFail')}: ${e.response?.data?.message || e.message}`;
	} finally {
		saving.value = false;
	}
}

onMounted(() => {
	if (!getToken()) { gotoLogin(); return; }
	loadStaff();
});
</script>

<style scoped>
.admin { max-width: 1100px; }
.head { margin-bottom: 16px; }
.admin h2 { margin: 0 0 12px; color: #1a3a6e; }
.bar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.bar button { padding: 8px 14px; border: 1px solid #cbd5e0; background: #f7fafc; border-radius: 6px; cursor: pointer; font-size: 13px; }
.bar button:disabled { opacity: 0.5; cursor: not-allowed; }
.bar button.ghost { background: transparent; color: #4a5568; }
.ctx-chip {
	display: inline-flex; align-items: center; padding: 6px 12px;
	background: #edf4ff; color: #1a3a6e; border-radius: 999px;
	font-size: 12px; font-weight: 700;
	font-family: ui-monospace, Menlo, monospace; letter-spacing: 0.3px;
}
.hint { font-size: 12px; color: #718096; margin: 8px 0 0; }

.err { background: #fff5f5; color: #c53030; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; }
.success { background: #f0fff4; color: #276749; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; border: 1px solid #c6f6d5; }

.layout {
	display: grid;
	grid-template-columns: 320px 1fr;
	gap: 20px;
	align-items: start;
}
.section-head {
	display: flex; align-items: center; justify-content: space-between;
	margin-bottom: 10px; gap: 8px;
}
.section-head h3 { font-size: 14px; font-weight: 700; color: #1a3a6e; margin: 0; }
.for-user { color: #4a5568; font-weight: 500; margin-left: 6px; font-size: 13px; }

.search-input {
	padding: 6px 10px; border: 1px solid #cbd5e0; border-radius: 6px;
	font-size: 13px; width: 140px;
}

/* User list */
.users {
	background: #fff; border-radius: 12px; padding: 14px;
	box-shadow: 0 2px 8px rgba(26, 58, 110, 0.06);
}
.user-list { display: flex; flex-direction: column; gap: 6px; max-height: 60vh; overflow-y: auto; }
.user-row {
	text-align: left;
	background: transparent;
	border: 1px solid #e2e8f0;
	padding: 10px 12px;
	border-radius: 8px;
	cursor: pointer;
	transition: all 0.15s;
}
.user-row:hover { background: #f7fafc; border-color: #cbd5e0; }
.user-row.active { background: #ebf4ff; border-color: #1a3a6e; }
.user-name { font-size: 14px; font-weight: 600; color: #2d3748; }
.user-meta { display: flex; gap: 8px; align-items: center; margin-top: 4px; font-size: 11px; color: #718096; }
.role-chip {
	display: inline-block; padding: 1px 8px;
	background: #e6f6ff; color: #1a3a6e; border-radius: 10px;
	font-weight: 600;
}
.dept { color: #4a5568; }

/* Menu grid */
.grid {
	background: #fff; border-radius: 12px; padding: 14px;
	box-shadow: 0 2px 8px rgba(26, 58, 110, 0.06);
}
.empty-state {
	padding: 40px 20px;
	text-align: center;
	color: #a0aec0;
	font-size: 13px;
}
.menu-grid { display: flex; flex-direction: column; gap: 4px; }
.menu-row {
	display: flex; align-items: center; justify-content: space-between;
	padding: 12px 14px; border-radius: 8px; transition: background 0.1s;
}
.menu-row:hover { background: #f7fafc; }
.menu-info { display: flex; align-items: center; gap: 10px; flex: 1; }
.menu-icon { font-size: 18px; width: 24px; text-align: center; }
.menu-name { font-size: 14px; color: #2d3748; font-weight: 500; }
.menu-code {
	font-family: ui-monospace, Menlo, monospace;
	font-size: 11px; color: #a0aec0;
}

.btn-save {
	padding: 8px 14px;
	background: #1a3a6e;
	color: #fff;
	border: none;
	border-radius: 6px;
	font-size: 13px;
	cursor: pointer;
	font-weight: 600;
}
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }

/* iOS toggle switch (AdminCcsView 와 동일) */
.switch {
	position: relative; display: inline-block; width: 40px; height: 22px; flex-shrink: 0;
}
.switch input { opacity: 0; width: 0; height: 0; }
.slider {
	position: absolute; cursor: pointer; inset: 0;
	background: #cbd5e0; border-radius: 22px;
	transition: background 0.2s;
}
.slider::before {
	content: ''; position: absolute;
	height: 16px; width: 16px; left: 3px; bottom: 3px;
	background: #fff; border-radius: 50%;
	transition: transform 0.2s;
}
.switch input:checked + .slider { background: #48bb78; }
.switch input:checked + .slider::before { transform: translateX(18px); }

.dim { text-align: center; color: #a0aec0; padding: 24px; font-size: 13px; }

@media (max-width: 720px) {
	.layout { grid-template-columns: 1fr; }
	.user-list { max-height: 30vh; }
}
</style>
