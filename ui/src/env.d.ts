/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_API_BASE_URL?: string
    readonly VITE_UPLOAD_BASE_URL?: string
    readonly VITE_TURNSTILE_SITEKEY?: string
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}

