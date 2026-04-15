<template>
	<div class="login-wrap">
		<form class="card" @submit.prevent="submit">
			<h2>🔐 관리자 로그인</h2>
			<p class="sub">컨시어지 기능 관리 콘솔</p>
			<label>비밀번호
				<input
					ref="pwInput"
					type="password"
					v-model="pw"
					autocomplete="current-password"
					:disabled="busy"
				/>
			</label>
			<button class="primary" type="submit" :disabled="busy || !pw">
				{{ busy ? '확인 중…' : '로그인' }}
			</button>
			<div v-if="err" class="err">{{ err }}</div>
		</form>
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
.login-wrap {
	min-height: 70vh;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 24px;
}
.card {
	background: #fff;
	padding: 32px 28px;
	border-radius: 12px;
	width: 100%;
	max-width: 360px;
	box-shadow: 0 4px 16px rgba(26, 58, 110, 0.08);
}
.card h2 {
	margin: 0 0 4px;
	color: #1a3a6e;
	font-size: 20px;
}
.sub {
	margin: 0 0 20px;
	font-size: 13px;
	color: #8492a6;
}
label {
	display: block;
	font-size: 13px;
	color: #4a5568;
	font-weight: 600;
	margin-bottom: 16px;
}
label input {
	display: block;
	width: 100%;
	margin-top: 6px;
	padding: 10px 12px;
	border: 1px solid #cbd5e0;
	border-radius: 6px;
	font-size: 16px;
	box-sizing: border-box;
}
label input:focus {
	outline: none;
	border-color: #1a3a6e;
	box-shadow: 0 0 0 3px rgba(26, 58, 110, 0.12);
}
.primary {
	width: 100%;
	padding: 12px;
	background: #1a3a6e;
	color: #fff;
	border: none;
	border-radius: 8px;
	font-size: 15px;
	font-weight: 700;
	cursor: pointer;
}
.primary:disabled {
	opacity: 0.5;
	cursor: not-allowed;
}
.err {
	margin-top: 16px;
	padding: 10px 12px;
	background: #fff5f5;
	color: #c53030;
	border-radius: 6px;
	font-size: 13px;
}
</style>
