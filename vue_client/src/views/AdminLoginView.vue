<template>
	<div class="login-shell">
		<div class="card">
			<header class="brand">
				<div class="logo">🔐</div>
				<h1>Admin Console</h1>
				<p class="tag">Concierge Management</p>
			</header>
			<form @submit.prevent="submit" novalidate>
				<label>
					<span class="lb">관리자 패스워드</span>
					<input
						ref="pwInput"
						type="password"
						v-model="pw"
						autocomplete="current-password"
						:disabled="busy"
						placeholder="••••••••"
					/>
				</label>
				<button class="primary" type="submit" :disabled="busy || !pw">
					<span v-if="busy" class="spinner" />
					{{ busy ? '확인 중' : '로그인' }}
				</button>
				<div v-if="err" class="err">{{ err }}</div>
			</form>
			<p class="hint">
				환경변수 <code>CONCIERGE_ADMIN_PW</code> 가 서버에 설정돼야 접속 가능합니다.
			</p>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import { API_BASE } from '../api/client.js';

const TOKEN_KEY = 'concierge.adminToken';

const router = useRouter();
const pw = ref('');
const err = ref('');
const busy = ref(false);
const pwInput = ref(null);

onMounted(() => {
	pwInput.value?.focus();
});

async function submit() {
	if (!pw.value || busy.value) return;
	err.value = '';
	busy.value = true;
	try {
		await axios.get(`${API_BASE}/concierge/admin/features`, {
			params: { propCd: 'HQ' },
			headers: { 'X-Admin-Token': pw.value },
			timeout: 8000
		});
		sessionStorage.setItem(TOKEN_KEY, pw.value);
		router.replace('/admin/features');
	} catch (e) {
		const status = e.response?.status;
		if (status === 401) {
			err.value = '인증 실패';
		} else if (status === 503) {
			err.value = '관리자 인증이 설정되지 않았습니다';
		} else {
			err.value = `서버 오류: ${e.response?.data?.resMsg || e.message}`;
		}
		pw.value = '';
		pwInput.value?.focus();
	} finally {
		busy.value = false;
	}
}
</script>

<style scoped>
.login-shell { width: 100%; display: flex; justify-content: center; }

.card {
	background: var(--c-surface);
	padding: var(--sp-8) var(--sp-8) var(--sp-6);
	border-radius: var(--r-xl);
	width: 100%;
	max-width: 400px;
	box-shadow: var(--sh-xl);
	border: 1px solid var(--c-border);
	animation: rise var(--t-slow) var(--ease-out) both;
}
@keyframes rise {
	from { opacity: 0; transform: translateY(16px) scale(0.98); }
	to   { opacity: 1; transform: translateY(0) scale(1); }
}

.brand { text-align: center; margin-bottom: var(--sp-6); }
.brand .logo {
	width: 64px;
	height: 64px;
	margin: 0 auto var(--sp-3);
	background: linear-gradient(135deg, #6b46c1, #805ad5);
	border-radius: var(--r-lg);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 30px;
	box-shadow: 0 6px 18px rgba(107, 70, 193, 0.32);
}
.brand h1 {
	font-size: 22px;
	margin: 0 0 4px;
	color: var(--c-brand-900);
	letter-spacing: 0.3px;
	font-weight: 800;
}
.brand .tag {
	margin: 0;
	font-size: var(--fs-xs);
	color: var(--c-text-dim);
	letter-spacing: 1px;
	text-transform: uppercase;
}

label { display: block; margin-bottom: var(--sp-4); }
.lb {
	display: block;
	font-size: var(--fs-xs);
	color: var(--c-text-soft);
	font-weight: 700;
	text-transform: uppercase;
	letter-spacing: 0.8px;
	margin-bottom: 6px;
}
label input {
	display: block;
	width: 100%;
	padding: 12px 14px;
	border: 1px solid var(--c-border-strong);
	border-radius: var(--r-sm);
	font-size: 15px;
	background: var(--c-bg);
	transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
label input:focus {
	outline: none;
	border-color: #805ad5;
	background: var(--c-surface);
	box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.18);
}

.primary {
	width: 100%;
	padding: 13px;
	background: linear-gradient(135deg, #6b46c1, #805ad5);
	color: #fff;
	border: none;
	border-radius: var(--r-md);
	font-size: 15px;
	font-weight: 800;
	letter-spacing: 0.3px;
	cursor: pointer;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: var(--sp-2);
	box-shadow: 0 6px 18px rgba(107, 70, 193, 0.32);
	transition: transform var(--t-fast), opacity var(--t-fast);
}
.primary:hover:not(:disabled) { transform: translateY(-1px); }
.primary:disabled { opacity: 0.55; cursor: not-allowed; box-shadow: none; }

.spinner {
	width: 14px;
	height: 14px;
	border: 2px solid rgba(255, 255, 255, 0.35);
	border-top-color: #fff;
	border-radius: 50%;
	animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.err {
	margin-top: var(--sp-4);
	padding: 10px 14px;
	background: var(--c-err-50);
	color: var(--c-err-600);
	border: 1px solid rgba(197, 48, 48, 0.2);
	border-radius: var(--r-sm);
	font-size: var(--fs-sm);
}

.hint {
	margin: var(--sp-6) 0 0;
	font-size: var(--fs-xs);
	color: var(--c-text-dim);
	text-align: center;
	line-height: 1.6;
}
.hint code {
	background: var(--c-bg-soft);
	padding: 2px 6px;
	border-radius: 4px;
	font-family: ui-monospace, "SF Mono", Menlo, monospace;
	color: #6b46c1;
}
</style>
