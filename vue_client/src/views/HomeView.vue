<template>
	<div class="home">
		<h2>투숙객 요청</h2>
		<div class="card-grid">
			<router-link to="/amenity" class="card">
				<div class="icon">🛎️</div>
				<div class="title">어메니티 요청</div>
				<div class="subtitle">Amenity Request</div>
			</router-link>
			<router-link to="/housekeeping" class="card">
				<div class="icon">🧹</div>
				<div class="title">객실 정비 / DND</div>
				<div class="subtitle">Housekeeping</div>
			</router-link>
			<router-link to="/late-checkout" class="card">
				<div class="icon">⏰</div>
				<div class="title">레이트 체크아웃</div>
				<div class="subtitle">Late Checkout</div>
			</router-link>
		</div>
	</div>
</template>

<style scoped>
.home h2 { margin-bottom: 24px; font-size: 24px; }
.card-grid {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
	gap: 20px;
}
.card {
	background: #fff;
	border-radius: 12px;
	padding: 40px 24px;
	text-align: center;
	text-decoration: none;
	color: inherit;
	box-shadow: 0 2px 8px rgba(0,0,0,0.06);
	transition: transform 0.15s, box-shadow 0.15s;
	min-height: 200px;
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
}
.card:active { transform: scale(0.97); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.card .icon { font-size: 56px; margin-bottom: 12px; }
.card .title { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
.card .subtitle { font-size: 13px; color: #8492a6; }
</style>
