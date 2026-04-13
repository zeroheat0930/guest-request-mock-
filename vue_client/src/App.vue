<template>
	<div class="app-shell">
		<header class="app-header">
			<h1>🏨 다올 컨시어지</h1>
			<nav>
				<router-link to="/">홈</router-link>
				<router-link to="/amenity">어메니티</router-link>
				<router-link to="/housekeeping">객실정비</router-link>
				<router-link to="/late-checkout">레이트체크아웃</router-link>
			</nav>
		</header>
		<main class="app-body">
			<router-view />
		</main>
	</div>
</template>

<style scoped>
.app-shell { display: flex; flex-direction: column; height: 100%; }
.app-header {
	background: #1a3a6e;
	color: #fff;
	padding: 16px 24px;
	display: flex;
	align-items: center;
	gap: 24px;
	flex-wrap: wrap;
}
.app-header h1 { font-size: 20px; }
.app-header nav { display: flex; gap: 8px; flex-wrap: wrap; }
.app-header nav a {
	color: #cfd8dc;
	text-decoration: none;
	padding: 8px 14px;
	border-radius: 6px;
	font-size: 15px;
}
.app-header nav a.router-link-active {
	background: rgba(255,255,255,0.18);
	color: #fff;
}
.app-body { flex: 1; padding: 24px; overflow: auto; }
</style>
