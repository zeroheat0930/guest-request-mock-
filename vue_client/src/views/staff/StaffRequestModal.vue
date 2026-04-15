<template>
  <teleport to="body">
    <div v-if="open" class="srm-backdrop" @click.self="onClose">
      <div class="srm-card" role="dialog" aria-modal="true" aria-labelledby="srm-title">
        <h2 id="srm-title" class="srm-title">새 요청 만들기</h2>

        <form @submit.prevent="submit" class="srm-form">
          <!-- 대상 부서 (필수) -->
          <label class="srm-label">
            대상 부서 <span class="srm-required">*</span>
            <select v-model="form.toDeptCd" class="srm-input" required>
              <option value="" disabled>부서 선택</option>
              <option value="HK">HK — 하우스키핑</option>
              <option value="FR">FR — 프론트</option>
              <option value="ENG">ENG — 엔지니어링</option>
              <option value="FB">FB — F&amp;B</option>
            </select>
          </label>

          <!-- 특정 직원 (선택) -->
          <label class="srm-label">
            특정 직원 staffId <span class="srm-hint">(선택)</span>
            <input v-model="form.toAssigneeId" class="srm-input" type="text" placeholder="예: S001" />
          </label>

          <!-- 제목 (필수) -->
          <label class="srm-label">
            제목 <span class="srm-required">*</span>
            <input v-model="form.title" class="srm-input" type="text" placeholder="요청 제목" required />
          </label>

          <!-- 메모 (선택) -->
          <label class="srm-label">
            메모 <span class="srm-hint">(선택)</span>
            <textarea v-model="form.memo" class="srm-input srm-textarea" rows="3" placeholder="상세 내용"></textarea>
          </label>

          <!-- 객실 번호 (선택) -->
          <label class="srm-label">
            객실 번호 <span class="srm-hint">(선택)</span>
            <input v-model="form.roomNo" class="srm-input" type="text" placeholder="예: 1205" />
          </label>

          <p v-if="errorMsg" class="srm-error">{{ errorMsg }}</p>

          <div class="srm-actions">
            <button type="button" class="srm-btn srm-btn-cancel" @click="onClose" :disabled="loading">취소</button>
            <button type="submit" class="srm-btn srm-btn-submit" :disabled="loading">
              {{ loading ? '전송 중…' : '요청 전송' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { createCcsTask } from '@/api/client.js';

const props = defineProps({
  open:      { type: Boolean, required: true },
  onClose:   { type: Function, required: true },
  onSuccess: { type: Function, required: true },
});

const loading  = ref(false);
const errorMsg = ref('');

const form = reactive({
  toDeptCd:    '',
  toAssigneeId: '',
  title:        '',
  memo:         '',
  roomNo:       '',
});

async function submit() {
  errorMsg.value = '';

  if (!form.toDeptCd || !form.title.trim()) {
    errorMsg.value = '대상 부서와 제목은 필수입니다.';
    return;
  }

  loading.value = true;
  try {
    await createCcsTask({
      toDeptCd:     form.toDeptCd,
      toAssigneeId: form.toAssigneeId || null,
      title:        form.title.trim(),
      memo:         form.memo.trim() || null,
      roomNo:       form.roomNo.trim() || null,
    });
    props.onSuccess();
    props.onClose();
    Object.assign(form, { toDeptCd: '', toAssigneeId: '', title: '', memo: '', roomNo: '' });
  } catch (err) {
    errorMsg.value = err?.resMsg || '요청 전송에 실패했습니다.';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.srm-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.srm-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  width: 100%;
  max-width: 440px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}

.srm-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0 0 20px;
  color: #1a1a2e;
}

.srm-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.srm-label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.875rem;
  color: #444;
  font-weight: 500;
}

.srm-required {
  color: #e53e3e;
}

.srm-hint {
  color: #999;
  font-weight: 400;
}

.srm-input {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: border-color 0.15s;
  outline: none;
}

.srm-input:focus {
  border-color: #4f46e5;
}

.srm-textarea {
  resize: vertical;
  min-height: 72px;
}

.srm-error {
  color: #e53e3e;
  font-size: 0.85rem;
  margin: 0;
}

.srm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

.srm-btn {
  padding: 9px 20px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: opacity 0.15s;
}

.srm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.srm-btn-cancel {
  background: #f3f4f6;
  color: #374151;
}

.srm-btn-cancel:hover:not(:disabled) {
  background: #e5e7eb;
}

.srm-btn-submit {
  background: #4f46e5;
  color: #fff;
}

.srm-btn-submit:hover:not(:disabled) {
  background: #4338ca;
}
</style>
