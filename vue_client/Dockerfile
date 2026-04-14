# syntax=docker/dockerfile:1.6
# ───────── build stage ─────────
FROM node:20-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json* ./
RUN --mount=type=cache,target=/root/.npm \
	npm install --no-audit --no-fund

COPY . .
# 빌드 타임에 상대 /api 를 쓰도록 VITE_API_BASE 를 nginx 경로에 고정 (런타임엔 nginx 프록시가 api 컨테이너로 포워딩)
ENV VITE_API_BASE=/api
RUN npm run build

# ───────── runtime stage (nginx) ─────────
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
	CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
