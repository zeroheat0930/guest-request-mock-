<template>
	<div class="login-shell">
		<div class="card">
			<header class="brand">
				<div class="logo">🧑‍🍳</div>
				<h1>DAOL CCS</h1>
				<p class="tag">Communication Center System</p>
			</header>

			<form @submit.prevent="submit" novalidate>
				<label>
					<span class="lb">아이디</span>
					<input
						ref="idInput"
						type="text"
						v-model="loginId"
						autocomplete="username"
						:disabled="busy"
						placeholder="hk1, fr1, eng1..."
					/>
				</label>
				<label>
					<span class="lb">비밀번호</span>
					<input
						type="password"
						v-model="password"
						autocomplete="current-password"
						:disabled="busy"
						placeholder="••••••••"
					/>
				</label>
				<button class="primary" type="submit" :disabled="busy || !loginId || !password">
					<span v-if="busy" class="spinner" />
					{{ busy ? '확인 중' : '로그인' }}
				</button>
				<div v-if="err" class="err">{{ err }}</div>
			</form>

			<details class="demo">
				<summary>데모 계정 빠른 입력</summary>
				<div class="demo-grid">
					<button
						v-for="d in DEMO_STAFF"
						:key="d.loginId"
						type="button"
						class="demo-btn"
						@click="quickFill(d.loginId)"
					>
						<div class="nm">{{ d.loginId }}</div>
						<div class="dept">{{ d.deptNm }}</div>
					</button>
				</div>
				<p class="hint">비밀번호는 모두 <code>test1234</code></p>
			</details>
		</div>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { postCcsLogin } from '../../api/client.js';

const PROP_CD = '0000000010';
const CMPX_CD = '00001';

const DEMO_STAFF = [
	{ loginId: 'hk1',  deptNm: '하우스키핑' },
	{ loginId: 'fr1',  deptNm: '프론트' },
	{ loginId: 'eng1', deptNm: '엔지니어링' },
	{ loginId: 'fb1',  deptNm: '식음료' }
];

const router = useRouter();
const loginId = ref('');
const password = ref('');
const err = ref('');
const busy = ref(false);
const idInput = ref(null);

function quickFill(id) {
	loginId.value = id;
	password.value = 'test1234';
	err.value = '';
}

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
		const map = res?.map || {};
		if (!map.token) {
			err.value = '서버 오류';
			return;
		}
		sessionStorage.setItem('ccs.token', map.token);
		sessionStorage.setItem('ccs.staff', JSON.stringify(map));
		router.replace('/staff');
	} catch (e) {
		// unwrapErr 는 {resCd, resMsg, map} 을 던지므로 status 가 직접 오지 않음.
		// 401 / 404 는 resCd 로 구분 (CcsAuthController: 9404=계정없음, 9102=비번/비활성)
		const code = e?.resCd;
		if (code === '9404') err.value = '계정이 없습니다';
		else if (code === '9102' || code === '9400') err.value = '로그인 실패';
		else err.value = '서버 오류';
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
	width: 64px;
	height: 64px;
	margin: 0 auto var(--sp-3);
	background: linear-gradient(135deg, var(--c-brand-500), var(--c-accent-500));
	border-radius: var(--r-lg);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 32px;
	box-shadow: var(--sh-brand);
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
	padding: 12px 14px;
	border: 1px solid var(--c-border-strong);
	border-radius: var(--r-sm);
	font-size: 15px;
	background: var(--c-bg);
	transition: border-color var(--t-fast), box-shadow var(--t-fast);
}
label input:focus {
	outline: none;
	border-color: var(--c-brand-500);
	background: var(--c-surface);
	box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.primary {
	width: 100%;
	padding: 13px;
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

.demo {
	margin-top: var(--sp-6);
	padding-top: var(--sp-5);
	border-top: 1px dashed var(--c-border);
}
.demo summary {
	cursor: pointer;
	font-size: var(--fs-sm);
	color: var(--c-text-soft);
	font-weight: 600;
	list-style: none;
	display: flex;
	align-items: center;
	gap: 6px;
	user-select: none;
}
.demo summary::-webkit-details-marker { display: none; }
.demo summary::before { content: '▸'; transition: transform var(--t-fast); color: var(--c-muted); }
.demo[open] summary::before { transform: rotate(90deg); }

.demo-grid {
	margin-top: var(--sp-3);
	display: grid;
	grid-template-columns: 1fr 1fr;
	gap: var(--sp-2);
}
.demo-btn {
	background: var(--c-bg);
	border: 1px solid var(--c-border);
	border-radius: var(--r-sm);
	padding: 10px 12px;
	text-align: left;
	cursor: pointer;
	transition: all var(--t-fast);
}
.demo-btn:hover {
	background: var(--c-brand-50);
	border-color: var(--c-brand-300);
}
.demo-btn .nm {
	font-size: var(--fs-sm);
	font-weight: 700;
	color: var(--c-brand-700);
}
.demo-btn .dept {
	font-size: var(--fs-xs);
	color: var(--c-text-dim);
	margin-top: 2px;
}
.hint {
	margin: var(--sp-3) 0 0;
	font-size: var(--fs-xs);
	color: var(--c-text-dim);
	text-align: center;
}
.hint code {
	background: var(--c-bg-soft);
	padding: 2px 6px;
	border-radius: 4px;
	font-family: ui-monospace, "SF Mono", Menlo, monospace;
	color: var(--c-brand-700);
}
</style>
