# 🪸 Coralsum AI 图片生成

![Micronaut](https://img.shields.io/badge/Micronaut-4.x-222?logo=micronaut) ![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin) ![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk) ![Vue3](https://img.shields.io/badge/Vue-3-42b883?logo=vue.js) ![Vite](https://img.shields.io/badge/Vite-5-646CFF?logo=vite)

> 🚀 Micronaut + Kotlin 后端，Vue 3 + Vite 前端；以 NanoBananaPro（Gemini 3 Pro Image Preview）为引擎，面向文本到图像与图片增强。

---

## ✨ 项目简介
- 轻量前后端一体：后端 Micronaut + Kotlin；前端 Vue 3 + Vite。
- 模型引擎：NanoBananaPro（Gemini 3 Pro Image Preview）。
- 支持多候选生成、可选 Upscayl 增强与直链预览。

## 🛠 技术栈
- 后端：Micronaut、Kotlin、R2DBC、Flyway、H2、JobRunr、Caffeine
- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router、TailwindCSS、Naive UI

## ⚙️ 环境准备
- JDK 17+
- Node.js 18+
- npm 9+

## ▶️ 本地启动
- 后端：
```bash
./gradlew run
```
- 前端：
```bash
cd ui
npm i
npm run dev
```

## 🛡️ 配置说明（.env）
- 在项目根目录创建 `.env`：
```bash
# 模型引擎
GOOGLE_GEMINI_API_KEY=<your-gemini-key>

# 直链与预览域名（如 https://your.domain.example）
CLOUDFLARE_HOST=<your-host>

# S3 兼容端点
OSS_ENDPOINT=<https://s3.your-endpoint.example>
```

环境变量建议：
```bash
export oss.endpoint="https://s3.your-endpoint.example"
```

## 🗂 目录结构
```
Coralsum/
├─ ui/                      # 前端 (Vue + Vite)
│  ├─ src/                  # 页面与组件
│  └─ vite.config.ts        # 开发与构建配置
├─ src/main/kotlin/         # 后端 Kotlin
│  └─ coralsum/             # controller / service / repository
├─ src/main/resources/
│  ├─ application.yml       # 应用配置
│  └─ public/               # 前端构建产物托管
├─ models/                  # 模型
├─ libs/                    # 本地二进制
└─ build.gradle.kts         # 构建配置
```
