<template>
	<div class="admin">
		<div class="head">
			<h2>⚙️ 컨시어지 기능 관리</h2>
			<div class="bar">
				<label>propCd
					<input v-model="propCd" @change="load" />
				</label>
				<button @click="load" :disabled="busy">새로고침</button>
				<button class="primary" @click="save" :disabled="busy || hasJsonErrors">저장</button>
			</div>
		</div>

		<div v-if="err" class="err">{{ err }}</div>

		<div v-if="sortedRows.length" class="list">
			<div
				v-for="r in sortedRows"
				:key="r.featureCd"
				class="feature-card"
			>
				<div class="row-main">
					<div class="meta">
						<span class="icon">{{ META[r.featureCd]?.icon || '·' }}</span>
						<div class="names">
							<div class="kr">{{ META[r.featureCd]?.label || r.featureCd }}</div>
							<div class="en">{{ META[r.featureCd]?.labelEn || r.featureCd }}</div>
						</div>
					</div>
					<div class="ctrls">
						<label class="switch" :title="r.useYn === 'Y' ? '사용' : '미사용'">
							<input
								type="checkbox"
								:checked="r.useYn === 'Y'"
								@change="r.useYn = $event.target.checked ? 'Y' : 'N'"
							/>
							<span class="slider" />
						</label>
						<input
							class="sort"
							type="number"
							v-model.number="r.sortOrd"
							title="sortOrd"
						/>
						<button class="adv" type="button" @click="toggleAdv(r.featureCd)">
							{{ expanded[r.featureCd] ? '▲ 고급 설정' : '▼ 고급 설정' }}
						</button>
					</div>
				</div>
				<div v-if="expanded[r.featureCd]" class="adv-panel">
					<label>configJson
						<textarea
							rows="6"
							v-model="cfgText[r.featureCd]"
							@blur="validateJson(r.featureCd)"
							placeholder='{ "key": "value" }'
						/>
					</label>
					<div v-if="cfgErr[r.featureCd]" class="json-err">JSON 형식 오류</div>
				</div>
			</div>
		</div>
		<div v-else class="dim">데이터 없음</div>

		<transition name="fade">
			<div v-if="toast" class="toast">{{ toast }}</div>
		</transition>
	</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import { API_BASE } from '../api/client.js';
import { FEATURE_META } from '../features/featureStore.js';

const META = FEATURE_META;
const TOKEN_KEY = 'concierge.adminToken';

const router = useRouter();
const propCd = ref('HQ');
const rows = ref([]);
const busy = ref(false);
const err = ref('');
const toast = ref('');

// 카드 표시 순서는 load 시점에만 재계산해서 편집 도중 점프하지 않도록 고정.
const sortedRows = computed(() => rows.value);

const expanded = reactive({});
const cfgText = reactive({});
const cfgErr = reactive({});

const hasJsonErrors = computed(() => Object.values(cfgErr).some(Boolean));

function getToken() {
	try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}

function gotoLogin() {
	try { sessionStorage.removeItem(TOKEN_KEY); } catch {}
	router.replace('/admin/login');
}

function toggleAdv(cd) {
	expanded[cd] = !expanded[cd];
}

function validateJson(cd) {
	const txt = (cfgText[cd] || '').trim();
	if (!txt) { cfgErr[cd] = false; return; }
	try {
		JSON.parse(txt);
		cfgErr[cd] = false;
	} catch {
		cfgErr[cd] = true;
	}
}

function showToast(msg) {
	toast.value = msg;
	setTimeout(() => { toast.value = ''; }, 2000);
}

function hydrateRows(list) {
	const sorted = [...list].sort((a, b) => (a.sortOrd || 0) - (b.sortOrd || 0));
	rows.value = sorted;
	for (const k of Object.keys(cfgText)) delete cfgText[k];
	for (const k of Object.keys(cfgErr)) delete cfgErr[k];
	for (const r of sorted) {
		let txt = '';
		if (r.configJson != null) {
			if (typeof r.configJson === 'string') {
				txt = r.configJson;
			} else {
				try { txt = JSON.stringify(r.configJson, null, 2); } catch { txt = ''; }
			}
		}
		cfgText[r.featureCd] = txt;
		cfgErr[r.featureCd] = false;
	}
}

async function load() {
	const t = getToken();
	if (!t) { gotoLogin(); return; }
	err.value = '';
	busy.value = true;
	try {
		const res = await axios.get(`${API_BASE}/concierge/admin/features`, {
			params: { propCd: propCd.value },
			headers: { 'X-Admin-Token': t },
			timeout: 8000
		});
		hydrateRows(res.data?.map?.list || []);
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `조회 실패: ${e.response?.data?.resMsg || e.message}`;
	} finally {
		busy.value = false;
	}
}

async function save() {
	const t = getToken();
	if (!t) { gotoLogin(); return; }
	// 저장 전 모든 고급 설정 JSON 재검증 (blur 없이 저장 눌렀을 때 대비)
	for (const r of rows.value) validateJson(r.featureCd);
	if (hasJsonErrors.value) {
		err.value = '고급 설정 JSON 형식 오류를 수정해주세요';
		return;
	}
	err.value = '';
	busy.value = true;
	try {
		const payload = rows.value.map(r => {
			const txt = (cfgText[r.featureCd] || '').trim();
			let configJson = null;
			if (txt) {
				try { configJson = JSON.parse(txt); } catch { configJson = null; }
			}
			return {
				featureCd: r.featureCd,
				useYn: r.useYn,
				sortOrd: r.sortOrd,
				configJson
			};
		});
		const res = await axios.put(`${API_BASE}/concierge/admin/features`, payload, {
			params: { propCd: propCd.value },
			headers: { 'X-Admin-Token': t, 'Content-Type': 'application/json' },
			timeout: 8000
		});
		hydrateRows(res.data?.map?.list || payload);
		showToast('저장 완료');
	} catch (e) {
		if (e.response?.status === 401) { gotoLogin(); return; }
		err.value = `저장 실패: ${e.response?.data?.resMsg || e.message}`;
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
.admin { max-width: 760px; }
.head { margin-bottom: 16px; }
.admin h2 { margin: 0 0 12px; color: #1a3a6e; }
.bar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.bar label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #4a5568; }
.bar input { padding: 6px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 14px; }
.bar button { padding: 8px 14px; border: 1px solid #cbd5e0; background: #f7fafc; border-radius: 6px; cursor: pointer; font-size: 13px; }
.bar button.primary { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }
.bar button.primary:disabled { opacity: 0.5; cursor: not-allowed; }
.bar button.ghost { background: transparent; color: #4a5568; }

.err { background: #fff5f5; color: #c53030; padding: 10px 12px; border-radius: 6px; margin-bottom: 12px; font-size: 13px; }
.dim { color: #a0aec0; padding: 24px; background: #fff; border-radius: 12px; text-align: center; }

.list { display: flex; flex-direction: column; gap: 12px; }
.feature-card {
	background: #fff;
	border-radius: 12px;
	padding: 16px 20px;
	transition: box-shadow 0.18s ease;
}
.feature-card:hover { box-shadow: 0 4px 16px rgba(26, 58, 110, 0.08); }

.row-main { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.meta { display: flex; align-items: center; gap: 14px; min-width: 0; }
.icon { font-size: 26px; }
.names .kr { font-size: 15px; font-weight: 700; color: #1a3a6e; }
.names .en { font-size: 12px; color: #8492a6; margin-top: 2px; }

.ctrls { display: flex; align-items: center; gap: 12px; }
.sort {
	width: 80px; padding: 6px 8px; border: 1px solid #cbd5e0; border-radius: 6px;
	font-size: 13px; text-align: center;
}
.adv {
	padding: 6px 10px; border: 1px solid #edf2f7; background: #edf2f7; color: #4a5568;
	border-radius: 6px; cursor: pointer; font-size: 12px;
}

/* iOS 스타일 토글 — 44×24 트랙 + 20px 썸, 숨겨진 checkbox + .slider span */
.switch { position: relative; display: inline-block; width: 44px; height: 24px; flex-shrink: 0; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider {
	position: absolute; inset: 0;
	background: #cbd5e0;
	border-radius: 999px;
	cursor: pointer;
	transition: background 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.slider::before {
	content: '';
	position: absolute;
	left: 2px; top: 2px;
	width: 20px; height: 20px;
	background: #fff;
	border-radius: 50%;
	box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
	transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.switch input:checked + .slider { background: #1a3a6e; }
.switch input:checked + .slider::before { transform: translateX(20px); }

.adv-panel { margin-top: 14px; padding-top: 14px; border-top: 1px dashed #edf2f7; }
.adv-panel label { display: block; font-size: 12px; color: #4a5568; font-weight: 600; }
.adv-panel textarea {
	display: block; width: 100%; margin-top: 6px;
	padding: 10px 12px; border: 1px solid #cbd5e0; border-radius: 6px;
	font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
	font-size: 12px; box-sizing: border-box; resize: vertical;
}
.json-err { margin-top: 6px; font-size: 12px; color: #c53030; }

.toast {
	position: fixed; bottom: 32px; left: 50%; transform: translateX(-50%);
	background: #1a3a6e; color: #fff; padding: 12px 24px;
	border-radius: 999px; font-size: 14px; font-weight: 600;
	box-shadow: 0 6px 20px rgba(26, 58, 110, 0.35);
	z-index: 9999;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
