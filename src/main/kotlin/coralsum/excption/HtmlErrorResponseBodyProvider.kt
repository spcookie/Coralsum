package coralsum.excption

import io.micronaut.context.LocalizedMessageSource
import io.micronaut.context.annotation.Primary
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.server.exceptions.response.ErrorContext
import io.micronaut.http.server.exceptions.response.HtmlErrorResponseBodyProvider
import io.micronaut.http.server.util.locale.HttpLocaleResolver
import jakarta.inject.Singleton
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Singleton
@Primary
class HtmlErrorResponseBodyProvider(
    private val lms: LocalizedMessageSource,
    private val localeResolver: HttpLocaleResolver,
) : HtmlErrorResponseBodyProvider {
    override fun contentType(): String = MediaType.TEXT_HTML

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun body(errorContext: ErrorContext, response: HttpResponse<*>): String {
        val status = response.status.code
        val reason = response.status.reason
        val now = ZonedDateTime.now().format(formatter)
        val desc = lms.getMessage("error.page.description").orElse("Error occurred.")
        val backHome = lms.getMessage("error.page.back_home").orElse("Back to Home")
        val backPrev = lms.getMessage("error.page.back_prev").orElse("Back")
        val timeLabel = lms.getMessage("error.page.time_label").orElse("Time")
        val request = errorContext.request
        val langTag =
            request?.let { localeResolver.resolve(it).orElse(java.util.Locale.ENGLISH).toLanguageTag() } ?: "en"
        return """
        <!DOCTYPE html>
        <html lang="$langTag">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Coralsum · $status $reason</title>
        <link rel="icon" href="/favicon.svg">
        <style>
        :root{--bg:#ffffff;--text:#0f172a;--muted:#64748b;--card:#f8fafc;--border:#e2e8f0;--accent:#2563eb}
        @media (prefers-color-scheme: dark){
        :root{--bg:#0b0f1a;--text:#e5e7eb;--muted:#94a3b8;--card:#111827;--border:#1f2937;--accent:#60a5fa}
        }
        *{box-sizing:border-box}
        html,body{height:100%}
        body{margin:0;background:var(--bg);color:var(--text);font-family:ui-sans-serif,system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Cantarell,Noto Sans,sans-serif;display:flex;align-items:center;justify-content:center}
        .container{max-width:640px;padding:32px;border:1px solid var(--border);background:var(--card);border-radius:16px;box-shadow:0 10px 30px rgba(0,0,0,.08)}
        .header{display:flex;align-items:center;gap:12px;margin-bottom:16px}
        .badge{display:inline-flex;align-items:center;padding:6px 10px;border-radius:999px;background:rgba(99,102,241,.12);color:var(--accent);font-weight:600;font-size:12px}
        .title{font-size:22px;font-weight:700;margin:0}
        .desc{margin:8px 0 16px;color:var(--muted);font-size:14px;line-height:1.6}
        .meta{display:flex;gap:16px;flex-wrap:wrap;color:var(--muted);font-size:12px}
        .actions{margin-top:20px;display:flex;gap:12px}
        .btn{display:inline-flex;align-items:center;justify-content:center;height:36px;padding:0 14px;border-radius:10px;border:1px solid var(--border);background:transparent;color:var(--text);text-decoration:none;font-weight:600}
        .btn.primary{background:var(--accent);color:#fff;border-color:transparent}
        .icon{width:20px;height:20px}
        </style>
        </head>
        <body>
        <div class="container" role="alert">
        <div class="header">
        <svg class="icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/><path d="M12 7v6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="12" cy="17" r="1.5" fill="currentColor"/></svg>
        <span class="badge">$status</span>
        <h1 class="title">$reason</h1>
        </div>
        <p class="desc">$desc</p>
        <div class="meta">
        <span>$timeLabel：$now</span>
        </div>
        <div class="actions">
        <a class="btn primary" href="/">$backHome</a>
        <a class="btn" href="javascript:history.back()">$backPrev</a>
        </div>
        </div>
        </body>
        </html>
        """.trimIndent()
    }
}

