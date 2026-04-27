<template>
	<div class="admin">
		<div class="head">
			<h2>🧑‍🍳 {{ t('admin.ccs.title') }}</h2>
			<div class="bar">
				<span class="ctx-chip">🏨 {{ ctx.hotelNm.value || (ctx.propCd.value + ' / ' + ctx.cmpxCd.value) }}</span>
				<button v-if="ctx.canPickProperty.value || ctx.canPickComplex.value"
					class="ghost"
					@click="goContextSelect"
					:title="t('ctx.change')">
					🔄 {{ t('ctx.change') }}
				</button>
				<button class="search" @click="load" :disabled="busy">🔍 {{ t('admin.ccs.search') }}</button>
				<button class="ghost" @click="goBack">{{ t('admin.ccs.back') }}</button>
			</div>
		</div>

		<div v-if="err" class="err">{{ err }}</div>

		<!-- 부서 목록 (PMS_DIVISION 읽기 전용) -->
		<section class="section">
			<div class="section-head">
				<h3>{{ t('admin.ccs.dept.list') }}</h3>
			</div>

			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>{{ t('admin.ccs.col.deptCd') }}</th>
							<th>{{ t('admin.ccs.col.deptNm') }}</th>
							<th>{{ t('admin.ccs.col.useYn') }}</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="d in depts" :key="d.deptCd">
							<td class="mono">{{ d.deptCd }}</td>
							<td>{{ d.deptNm || '-' }}</td>
							<td>
								<span :class="['badge', d.useYn === 'Y' ? 'badge-on' : 'badge-off']">
									{{ d.useYn === 'Y' ? t('admin.ccs.on') : t('admin.ccs.off') }}
								</span>
							</td>
							<td class="actions">
								<button class="btn-sm btn-delete" :disabled="busy" @click="deleteDept(d)">삭제</button>
							</td>
						</tr>
						<tr v-if="!depts.length">
							<td colspan="4" class="dim">{{ t('admin.ccs.empty') }}</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>

		<!-- 직원 목록 -->
		<section class="section">
			<h3>{{ t('admin.ccs.staff.list') }}</h3>
			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>{{ t('admin.ccs.col.loginId') }}</th>
							<th>{{ t('admin.ccs.col.staffNm') }}</th>
							<th>{{ t('admin.ccs.col.deptNm') }}</th>
							<th>{{ t('admin.ccs.col.userTp') }}</th>
							<th>{{ t('admin.ccs.col.useYn') }}</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="s in staffList" :key="s.staffId">
							<td class="mono">{{ s.loginId }}</td>
							<td>{{ s.staffNm }}</td>
							<td>{{ s.deptNm || s.deptCd || '-' }}</td>
							<td>
								<span class="role-chip">{{ roleLabel(s.userTp) }}</span>
							</td>
							<td>
								<span :class="['badge', s.useYn === 'Y' ? 'badge-on' : 'badge-off']">
									{{ s.useYn === 'Y' ? t('admin.ccs.staff.active') : t('admin.ccs.staff.inactive') }}
								</span>
							</td>
						</tr>
						<tr v-if="!staffList.length">
							<td colspan="5" class="dim">{{ t('admin.ccs.empty') }}</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>
	</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import { API_BASE } from '../api/client.js';
import { t } from '../i18n/ui.js';
import { useAdminContext } from '../composables/useAdminContext.js';

const TOKEN_KEY = 'ccs.token';  // 스태프 JWT 재사용

const router = useRouter();
const ctx = useAdminContext();
// 선택된 호텔 컨텍스트 사용 (ctx.propCd / ctx.cmpxCd 는 이미 computed)
const propCd = computed(() => ctx.propCd.value);
const cmpxCd = computed(() => ctx.cmpxCd.value);

function goContextSelect() { router.push('/staff/context'); }

// PMS_USER_MTR.USER_TP → 한국어 라벨 매핑 (i18n 재사용)
const USER_TP_I18N = {
	'00001': 'role.sysAdmin',
	'00002': 'role.propAdmin',
	'00003': 'role.cmpxAdmin',
	'00004': 'role.normalUser',
	'00005': 'role.pos',
	'00007': 'role.hkMgr',
	'00008': 'role.hkUser',
};
function roleLabel(userTp) {
	const key = USER_TP_I18N[userTp] || 'role.staff';
	return t(key);
}
const depts = ref([]);
const staffList = ref([]);
const busy = ref(false);
const err = ref('');

function getToken() {
	try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}

function gotoLogin() {
	try { sessionStorage.removeItem(TOKEN_KEY); } catch {}
	router.replace('/staff/login');
}

function goBack() {
	router.push('/admin/features');
}

async function deleteDept(d) {
	if (!confirm(`부서 ${d.deptCd}${d.deptNm ? ' (' + d.deptNm + ')' : ''} 을(를) 삭제할까요?`)) return;
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	err.value = '';
	busy.value = true;
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		const params = { propCd: propCd.value, cmpxCd: cmpxCd.value };
		await axios.delete(`${API_BASE}/concierge/admin/departments/${encodeURIComponent(d.deptCd)}`, { params, headers, timeout: 8000 });
		await load();
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `삭제 실패: ${e.response?.data?.message || e.message}`;
	} finally {
		busy.value = false;
	}
}

async function load() {
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	err.value = '';
	busy.value = true;
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		const params = { propCd: propCd.value, cmpxCd: cmpxCd.value };
		const [deptRes, staffRes] = await Promise.all([
			axios.get(`${API_BASE}/concierge/admin/departments`, { params, headers, timeout: 8000 }),
			axios.get(`${API_BASE}/concierge/admin/staff`, { params, headers, timeout: 8000 })
		]);
		depts.value = deptRes.data?.map?.list || deptRes.data?.list || [];
		staffList.value = staffRes.data?.map?.list || staffRes.data?.list || [];
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `${t('admin.ccs.loadFail')}: ${e.response?.data?.message || e.message}`;
	} finally {
		busy.value = false;
	}
}

onMounted(() => {
	if (!getToken()) { gotoLogin(); return; }
	load();
});
</script>

<style scoped>
.admin { max-width: 900px; }
.head { margin-bottom: 16px; }
.admin h2 { margin: 0 0 12px; color: #1a3a6e; }
.bar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.bar label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #4a5568; }
.bar input { padding: 6px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 14px; width: 120px; }
.bar button { padding: 8px 14px; border: 1px solid #cbd5e0; background: #f7fafc; border-radius: 6px; cursor: pointer; font-size: 13px; }
.bar button:disabled { opacity: 0.5; cursor: not-allowed; }
.bar button.search { background: #1a3a6e; color: #fff; border-color: #1a3a6e; font-weight: 600; }
.bar button.icon { padding: 8px 10px; }
.bar button.ghost { background: transparent; color: #4a5568; }
.ctx-chip {
	display: inline-flex;
	align-items: center;
	padding: 6px 12px;
	background: #edf4ff;
	color: #1a3a6e;
	border-radius: 999px;
	font-size: 12px;
	font-weight: 700;
	font-family: ui-monospace, Menlo, monospace;
	letter-spacing: 0.3px;
}
.mono { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 12px; color: #4a5568; }
.dim-note { font-size: 11px; color: #a0aec0; }
.role-chip {
	display: inline-block;
	padding: 2px 10px;
	background: #e6f6ff;
	color: #1a3a6e;
	border-radius: 10px;
	font-size: 11px;
	font-weight: 600;
}

.err { background: #fff5f5; color: #c53030; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; }
.success { background: #f0fff4; color: #276749; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; border: 1px solid #c6f6d5; }

.section { margin-bottom: 24px; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.section-head h3 { font-size: 14px; font-weight: 700; color: #1a3a6e; margin: 0; }
.section h3 { font-size: 14px; font-weight: 700; color: #1a3a6e; margin: 0 0 10px; }

.btn-add {
	padding: 6px 14px;
	background: #1a3a6e;
	color: #fff;
	border: none;
	border-radius: 6px;
	font-size: 13px;
	cursor: pointer;
	font-weight: 600;
}
.btn-add:disabled { opacity: 0.5; cursor: not-allowed; }

/* 인라인 추가 폼 */
.inline-form {
	background: #edf2f7;
	border-radius: 8px;
	padding: 12px 14px;
	margin-bottom: 10px;
}
.form-row {
	display: flex;
	gap: 12px;
	align-items: flex-end;
	flex-wrap: wrap;
}
.form-row label {
	display: flex;
	flex-direction: column;
	gap: 4px;
	font-size: 12px;
	color: #4a5568;
	font-weight: 600;
}
.form-row input {
	padding: 6px 10px;
	border: 1px solid #cbd5e0;
	border-radius: 6px;
	font-size: 13px;
	background: #fff;
}
.form-actions { display: flex; gap: 6px; }

/* 테이블 */
.table-wrap {
	background: #fff;
	border-radius: 12px;
	overflow: hidden;
	box-shadow: 0 2px 8px rgba(26, 58, 110, 0.06);
}

table { width: 100%; border-collapse: collapse; font-size: 13px; }
thead { background: #edf2f7; }
thead th { padding: 10px 14px; text-align: left; font-weight: 600; color: #4a5568; }
tbody td { padding: 10px 14px; border-top: 1px solid #f0f4f8; color: #2d3748; }
tbody tr:hover td { background: #f7fafc; }
tbody tr.editing-row td { background: #fffbeb; }

.dim { text-align: center; color: #a0aec0; padding: 24px; }

/* 배지 */
.badge {
	display: inline-block;
	padding: 2px 8px;
	border-radius: 10px;
	font-size: 11px;
	font-weight: 600;
}
.badge-on { background: #c6f6d5; color: #276749; }
.badge-off { background: #e2e8f0; color: #718096; }

/* 작업 버튼 */
.actions { white-space: nowrap; }
.btn-sm {
	padding: 4px 10px;
	border-radius: 5px;
	font-size: 12px;
	cursor: pointer;
	border: 1px solid #cbd5e0;
	margin-right: 4px;
	font-weight: 500;
}
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-edit { background: #ebf8ff; color: #2b6cb0; border-color: #bee3f8; }
.btn-delete { background: #fff5f5; color: #c53030; border-color: #fed7d7; }
.btn-save { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }
.btn-cancel { background: #f7fafc; color: #4a5568; }

/* 셀 내 입력 */
.cell-input {
	padding: 4px 8px;
	border: 1px solid #bee3f8;
	border-radius: 5px;
	font-size: 13px;
	width: 100%;
	background: #fff;
}
.cell-input-sm { width: 70px; }

/* iOS 토글 스위치 */
.switch {
	position: relative;
	display: inline-block;
	width: 40px;
	height: 22px;
}
.switch input { opacity: 0; width: 0; height: 0; }
.slider {
	position: absolute;
	cursor: pointer;
	inset: 0;
	background: #cbd5e0;
	border-radius: 22px;
	transition: background 0.2s;
}
.slider::before {
	content: '';
	position: absolute;
	height: 16px;
	width: 16px;
	left: 3px;
	bottom: 3px;
	background: #fff;
	border-radius: 50%;
	transition: transform 0.2s;
}
.switch input:checked + .slider { background: #48bb78; }
.switch input:checked + .slider::before { transform: translateX(18px); }
</style>
