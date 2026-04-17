<template>
  <Transition name="slide-down">
    <div v-if="!online" class="offline-banner">
      {{ t('network.offline') }}
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { t } from '../i18n/ui.js';

const online = ref(navigator.onLine);

function goOnline() { online.value = true; }
function goOffline() { online.value = false; }

onMounted(() => {
  window.addEventListener('online', goOnline);
  window.addEventListener('offline', goOffline);
});
onUnmounted(() => {
  window.removeEventListener('online', goOnline);
  window.removeEventListener('offline', goOffline);
});
</script>

<style scoped>
.offline-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  background: #dc2626;
  color: #fff;
  text-align: center;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 600;
}
.slide-down-enter-active, .slide-down-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.slide-down-enter-from, .slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
