<template>
	<div class="login-wrap">
		<form class="card" @submit.prevent="submit">
			<h2>👔 스태프 로그인</h2>
			<p class="sub">컨시어지 컨트롤 시스템 (CCS)</p>
			<label>아이디
				<input
					ref="idInput"
					type="text"
					v-model="loginId"
					autocomplete="username"
					:disabled="busy"
				/>
			</label>
			<label>비밀번호
				<input
					type="password"
					v-model="password"
					autocomplete="current-password"
					:disabled="busy"
				/>
			</label>
			<button class="primary" type="submit" :disabled="busy || !loginId || !password">
				{{ busy ? '확인 중…' : '로그인' }}
			</button>
			<div v-if="err" class="err">{{ err }}</div>
		</form>
	</div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { postCcsLogin } from '../../api/client.js';

const PROP_CD = '0000000010';
const CMPX_CD = '00001';

const router = useRouter();
const loginId = ref('');
const password = ref('');
const err = ref('');
const busy = ref(false);
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
