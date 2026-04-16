<template>
	<div class="admin">
		<div class="head">
			<h2>🧑‍🍳 CCS 스태프 관리</h2>
			<div class="bar">
				<label>propCd
					<input v-model="propCd" @change="load" />
				</label>
				<label>cmpxCd
					<input v-model="cmpxCd" @change="load" />
				</label>
				<button @click="load" :disabled="busy">새로고침</button>
				<button class="ghost" @click="goBack">뒤로</button>
			</div>
		</div>

		<div v-if="err" class="err">{{ err }}</div>
		<div v-if="successMsg" class="success">{{ successMsg }}</div>

		<!-- 부서 목록 -->
		<section class="section">
			<div class="section-head">
				<h3>부서 목록</h3>
				<button class="btn-add" @click="openAddForm" :disabled="busy">+ 부서 추가</button>
			</div>

			<!-- 추가 폼 -->
			<div v-if="addFormOpen" class="inline-form">
				<div class="form-row">
					<label>부서 코드
						<input v-model="addForm.deptCd" placeholder="예) FOOD" />
					</label>
					<label>부서명
						<input v-model="addForm.deptNm" placeholder="예) 식음료" />
					</label>
					<label>정렬순서
						<input v-model.number="addForm.sortOrd" type="number" placeholder="0" style="width:80px" />
					</label>
					<div class="form-actions">
						<button class="btn-save" @click="createDept" :disabled="busy || !addForm.deptCd">저장</button>
						<button class="btn-cancel" @click="closeAddForm">취소</button>
					</div>
				</div>
			</div>

			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>deptCd</th>
							<th>deptNm</th>
							<th>sortOrd</th>
							<th>useYn</th>
							<th>작업</th>
						</tr>
					</thead>
					<tbody>
						<template v-for="d in depts" :key="d.deptCd">
							<!-- 일반 행 -->
							<tr v-if="editingDeptCd !== d.deptCd">
								<td>{{ d.deptCd }}</td>
								<td>{{ d.deptNm }}</td>
								<td>{{ d.sortOrd ?? '-' }}</td>
								<td>
									<span :class="['badge', d.useYn === 'Y' ? 'badge-on' : 'badge-off']">
										{{ d.useYn === 'Y' ? '사용' : '미사용' }}
									</span>
								</td>
								<td class="actions">
									<button class="btn-sm btn-edit" @click="startEdit(d)">수정</button>
									<button class="btn-sm btn-delete" @click="deleteDept(d.deptCd)">삭제</button>
								</td>
							</tr>
							<!-- 인라인 편집 행 -->
							<tr v-else class="editing-row">
								<td>{{ d.deptCd }}</td>
								<td><input class="cell-input" v-model="editForm.deptNm" /></td>
								<td><input class="cell-input cell-input-sm" v-model.number="editForm.sortOrd" type="number" /></td>
								<td>
									<label class="switch" :title="editForm.useYn === 'Y' ? '사용' : '미사용'">
										<input
											type="checkbox"
											:checked="editForm.useYn === 'Y'"
											@change="editForm.useYn = $event.target.checked ? 'Y' : 'N'"
										/>
										<span class="slider" />
									</label>
								</td>
								<td class="actions">
									<button class="btn-sm btn-save" @click="saveEdit(d.deptCd)" :disabled="busy">저장</button>
									<button class="btn-sm btn-cancel" @click="cancelEdit">취소</button>
								</td>
							</tr>
						</template>
						<tr v-if="!depts.length">
							<td colspan="5" class="dim">데이터 없음</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>

		<!-- 직원 목록 -->
		<section class="section">
			<h3>직원 목록</h3>
			<div class="table-wrap">
				<table>
					<thead>
						<tr>
							<th>loginId</th>
							<th>staffNm</th>
							<th>deptCd</th>
							<th>positionCd</th>
							<th>useYn</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="s in staffList" :key="s.staffId">
							<td>{{ s.loginId }}</td>
							<td>{{ s.staffNm }}</td>
							<td>{{ s.deptCd }}</td>
							<td>{{ s.positionCd ?? '-' }}</td>
							<td>
								<span :class="['badge', s.useYn === 'Y' ? 'badge-on' : 'badge-off']">
									{{ s.useYn === 'Y' ? '재직' : '미사용' }}
								</span>
							</td>
						</tr>
						<tr v-if="!staffList.length">
							<td colspan="5" class="dim">데이터 없음</td>
						</tr>
					</tbody>
				</table>
			</div>
		</section>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import { API_BASE } from '../api/client.js';
import {
	createAdminDept,
	updateAdminDept,
	deleteAdminDept,
} from '../api/client.js';

const TOKEN_KEY = 'concierge.adminToken';

const router = useRouter();
const propCd = ref('0000000010');
const cmpxCd = ref('00001');
const depts = ref([]);
const staffList = ref([]);
const busy = ref(false);
const err = ref('');
const successMsg = ref('');

// 추가 폼 상태
const addFormOpen = ref(false);
const addForm = ref({ deptCd: '', deptNm: '', sortOrd: 0 });

// 인라인 편집 상태
const editingDeptCd = ref(null);
const editForm = ref({ deptNm: '', sortOrd: 0, useYn: 'Y' });

function getToken() {
	try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}

function gotoLogin() {
	try { sessionStorage.removeItem(TOKEN_KEY); } catch {}
	router.replace('/admin/login');
}

function goBack() {
	router.push('/admin/features');
}

function showSuccess(msg) {
	successMsg.value = msg;
	setTimeout(() => { successMsg.value = ''; }, 2500);
}

async function load() {
	const t = getToken();
	if (!t) { gotoLogin(); return; }
	err.value = '';
	busy.value = true;
	try {
		const headers = { 'X-Admin-Token': t };
		const params = { propCd: propCd.value, cmpxCd: cmpxCd.value };

		const [deptRes, staffRes] = await Promise.all([
			axios.get(`${API_BASE}/concierge/admin/departments`, { params, headers, timeout: 8000 }),
			axios.get(`${API_BASE}/concierge/admin/staff`, { params, headers, timeout: 8000 })
		]);

		depts.value = deptRes.data?.list ?? [];
		staffList.value = staffRes.data?.list ?? [];
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `조회 실패: ${e.response?.data?.message || e.message}`;
	} finally {
		busy.value = false;
	}
}

// 추가 폼
function openAddForm() {
	addForm.value = { deptCd: '', deptNm: '', sortOrd: 0 };
	addFormOpen.value = true;
	cancelEdit();
}

function closeAddForm() {
	addFormOpen.value = false;
}

async function createDept() {
	if (!addForm.value.deptCd) return;
	err.value = '';
	busy.value = true;
	try {
		await createAdminDept({
			...addForm.value,
			propCd: propCd.value,
			cmpxCd: cmpxCd.value,
		});
		closeAddForm();
		showSuccess('부서가 추가되었습니다.');
		await load();
	} catch (e) {
		err.value = `추가 실패: ${e.response?.data?.message || e.message || '알 수 없는 오류'}`;
		busy.value = false;
	}
}

// 인라인 편집
function startEdit(dept) {
	closeAddForm();
	editingDeptCd.value = dept.deptCd;
	editForm.value = { deptNm: dept.deptNm || '', sortOrd: dept.sortOrd ?? 0, useYn: dept.useYn || 'Y' };
}

function cancelEdit() {
	editingDeptCd.value = null;
}

async function saveEdit(deptCd) {
	err.value = '';
	busy.value = true;
	try {
		await updateAdminDept(deptCd, {
			...editForm.value,
			propCd: propCd.value,
			cmpxCd: cmpxCd.value,
		});
		cancelEdit();
		showSuccess('부서 정보가 수정되었습니다.');
		await load();
	} catch (e) {
		err.value = `수정 실패: ${e.response?.data?.message || e.message || '알 수 없는 오류'}`;
		busy.value = false;
	}
}

async function deleteDept(deptCd) {
	if (!confirm(`'${deptCd}' 부서를 정말 삭제하시겠습니까?`)) return;
	err.value = '';
	busy.value = true;
	try {
		await deleteAdminDept(deptCd);
		showSuccess('부서가 삭제되었습니다.');
		await load();
	} catch (e) {
		err.value = `삭제 실패: ${e.response?.data?.message || e.message || '알 수 없는 오류'}`;
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
.bar button.ghost { background: transparent; color: #4a5568; }

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
