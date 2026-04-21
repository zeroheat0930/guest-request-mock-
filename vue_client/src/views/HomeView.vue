<template>
	<div class="home">
		<!-- Hero: 환영 + 룸/이름 -->
		<section class="hero">
			<div class="hero-sub">WELCOME · 환영합니다</div>
			<h1 class="hero-title">{{ guestName || t('welcome') }}</h1>
			<div class="hero-room" v-if="guestRoomNo">
				<span class="hero-room-label">ROOM</span>
				<span class="hero-room-num">{{ guestRoomNo }}</span>
			</div>
			<div class="hero-divider"></div>
			<p class="hero-tagline">{{ t('home.tagline') }}</p>
		</section>

		<!-- 기능 타일 그리드 -->
		<section class="tiles">
			<router-link
				v-for="tab in tiles"
				:key="tab.featureCd"
				:to="tab.to"
				class="tile"
				:class="tileClass(tab.featureCd)"
			>
				<div class="tile-icon">{{ tab.icon }}</div>
				<div class="tile-label">{{ tab.label }}</div>
				<div class="tile-sub">{{ tileSub(tab.featureCd) }}</div>
				<div class="tile-arrow">→</div>
			</router-link>
		</section>
	</div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue';
import { enabledSortedFeatures } from '../features/featureStore.js';
import { t } from '../i18n/ui.js';

const tiles = computed(() => enabledSortedFeatures());
const guestRoomNo = ref('');
const guestName = ref('');

onMounted(() => {
	try {
		const r = sessionStorage.getItem('concierge.roomNo');
		guestRoomNo.value = r ? r + '호' : '';
		guestName.value = sessionStorage.getItem('concierge.guestName') || '';
	} catch {}
});

function tileClass(cd) {
	return 'tile-' + cd.toLowerCase().replace('_', '-');
}

function tileSub(cd) {
	const subs = {
		ko: {
			CHAT: '자연어로 요청하세요',
			AMENITY: '수건 · 생수 · 어메니티',
			HK: '청소 · 방해금지',
			LATE_CO: '체크아웃 시간 연장',
			NEARBY: '주변 정보 검색',
			PARKING: '차량 번호 등록',
			HISTORY: '내 요청 내역 확인'
		},
		en: {
			CHAT: 'Natural language requests',
			AMENITY: 'Towel · Water · Amenities',
			HK: 'Cleaning · Do Not Disturb',
			LATE_CO: 'Extend checkout time',
			NEARBY: 'Nearby recommendations',
			PARKING: 'Register vehicle',
			HISTORY: 'My requests'
		}
	};
	const lang = (window.localStorage?.getItem?.('concierge.lang') || 'ko').startsWith('en') ? 'en' : 'ko';
	return subs[lang][cd] || '';
}
</script>

<style scoped>
.home {
	max-width: 960px;
	margin: 0 auto;
	padding: var(--sp-10, 40px) var(--sp-6, 24px) var(--sp-12, 56px);
}

/* ═══ Hero ═══ */
.hero {
	text-align: center;
	margin-bottom: var(--sp-10, 40px);
}
.hero-sub {
	font-size: 11px;
	letter-spacing: 3px;
	color: rgba(201, 169, 110, 0.7);
	text-transform: uppercase;
	font-weight: 600;
	margin-bottom: 14px;
}
.hero-title {
	font-family: 'Georgia', 'Times New Roman', serif;
	font-size: 36px;
	font-weight: 400;
	color: var(--c-gold-light, #e8d5a3);
	letter-spacing: 0.5px;
	margin: 0 0 var(--sp-4, 16px);
	line-height: 1.2;
}
.hero-room {
	display: inline-flex;
	align-items: center;
	gap: 10px;
	padding: 8px 20px;
	background: rgba(201, 169, 110, 0.08);
	border: 1px solid rgba(201, 169, 110, 0.25);
	border-radius: 999px;
}
.hero-room-label {
	font-size: 10px;
	letter-spacing: 2px;
	color: rgba(201, 169, 110, 0.6);
	font-weight: 700;
}
.hero-room-num {
	font-size: 16px;
	font-weight: 700;
	color: var(--c-gold, #c9a96e);
	letter-spacing: 0.5px;
}
.hero-divider {
	width: 60px;
	height: 1px;
	background: linear-gradient(90deg, transparent, var(--c-gold, #c9a96e), transparent);
	margin: var(--sp-6, 24px) auto;
}
.hero-tagline {
	font-size: 14px;
	color: rgba(232, 224, 212, 0.6);
	letter-spacing: 0.3px;
	line-height: 1.6;
	margin: 0;
}

/* ═══ 타일 그리드 ═══ */
.tiles {
	display: grid;
	grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
	gap: var(--sp-4, 16px);
}
.tile {
	position: relative;
	display: flex;
	flex-direction: column;
	padding: 24px 22px;
	border-radius: 16px;
	background: linear-gradient(135deg, #1a2d4d 0%, #0f1d35 100%);
	border: 1px solid rgba(201, 169, 110, 0.18);
	color: var(--c-text-light, #e8e0d4);
	text-decoration: none;
	transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
	overflow: hidden;
	min-height: 150px;
}
.tile::before {
	content: '';
	position: absolute;
	inset: 0;
	background: radial-gradient(circle at 100% 0%, rgba(201, 169, 110, 0.12) 0%, transparent 60%);
	pointer-events: none;
	opacity: 0.8;
}
.tile:hover, .tile:focus-visible {
	transform: translateY(-3px);
	border-color: rgba(201, 169, 110, 0.5);
	box-shadow: 0 10px 30px rgba(11, 31, 59, 0.4), 0 0 0 1px rgba(201, 169, 110, 0.15);
}
.tile:active { transform: translateY(-1px); }

.tile-icon {
	font-size: 38px;
	line-height: 1;
	margin-bottom: 14px;
	filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}
.tile-label {
	font-size: 17px;
	font-weight: 700;
	color: var(--c-gold-light, #e8d5a3);
	letter-spacing: 0.3px;
	margin-bottom: 6px;
	font-family: 'Georgia', serif;
}
.tile-sub {
	font-size: 12px;
	color: rgba(232, 224, 212, 0.55);
	line-height: 1.5;
	flex: 1;
}
.tile-arrow {
	position: absolute;
	top: 20px;
	right: 20px;
	font-size: 18px;
	color: rgba(201, 169, 110, 0.35);
	transition: transform 0.2s ease, color 0.2s ease;
}
.tile:hover .tile-arrow {
	color: var(--c-gold, #c9a96e);
	transform: translateX(4px);
}

/* 기능별 액센트 */
.tile-chat { background: linear-gradient(135deg, #1e3461 0%, #0f1d35 100%); }
.tile-amenity { background: linear-gradient(135deg, #2b2c4e 0%, #1a1a2e 100%); }
.tile-hk { background: linear-gradient(135deg, #1b3a4b 0%, #0f2230 100%); }
.tile-late-co { background: linear-gradient(135deg, #3a2a4e 0%, #221530 100%); }
.tile-nearby { background: linear-gradient(135deg, #1a3d3d 0%, #0f2525 100%); }
.tile-parking { background: linear-gradient(135deg, #3a2d1b 0%, #241b10 100%); }
.tile-history { background: linear-gradient(135deg, #2d2d3f 0%, #18182a 100%); }

/* ═══ Mobile ═══ */
@media (max-width: 720px) {
	.home { padding: var(--sp-6, 24px) var(--sp-4, 16px) var(--sp-8, 32px); }
	.hero { margin-bottom: var(--sp-6, 24px); }
	.hero-title { font-size: 26px; }
	.hero-divider { margin: var(--sp-4, 16px) auto; }
	.hero-tagline { font-size: 13px; }
	.tiles {
		grid-template-columns: repeat(2, 1fr);
		gap: 12px;
	}
	.tile {
		padding: 18px 16px;
		min-height: 130px;
	}
	.tile-icon { font-size: 30px; margin-bottom: 10px; }
	.tile-label { font-size: 14px; }
	.tile-sub { font-size: 10.5px; }
	.tile-arrow { top: 14px; right: 14px; font-size: 14px; }
}
</style>
