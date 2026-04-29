import { ref } from 'vue';

/**
 * 어드민 표 헤더 클릭 정렬 공통 훅. 한 번 클릭 → asc, 같은 컬럼 다시 → desc, 다른 컬럼 → 그 컬럼 asc.
 *
 * 사용:
 *   const sort = useSortableTable('createdAt', 'desc');
 *   const sortedRows = computed(() => sort.applySort(rows.value));
 *
 *   <th class="sortable" @click="sort.sortBy('foo')">Foo <span>{{ sort.sortIcon('foo') }}</span></th>
 *   <tr v-for="r in sortedRows" :key="r.id">
 */
export function useSortableTable(initialKey = null, initialDir = 'asc') {
	const sortKey = ref(initialKey);
	const sortDir = ref(initialDir);

	function sortBy(key) {
		if (sortKey.value === key) {
			sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc';
		} else {
			sortKey.value = key;
			sortDir.value = 'asc';
		}
	}

	function sortIcon(key) {
		if (sortKey.value !== key) return '';
		return sortDir.value === 'asc' ? '▲' : '▼';
	}

	function applySort(arr) {
		if (!sortKey.value || !Array.isArray(arr)) return arr || [];
		const key = sortKey.value;
		const dir = sortDir.value === 'asc' ? 1 : -1;
		return [...arr].sort((a, b) => {
			const va = a == null ? null : a[key];
			const vb = b == null ? null : b[key];
			if (va == null && vb == null) return 0;
			if (va == null) return 1;
			if (vb == null) return -1;
			if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir;
			// 날짜/숫자 String 도 lexicographic 으로 일관된 순서 (ISO 형식 가정)
			const sa = String(va).toLowerCase();
			const sb = String(vb).toLowerCase();
			if (sa < sb) return -1 * dir;
			if (sa > sb) return 1 * dir;
			return 0;
		});
	}

	return { sortKey, sortDir, sortBy, sortIcon, applySort };
}
