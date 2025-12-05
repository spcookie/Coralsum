package coralsum.service

interface ITurnstileService {
    fun validate(token: String?, remoteIp: String?): Boolean
}

