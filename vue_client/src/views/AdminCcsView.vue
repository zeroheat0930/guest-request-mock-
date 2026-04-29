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
		<div v-if="msg" class="success">{{ msg }}</div>

		<!-- 부서 관리 (PMS 부서 마스터 — UI 만 시연, 실제 변경은 상용화 단계 PMS REST API 위임) -->
		<section class="section">
			<div class="section-head">
				<h3>{{ t('admin.ccs.dept.list') }}</h3>
				<button class="btn-add" @click="pmsDeptTodo()">
					+ {{ t('admin.ccs.dept.add') }}
				</button>
			</div>

			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>{{ t('admin.ccs.col.deptCd') }}</th>
							<th>{{ t('admin.ccs.col.deptNm') }}</th>
							<th>{{ t('admin.ccs.col.useYn') }}</th>
							<th>{{ t('admin.ccs.col.action') }}</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="d in pmsDepts" :key="d.deptCd">
							<td class="mono">{{ d.deptCd }}</td>
							<td>{{ d.deptNm || '-' }}</td>
							<td>
								<label class="switch">
									<input type="checkbox" :checked="d.useYn === 'Y'" @change="pmsDeptTodo()" />
									<span class="slider"></span>
								</label>
							</td>
							<td class="actions">
								<button class="btn-sm btn-edit" @click="pmsDeptTodo()">{{ t('admin.ccs.edit') }}</button>
								<button class="btn-sm btn-delete" @click="pmsDeptTodo()">{{ t('admin.ccs.delete') }}</button>
							</td>
						</tr>
						<tr v-if="!pmsDepts.length">
							<td colspan="4" class="dim">{{ t('admin.ccs.empty') }}</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>

		<!-- 컨시어지 자체 라우팅 부서 (INV.CCS_DEPARTMENT) — UI 비공개, 백엔드 디폴트 라우팅 룰이 자동 처리 -->
		<section v-if="false" class="section">
			<div class="section-head">
				<h3>🛎️ 컨시어지 라우팅 부서
					<span class="dim-note">— 게스트 요청이 라우팅되는 부서. 추가/수정/삭제 가능</span>
				</h3>
				<button class="btn-add" :disabled="busy || addingDept" @click="startAddDept">
					+ {{ t('admin.ccs.dept.add') }}
				</button>
			</div>

			<!-- 인라인 추가 폼 -->
			<div v-if="addingDept" class="inline-form">
				<div class="form-row">
					<label>
						{{ t('admin.ccs.dept.code') }}
						<input v-model="newDept.deptCd" :placeholder="t('admin.ccs.dept.code.placeholder')" maxlength="10" />
					</label>
					<label>
						{{ t('admin.ccs.dept.name') }}
						<input v-model="newDept.deptNm" :placeholder="t('admin.ccs.dept.name.placeholder')" maxlength="40" />
					</label>
					<label>
						{{ t('admin.ccs.dept.sort') }}
						<input v-model.number="newDept.sortOrd" type="number" min="0" max="999" class="cell-input-sm" />
					</label>
					<div class="form-actions">
						<button class="btn-sm btn-save" :disabled="busy" @click="saveNewDept">{{ t('admin.ccs.save') }}</button>
						<button class="btn-sm btn-cancel" :disabled="busy" @click="cancelAddDept">{{ t('admin.ccs.cancel') }}</button>
					</div>
				</div>
			</div>

			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>{{ t('admin.ccs.col.deptCd') }}</th>
							<th>{{ t('admin.ccs.col.deptNm') }}</th>
							<th>{{ t('admin.ccs.col.sortOrd') }}</th>
							<th>{{ t('admin.ccs.col.useYn') }}</th>
							<th>{{ t('admin.ccs.col.action') }}</th>
						</tr>
					</thead>
					<tbody>
						<template v-for="d in invDepts" :key="d.deptCd">
							<tr v-if="editingDeptCd !== d.deptCd">
								<td class="mono">{{ d.deptCd }}</td>
								<td>{{ d.deptNm || '-' }}</td>
								<td>{{ d.sortOrd ?? '-' }}</td>
								<td>
									<span :class="['badge', d.useYn === 'Y' ? 'badge-on' : 'badge-off']">
										{{ d.useYn === 'Y' ? t('admin.ccs.on') : t('admin.ccs.off') }}
									</span>
								</td>
								<td class="actions">
									<button class="btn-sm btn-edit" :disabled="busy" @click="startEditDept(d)">{{ t('admin.ccs.edit') }}</button>
									<button class="btn-sm btn-delete" :disabled="busy" @click="deleteDept(d)">{{ t('admin.ccs.delete') }}</button>
								</td>
							</tr>
							<tr v-else class="editing-row">
								<td class="mono">{{ d.deptCd }}</td>
								<td><input class="cell-input" v-model="editDept.deptNm" maxlength="40" /></td>
								<td><input class="cell-input cell-input-sm" v-model.number="editDept.sortOrd" type="number" min="0" max="999" /></td>
								<td>
									<label class="switch">
										<input type="checkbox" :checked="editDept.useYn === 'Y'" @change="editDept.useYn = ($event.target.checked ? 'Y' : 'N')" />
										<span class="slider"></span>
									</label>
								</td>
								<td class="actions">
									<button class="btn-sm btn-save" :disabled="busy" @click="saveEditDept">{{ t('admin.ccs.save') }}</button>
									<button class="btn-sm btn-cancel" :disabled="busy" @click="cancelEditDept">{{ t('admin.ccs.cancel') }}</button>
								</td>
							</tr>
						</template>
						<tr v-if="!invDepts.length">
							<td colspan="5" class="dim">{{ t('admin.ccs.empty') }}</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>

		<!-- 직원 목록 (PMS 마스터 + 어드민 토글/부서 변경 — PMS REST API 위임) -->
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
						<tr v-for="s in staffList" :key="s.staffId" :class="{ 'row-disabled': s.useYn !== 'Y' }">
							<td class="mono">{{ s.loginId }}</td>
							<td>{{ s.staffNm }}</td>
							<td>
								<select class="cell-input"
								        :disabled="busy || !canManageStaff(s)"
								        :value="s.deptCd || ''"
								        @change="changeDept(s, $event.target.value)">
									<option value="" disabled>{{ s.deptNm || '(부서 선택)' }}</option>
									<option v-for="d in pmsDepts" :key="d.deptCd" :value="d.deptCd">
										{{ d.deptNm }} ({{ d.deptCd }})
									</option>
								</select>
							</td>
							<td>
								<span class="role-chip">{{ roleLabel(s.userTp) }}</span>
							</td>
							<td>
								<label class="switch" :title="canManageStaff(s) ? '' : '관리 권한 없음'">
									<input type="checkbox"
									       :checked="s.useYn === 'Y'"
									       :disabled="busy || !canManageStaff(s)"
									       @change="toggleStaffUseYn(s, $event.target.checked)" />
									<span class="slider"></span>
								</label>
								<span :class="['badge', 'badge-inline', s.useYn === 'Y' ? 'badge-on' : 'badge-off']">
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
import { ref, reactive, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import { API_BASE, updateAdminStaffUseYn, updateAdminStaffDept } from '../api/client.js';
import { t } from '../i18n/ui.js';
import { useAdminContext } from '../composables/useAdminContext.js';

const TOKEN_KEY = 'ccs.token';

const router = useRouter();
const ctx = useAdminContext();
const propCd = computed(() => ctx.propCd.value);
const cmpxCd = computed(() => ctx.cmpxCd.value);

function goContextSelect() { router.push('/staff/context'); }

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

// 자기 자신 + 동급/상위 역할은 토글 차단 (백엔드도 검증)
function canManageStaff(s) {
	if (!s) return false;
	if (s.loginId === ctx.staffId.value) return false;
	const myTp = ctx.userTp.value || '';
	const tp = s.userTp || '';
	if (!myTp) return true;
	return myTp.localeCompare(tp) < 0;
}

// PMS API 모드 표시 (mock 일 땐 시연 안전 표기)
const pmsApiMode = '시연 환경: 메모리 오버라이드';

const pmsDepts = ref([]);
const invDepts = ref([]);
const staffList = ref([]);
const busy = ref(false);
const err = ref('');
const msg = ref('');

const addingDept = ref(false);
const newDept = reactive({ deptCd: '', deptNm: '', sortOrd: 0 });

const editingDeptCd = ref(null);
const editDept = reactive({ deptCd: '', deptNm: '', sortOrd: 0, useYn: 'Y' });

function getToken() {
	try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}
function gotoLogin() {
	try { sessionStorage.removeItem(TOKEN_KEY); } catch {}
	router.replace('/staff/login');
}
function goBack() { router.push('/admin/features'); }

function flashOk(text) { msg.value = text; setTimeout(() => { msg.value = ''; }, 2500); }

// 부서 마스터 변경(추가/수정/삭제/USE_YN 토글)은 PMS REST API 위임 어댑터 활성 시 동작.
// 시연 환경에선 UI 만 노출하고 클릭 시 안내 토스트만.
function pmsDeptTodo() { flashOk(t('admin.ccs.dept.todo')); }
function flashErr(e) {
	if (e?.response?.status === 401) { gotoLogin(); return; }
	err.value = e?.response?.data?.message || e?.message || t('admin.ccs.unknownErr');
}

function startAddDept() {
	err.value = ''; msg.value = '';
	newDept.deptCd = ''; newDept.deptNm = ''; newDept.sortOrd = (invDepts.value.length || 0) * 10 + 10;
	addingDept.value = true;
}
function cancelAddDept() { addingDept.value = false; }

async function saveNewDept() {
	const code = (newDept.deptCd || '').trim().toUpperCase();
	const name = (newDept.deptNm || '').trim();
	if (!code) { err.value = t('admin.ccs.dept.code') + ' 누락'; return; }
	if (!name) { err.value = t('admin.ccs.dept.name') + ' 누락'; return; }
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	busy.value = true; err.value = '';
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		await axios.post(`${API_BASE}/concierge/admin/departments`, {
			propCd: propCd.value, cmpxCd: cmpxCd.value,
			deptCd: code, deptNm: name, sortOrd: newDept.sortOrd ?? 0
		}, { headers, timeout: 8000 });
		addingDept.value = false;
		flashOk(t('admin.ccs.dept.addOk'));
		await load();
	} catch (e) { flashErr(e); }
	finally { busy.value = false; }
}

function startEditDept(d) {
	err.value = ''; msg.value = '';
	editingDeptCd.value = d.deptCd;
	editDept.deptCd = d.deptCd;
	editDept.deptNm = d.deptNm || '';
	editDept.sortOrd = d.sortOrd ?? 0;
	editDept.useYn = d.useYn === 'N' ? 'N' : 'Y';
}
function cancelEditDept() { editingDeptCd.value = null; }

async function saveEditDept() {
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	busy.value = true; err.value = '';
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		await axios.put(`${API_BASE}/concierge/admin/departments/${encodeURIComponent(editDept.deptCd)}`, {
			propCd: propCd.value, cmpxCd: cmpxCd.value,
			deptNm: editDept.deptNm, sortOrd: editDept.sortOrd ?? 0, useYn: editDept.useYn
		}, { headers, timeout: 8000 });
		editingDeptCd.value = null;
		flashOk(t('admin.ccs.dept.editOk'));
		await load();
	} catch (e) { flashErr(e); }
	finally { busy.value = false; }
}

async function toggleStaffUseYn(s, checked) {
	const useYn = checked ? 'Y' : 'N';
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	busy.value = true; err.value = '';
	try {
		await updateAdminStaffUseYn(s.loginId, useYn);
		s.useYn = useYn;
		flashOk(useYn === 'Y' ? '직원 활성화 완료' : '직원 비활성화 완료');
	} catch (e) {
		flashErr(e);
		await load();
	} finally { busy.value = false; }
}

async function changeDept(s, deptCd) {
	if (!deptCd || deptCd === s.deptCd) return;
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	busy.value = true; err.value = '';
	try {
		await updateAdminStaffDept(s.loginId, deptCd);
		s.deptCd = deptCd;
		const found = pmsDepts.value.find(d => d.deptCd === deptCd);
		s.deptNm = found ? found.deptNm : '';
		flashOk('부서 변경 완료');
	} catch (e) {
		flashErr(e);
		await load();
	} finally { busy.value = false; }
}

async function deleteDept(d) {
	const confirmMsg = t('admin.ccs.dept.deleteConfirm').replace('{0}', d.deptNm || d.deptCd);
	if (!confirm(confirmMsg)) return;
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	busy.value = true; err.value = '';
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		const params = { propCd: propCd.value, cmpxCd: cmpxCd.value };
		await axios.delete(`${API_BASE}/concierge/admin/departments/${encodeURIComponent(d.deptCd)}`, { params, headers, timeout: 8000 });
		flashOk(t('admin.ccs.dept.deleteOk'));
		await load();
	} catch (e) { flashErr(e); }
	finally { busy.value = false; }
}

async function load() {
	const tok = getToken();
	if (!tok) { gotoLogin(); return; }
	err.value = '';
	busy.value = true;
	try {
		const headers = { Authorization: `Bearer ${tok}` };
		const params = { propCd: propCd.value, cmpxCd: cmpxCd.value };
		const [pmsRes, invRes, staffRes] = await Promise.all([
			axios.get(`${API_BASE}/concierge/admin/departments`, { params, headers, timeout: 8000 }),
			axios.get(`${API_BASE}/concierge/admin/inv-departments`, { params, headers, timeout: 8000 }),
			axios.get(`${API_BASE}/concierge/admin/staff`, { params, headers, timeout: 8000 })
		]);
		pmsDepts.value = pmsRes.data?.map?.list || pmsRes.data?.list || [];
		invDepts.value = invRes.data?.map?.list || invRes.data?.list || [];
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
.dim-note { font-size: 11px; color: #a0aec0; font-weight: 400; margin-left: 6px; }
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
.switch input:disabled + .slider { opacity: 0.4; cursor: not-allowed; }

.badge-inline { margin-left: 6px; vertical-align: middle; }
tbody tr.row-disabled td { background: #fafafa; color: #a0aec0; }
tbody tr.row-disabled td:nth-child(1),
tbody tr.row-disabled td:nth-child(2) { text-decoration: line-through; }
</style>
