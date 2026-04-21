<template>
	<div class="login-shell">
		<div class="card">
			<header class="brand">
				<div class="logo"><img src="/daol-logo.png" alt="DAOL" /></div>
				<h1>DAOL CCS</h1>
				<p class="tag">Communication Center System</p>
			</header>

			<form @submit.prevent="submit" novalidate>
				<label>
					<span class="lb">{{ t('staff.login.id') }}</span>
					<input
						ref="idInput"
						type="text"
						v-model="loginId"
						autocomplete="username"
						:disabled="busy"
						:placeholder="t('staff.login.id')"
					/>
				</label>
				<label>
					<span class="lb">{{ t('staff.login.password') }}</span>
					<div class="pw-wrap">
						<input
							:type="showPw ? 'text' : 'password'"
							v-model="password"
							autocomplete="current-password"
							:disabled="busy"
							placeholder="••••••••"
						/>
						<button type="button" class="pw-toggle" @click="showPw = !showPw" tabindex="-1">
							<svg v-if="!showPw" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
								<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
							</svg>
							<svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
								<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
								<line x1="1" y1="1" x2="23" y2="23"/>
							</svg>
						</button>
					</div>
				</label>
				<button class="primary" type="submit" :disabled="busy || !loginId || !password">
					<span v-if="busy" class="spinner" />
					{{ busy ? t('auth.loading') : t('staff.login.submit') }}
				</button>
				<div v-if="err" class="err">{{ err }}</div>
			</form>

		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { postCcsLogin } from '../../api/client.js';
import { t } from '../../i18n/ui.js';

const PROP_CD = '0000000010';
const CMPX_CD = '00001';

const router = useRouter();
const loginId = ref('');
const password = ref('');
const err = ref('');
const busy = ref(false);
const showPw = ref(false);
const idInput = ref(null);

onMounted(() => {
	idInput.value?.focus();
});

async function submit() {
	if (!loginId.value || !password.value || busy.value) return;
	err.value = '';
	busy.value = true;
	try {
		const res = await postCcsLogin({
			loginId: loginId.value,
			password: password.value,
			propCd: PROP_CD,
			cmpxCd: CMPX_CD
		});
		// BaseController 는 ApiException 도 HTTP 200 + body{status:음수} 로 내림.
		// status: 0=성공, 404=계정없음, -20=비번오류, -30=비활성, 401=입력누락
		const code = res?.status;
		if (code !== 0 && code !== undefined) {
			if (code === 404) err.value = t('staff.login.fail');
			else if (code === -20) err.value = t('staff.login.fail');
			else if (code === -30 || code === 401) err.value = t('staff.login.fail');
			else err.value = t('error.server');
			password.value = '';
			return;
		}
		const map = res?.map || {};
		if (!map.token) {
			err.value = t('error.server');
			return;
		}
		sessionStorage.setItem('ccs.token', map.token);
		sessionStorage.setItem('ccs.staff', JSON.stringify(map));
		router.replace('/staff');
	} catch (e) {
		// 네트워크 실패 / HTTP 5xx 등 throw 되는 경우
		err.value = t('staff.login.fail');
		password.value = '';
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
	max-width: 420px;
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
	width: 80px;
	height: 80px;
	margin: 0 auto var(--sp-3);
	display: flex;
	align-items: center;
	justify-content: center;
}
.brand .logo img {
	width: 100%;
	height: 100%;
	object-fit: contain;
}
.brand h1 {
	font-size: 22px;
	margin: 0 0 4px;
	color: var(--c-brand-900);
	letter-spacing: 0.5px;
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
	box-sizing: border-box;
	padding: 13px 14px;
	border: 1px solid var(--c-border-strong);
	border-radius: var(--r-md);
	font-size: 15px;
	height: 48px;
	background: var(--c-bg);
	transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
.pw-wrap {
	position: relative;
}
.pw-wrap input {
	padding-right: 44px;
}
.pw-toggle {
	position: absolute;
	right: 8px;
	top: 50%;
	transform: translateY(-50%);
	background: none;
	border: none;
	cursor: pointer;
	padding: 6px;
	color: var(--c-text-dim);
	transition: color var(--t-fast);
}
.pw-toggle:hover {
	color: var(--c-brand-500);
}

label input:focus {
	outline: none;
	border-color: var(--c-brand-500);
	background: var(--c-surface);
	box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.primary {
	width: 100%;
	box-sizing: border-box;
	padding: 13px;
	height: 48px;
	margin-top: var(--sp-2);
	background: linear-gradient(135deg, var(--c-brand-500), var(--c-accent-500));
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
	box-shadow: var(--sh-brand);
	transition: transform var(--t-fast), box-shadow var(--t-fast), opacity var(--t-fast);
}
.primary:hover:not(:disabled) { transform: translateY(-1px); }
.primary:active:not(:disabled) { transform: translateY(0); }
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
</style>
