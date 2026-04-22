<template>
	<div class="ctx-shell">
		<div class="card">
			<header class="brand">
				<div class="logo">🏨</div>
				<h1>{{ t('ctx.title') }}</h1>
				<p class="tag">
					{{ t(ctx.roleLabelKey.value) }} · {{ ctx.staffNm.value || '—' }}
				</p>
			</header>

			<section class="section">
				<label class="lb">{{ t('ctx.property') }}</label>
				<select
					v-model="selectedPropCd"
					:disabled="!ctx.canPickProperty.value || loadingProps"
					@change="onPropChange"
				>
					<option value="">{{ loadingProps ? t('ctx.loading') : t('ctx.selectProperty') }}</option>
					<option v-for="p in properties" :key="p.propCd" :value="p.propCd">
						{{ p.propShrtNm || p.propFullNm || p.propCd }} ({{ p.propCd }})
					</option>
				</select>
				<div v-if="!ctx.canPickProperty.value" class="locked-note">
					🔒 {{ t('ctx.propertyLocked') }}
				</div>
			</section>

			<section class="section">
				<label class="lb">{{ t('ctx.complex') }}</label>
				<div v-if="loadingCmpx" class="dim">{{ t('ctx.loading') }}</div>
				<div v-else-if="!selectedPropCd" class="dim">{{ t('ctx.pickPropertyFirst') }}</div>
				<div v-else-if="complexes.length === 0" class="dim">{{ t('ctx.noComplex') }}</div>
				<div v-else class="cmpx-list">
					<label
						v-for="c in complexes"
						:key="c.cmpxCd"
						class="cmpx-item"
						:class="{ active: selectedCmpxCd === c.cmpxCd }"
					>
						<input type="radio" v-model="selectedCmpxCd" :value="c.cmpxCd" />
						<div class="cmpx-body">
							<div class="cmpx-nm">{{ c.cmpxNm || c.cmpxCd }}</div>
							<div class="cmpx-sub">
								{{ c.cmpxReprUserNm || '—' }} · {{ c.cmpxReprTel || '—' }}
							</div>
						</div>
						<span class="cmpx-cd">{{ c.cmpxCd }}</span>
					</label>
				</div>
			</section>

			<div v-if="err" class="err">{{ err }}</div>

			<button
				class="primary"
				:disabled="!selectedPropCd || !selectedCmpxCd || entering"
				@click="enter"
			>
				<span v-if="entering" class="spinner" />
				{{ entering ? t('ctx.entering') : t('ctx.enter') }}
			</button>
			<button class="ghost" @click="logout">{{ t('shell.logout') }}</button>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAccessibleProperties, fetchAccessibleComplexes } from '../api/client.js';
import { useAdminContext } from '../composables/useAdminContext.js';
import { t } from '../i18n/ui.js';

const router = useRouter();
const ctx = useAdminContext();

const properties = ref([]);
const complexes = ref([]);
const selectedPropCd = ref('');
const selectedCmpxCd = ref('');
const loadingProps = ref(false);
const loadingCmpx = ref(false);
const err = ref('');
const entering = ref(false);

function unwrapList(res) {
	// Responses.ListResponse → { status:0, map:{ list:[...] } } or { status:0, list:[...] }
	return res?.list || res?.map?.list || [];
}

async function loadProperties() {
	err.value = '';
	loadingProps.value = true;
	try {
		const res = await fetchAccessibleProperties();
		properties.value = unwrapList(res);
		// 현재 컨텍스트가 있거나, 단일 프로퍼티뿐이면 자동 선택
		if (ctx.propCd.value && properties.value.some(p => p.propCd === ctx.propCd.value)) {
			selectedPropCd.value = ctx.propCd.value;
		} else if (properties.value.length === 1) {
			selectedPropCd.value = properties.value[0].propCd;
		}
		if (selectedPropCd.value) await loadComplexes();
	} catch (e) {
		if (e?.status === -30 || e?.response?.status === 401) {
			router.replace('/staff/login');
			return;
		}
		err.value = e?.message || t('error.server');
	} finally {
		loadingProps.value = false;
	}
}

async function loadComplexes() {
	if (!selectedPropCd.value) { complexes.value = []; return; }
	err.value = '';
	loadingCmpx.value = true;
	try {
		const res = await fetchAccessibleComplexes(selectedPropCd.value);
		complexes.value = unwrapList(res);
		// 기존 선택값이 새 리스트에 없으면 초기화, 단일이면 자동 선택
		if (!complexes.value.some(c => c.cmpxCd === selectedCmpxCd.value)) {
			selectedCmpxCd.value = complexes.value.length === 1 ? complexes.value[0].cmpxCd : '';
		}
	} catch (e) {
		err.value = e?.message || t('error.server');
		complexes.value = [];
	} finally {
		loadingCmpx.value = false;
	}
}

async function onPropChange() {
	selectedCmpxCd.value = '';
	await loadComplexes();
}

function enter() {
	if (!selectedPropCd.value || !selectedCmpxCd.value) return;
	entering.value = true;
	ctx.setContext(selectedPropCd.value, selectedCmpxCd.value);
	router.replace('/staff');
}

function logout() {
	try {
		sessionStorage.removeItem('ccs.token');
		sessionStorage.removeItem('ccs.staff');
		sessionStorage.removeItem('ccs.context');
	} catch {}
	router.replace('/staff/login');
}

onMounted(async () => {
	// 비관리자는 들어오면 안 됨 (라우터 가드가 먼저 차단해야 정상)
	if (!ctx.isAdmin.value) {
		router.replace('/staff');
		return;
	}
	// CMPX_ADMIN 은 본인 propCd+cmpxCd 로 자동 고정 — 선택 단계 스킵
	if (ctx.isComplexAdmin.value) {
		ctx.setContext(ctx.myPropCd.value, ctx.myCmpxCd.value);
		router.replace('/staff');
		return;
	}
	// 현재 선택값 초기화 (있으면 유지)
	selectedPropCd.value = ctx.propCd.value || '';
	selectedCmpxCd.value = ctx.cmpxCd.value || '';
	await loadProperties();
});
</script>

<style scoped>
.ctx-shell {
	width: 100%;
	min-height: 100vh;
	display: flex;
	justify-content: center;
	align-items: center;
	padding: 40px 20px;
	background: #f5f7fa;
	box-sizing: border-box;
}
.card {
	background: #fff;
	padding: 32px 32px 24px;
	border-radius: 16px;
	width: 100%;
	max-width: 520px;
	box-shadow: 0 10px 40px rgba(11, 31, 59, 0.12);
	animation: rise 0.35s ease-out both;
	box-sizing: border-box;
}
@keyframes rise {
	from { opacity: 0; transform: translateY(16px) scale(0.98); }
	to   { opacity: 1; transform: translateY(0) scale(1); }
}

.brand { text-align: center; margin-bottom: 28px; }
.brand .logo {
	width: 64px; height: 64px;
	margin: 0 auto 12px;
	background: linear-gradient(135deg, #1a3a6e, #4299e1);
	border-radius: 16px;
	display: flex; align-items: center; justify-content: center;
	font-size: 30px;
	box-shadow: 0 6px 18px rgba(26, 58, 110, 0.25);
}
.brand h1 { font-size: 20px; margin: 0 0 6px; color: #1a3a6e; font-weight: 800; }
.brand .tag { margin: 0; font-size: 12px; color: #718096; }

.section { margin-bottom: 20px; }
.lb {
	display: block; font-size: 11px;
	color: #4a5568; font-weight: 700;
	text-transform: uppercase;
	letter-spacing: 0.8px;
	margin-bottom: 8px;
}
.section select {
	width: 100%;
	padding: 12px 14px;
	border: 1px solid #cbd5e0;
	border-radius: 8px;
	font-size: 15px;
	background: #fff;
	cursor: pointer;
}
.section select:disabled { background: #f7fafc; cursor: not-allowed; color: #4a5568; }
.section select option { color: #1a202c; background: #fff; }
.locked-note {
	margin-top: 6px; font-size: 11px; color: #a0aec0; padding-left: 4px;
}

.dim {
	padding: 16px;
	text-align: center;
	color: #a0aec0;
	background: #f7fafc;
	border-radius: 8px;
	font-size: 13px;
}

.cmpx-list {
	display: flex;
	flex-direction: column;
	gap: 8px;
	max-height: 280px;
	overflow-y: auto;
	padding: 2px;
}
.cmpx-item {
	display: flex;
	align-items: center;
	gap: 12px;
	padding: 14px 16px;
	border: 1px solid #e2e8f0;
	border-radius: 10px;
	cursor: pointer;
	transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
}
.cmpx-item:hover { border-color: #4299e1; background: #ebf8ff; }
.cmpx-item.active { border-color: #1a3a6e; background: #ebf4ff; box-shadow: 0 0 0 2px rgba(26, 58, 110, 0.12); }
.cmpx-item input { margin: 0; }
.cmpx-body { flex: 1; min-width: 0; }
.cmpx-nm { font-weight: 700; font-size: 15px; color: #1a3a6e; }
.cmpx-sub { font-size: 12px; color: #718096; margin-top: 2px; }
.cmpx-cd { font-size: 11px; color: #a0aec0; font-family: ui-monospace, Menlo, monospace; }

.err {
	padding: 10px 14px;
	background: #fff5f5;
	color: #c53030;
	border: 1px solid rgba(197, 48, 48, 0.2);
	border-radius: 8px;
	font-size: 13px;
	margin-bottom: 16px;
}

.primary {
	width: 100%;
	padding: 14px;
	background: linear-gradient(135deg, #1a3a6e, #4299e1);
	color: #fff;
	border: none;
	border-radius: 10px;
	font-size: 15px;
	font-weight: 800;
	cursor: pointer;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8px;
	margin-bottom: 8px;
	box-shadow: 0 6px 18px rgba(26, 58, 110, 0.25);
	transition: transform 0.15s, opacity 0.15s;
}
.primary:hover:not(:disabled) { transform: translateY(-1px); }
.primary:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }
.spinner {
	width: 14px; height: 14px;
	border: 2px solid rgba(255, 255, 255, 0.35);
	border-top-color: #fff;
	border-radius: 50%;
	animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.ghost {
	width: 100%;
	padding: 10px;
	background: transparent;
	color: #718096;
	border: none;
	border-radius: 6px;
	font-size: 13px;
	cursor: pointer;
}
.ghost:hover { background: #f7fafc; color: #4a5568; }
</style>
