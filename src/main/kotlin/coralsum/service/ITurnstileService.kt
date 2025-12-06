package coralsum.service

interface ITurnstileService {
    suspend fun validate(token: String?, remoteIp: String?): Boolean
}

