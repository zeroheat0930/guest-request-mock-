<template>
  <div class="spinner-wrap">
    <div class="spinner"></div>
    <p v-if="text" class="spinner-text">{{ text }}</p>
  </div>
</template>
<script setup>
defineProps({ text: { type: String, default: '' } });
</script>
<style scoped>
.spinner-wrap { display: flex; flex-direction: column; align-items: center; padding: 32px 0; }
.spinner {
  width: 32px; height: 32px;
  border: 3px solid #e2e8f0; border-top-color: var(--c-brand-500, #2b6cb0);
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.spinner-text { margin-top: 12px; color: #718096; font-size: 14px; }
</style>
