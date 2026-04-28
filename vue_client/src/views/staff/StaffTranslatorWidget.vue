<template>
	<div class="translator-root">
		<!-- 플로팅 버튼 -->
		<button v-if="!open" class="float-btn" @click="open = true" title="다국어 통역 도우미 (Claude)">
			🌐
		</button>

		<!-- 팝업 패널 -->
		<div v-if="open" class="panel">
			<div class="panel-head">
				<div class="head-title">
					<span class="emoji">🌐</span>
					<div>
						<div class="h1">통역 도우미</div>
						<div class="h2">{{ enabled ? 'Claude · 한·영·일·중' : 'API 미설정' }}</div>
					</div>
				</div>
				<button class="close" @click="open = false">✕</button>
			</div>

			<!-- Quick presets -->
			<div class="presets">
				<button
					v-for="p in PRESETS" :key="p.key"
					:class="['preset', { active: preset === p.key }]"
					@click="applyPreset(p)">
					{{ p.label }}
				</button>
			</div>

			<!-- Source -->
			<label class="row">
				<span class="lbl">{{ sourceLabel }}</span>
				<textarea v-model="srcText" rows="3" :placeholder="srcPlaceholder" />
			</label>

			<!-- Target lang -->
			<div class="lang-row">
				<span class="lbl">번역 →</span>
				<select v-model="targetLang">
					<option value="ko">한국어</option>
					<option value="en">English</option>
					<option value="ja">日本語</option>
					<option value="zh">简体中文</option>
				</select>
				<button class="run" :disabled="busy || !srcText.trim()" @click="run">
					{{ busy ? '번역 중…' : '번역 실행' }}
				</button>
			</div>

			<!-- Result -->
			<div v-if="result" class="result">
				<div class="result-head">
					<span class="badge">{{ flag(result.sourceLang) }} → {{ flag(result.targetLang) }}</span>
					<button class="copy" @click="copyResult">📋 복사</button>
				</div>
				<pre class="result-text">{{ result.translated }}</pre>
				<div class="result-foot">{{ result.model }}</div>
			</div>
			<div v-else-if="err" class="err">{{ err }}</div>
		</div>
	</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { postAiTranslate, getAiStatus } from '../../api/client.js';

const open = ref(false);
const enabled = ref(false);
const srcText = ref('');
const targetLang = ref('ko');
const preset = ref('guest_to_staff');
const busy = ref(false);
const result = ref(null);
const err = ref('');

const PRESETS = [
	{ key: 'guest_to_staff', label: '게스트 → 한국어',  target: 'ko', placeholder: '게스트가 보낸 외국어 메시지' },
	{ key: 'staff_to_en',    label: '한국어 → English', target: 'en', placeholder: '한국어 응대 문구' },
	{ key: 'staff_to_ja',    label: '한국어 → 日本語',   target: 'ja', placeholder: '한국어 응대 문구' },
	{ key: 'staff_to_zh',    label: '한국어 → 简体中文', target: 'zh', placeholder: '한국어 응대 문구' }
];

const sourceLabel = computed(() => {
	const p = PRESETS.find(x => x.key === preset.value);
	return p ? p.label.split(' → ')[0] : '원문';
});
const srcPlaceholder = computed(() => {
	const p = PRESETS.find(x => x.key === preset.value);
	return p?.placeholder || '';
});

function applyPreset(p) {
	preset.value = p.key;
	targetLang.value = p.target;
}

function flag(code) {
	if (code === 'ko') return '🇰🇷 한';
	if (code === 'en') return '🇺🇸 EN';
	if (code === 'ja') return '🇯🇵 日';
	if (code === 'zh') return '🇨🇳 中';
	return code || '?';
}

async function run() {
	busy.value = true; err.value = '';
	try {
		const res = await postAiTranslate({
			text: srcText.value,
			targetLang: targetLang.value,
			sourceLang: 'auto',
			tone: 'polite_hotel'
		});
		if (res.status !== 0) throw new Error(res.message || '번역 실패');
		result.value = res.map;
	} catch (e) {
		err.value = e?.message || '번역 실패';
	} finally {
		busy.value = false;
	}
}

async function copyResult() {
	if (!result.value?.translated) return;
	try { await navigator.clipboard.writeText(result.value.translated); }
	catch { /* HTTPS 외부 환경 아닌 경우 무시 */ }
}

onMounted(async () => {
	try {
		const s = await getAiStatus();
		enabled.value = !!(s?.map?.translateEnabled || s?.map?.enabled);
	} catch {}
});
</script>

<style scoped>
.translator-root { position: fixed; right: 24px; bottom: 24px; z-index: 500; }

.float-btn {
	width: 52px; height: 52px; border-radius: 50%;
	background: linear-gradient(135deg, #1a3a6e 0%, #2d5db8 100%);
	color: #fff; border: none; cursor: pointer; font-size: 22px;
	box-shadow: 0 4px 14px rgba(26,58,110,0.4);
	transition: transform 0.15s, box-shadow 0.15s;
}
.float-btn:hover { transform: scale(1.06); box-shadow: 0 6px 20px rgba(26,58,110,0.55); }

.panel {
	width: 380px; max-width: calc(100vw - 32px);
	background: #fff; border-radius: 14px;
	box-shadow: 0 12px 32px rgba(0,0,0,0.18);
	overflow: hidden; border: 1px solid #d9e8ff;
	display: flex; flex-direction: column;
	max-height: calc(100vh - 60px);
}

.panel-head {
	background: linear-gradient(135deg, #1a3a6e 0%, #2d5db8 100%);
	color: #fff; padding: 14px 18px;
	display: flex; justify-content: space-between; align-items: center;
}
.head-title { display: flex; gap: 10px; align-items: center; }
.emoji { font-size: 22px; }
.h1 { font-size: 14px; font-weight: 800; }
.h2 { font-size: 10px; opacity: 0.75; margin-top: 2px; font-family: ui-monospace, Menlo, monospace; }
.close { background: transparent; border: none; color: #fff; font-size: 16px; cursor: pointer; padding: 4px 8px; }

.presets { display: flex; gap: 6px; padding: 12px 14px 0; flex-wrap: wrap; }
.preset { padding: 5px 10px; border: 1px solid #cbd5e0; background: #f7fafc; border-radius: 999px; cursor: pointer; font-size: 11px; font-weight: 600; color: #4a5568; }
.preset.active { background: #1a3a6e; color: #fff; border-color: #1a3a6e; }

.row { padding: 12px 14px 0; display: flex; flex-direction: column; gap: 4px; }
.row .lbl { font-size: 11px; font-weight: 700; color: #8492a6; text-transform: uppercase; letter-spacing: 0.4px; }
.row textarea { padding: 8px 10px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; font-family: inherit; resize: vertical; }

.lang-row { display: flex; gap: 8px; align-items: center; padding: 10px 14px; }
.lang-row .lbl { font-size: 11px; font-weight: 700; color: #8492a6; }
.lang-row select { padding: 6px 8px; border: 1px solid #cbd5e0; border-radius: 6px; font-size: 13px; background: #fff; }
.lang-row .run { margin-left: auto; padding: 7px 14px; background: #1a3a6e; color: #fff; border: none; border-radius: 6px; font-weight: 700; cursor: pointer; font-size: 12px; }
.lang-row .run:disabled { opacity: 0.5; cursor: not-allowed; }

.result {
	margin: 0 14px 14px; background: #f0f7ff; border: 1px solid #cfe1ff;
	border-radius: 8px; padding: 10px 12px;
}
.result-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.badge { display: inline-block; padding: 2px 8px; border-radius: 999px; background: #fff; color: #1a3a6e; font-size: 10px; font-weight: 700; border: 1px solid #cfe1ff; }
.copy { background: transparent; border: 1px solid #cfe1ff; padding: 3px 10px; border-radius: 6px; cursor: pointer; font-size: 11px; color: #1a3a6e; }
.result-text { white-space: pre-wrap; word-break: break-word; font-size: 13px; line-height: 1.6; color: #1a3a6e; margin: 0; font-family: inherit; }
.result-foot { font-size: 9px; color: #8492a6; margin-top: 6px; font-family: ui-monospace, Menlo, monospace; }

.err { margin: 0 14px 14px; padding: 10px 12px; background: #fff5f5; color: #c53030; border-radius: 6px; font-size: 12px; border: 1px solid #fed7d7; }
</style>
