package coralsum.service.impl

import coralsum.common.enums.MembershipTier
import coralsum.common.enums.UserSource
import coralsum.common.response.ProfileResponse
import coralsum.entity.NickNameBitmap
import coralsum.repository.NickNameBitmapRepository
import coralsum.repository.OpenUserRepository
import coralsum.repository.OutletUserRepository
import coralsum.repository.UserPointsRepository
import coralsum.service.IUserPointsService
import coralsum.service.IWebUserService
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.math.RoundingMode

@Singleton
class WebUserServiceImpl(
    private val openUserRepository: OpenUserRepository,
    private val outletUserRepository: OutletUserRepository,
    private val userPointsRepository: UserPointsRepository,
    private val nickNameBitmapRepository: NickNameBitmapRepository,
    private val userPointsService: IUserPointsService,
) : IWebUserService {
    override suspend fun profile(uid: String): ProfileResponse? {
        val openUser = openUserRepository.findByUid(uid) ?: return null
        val outletWeb = outletUserRepository.findByOpenUserIdAndUserSource(openUser.id!!, UserSource.WEB)
        var userPoints = userPointsRepository.findByOpenUserId(openUser.id)
        if (userPoints != null) {
            userPoints = userPointsService.reconcileTier(openUser.id!!)
        }
        val finalNickBase = outletWeb?.nickName ?: ""
        val finalNickTag = outletWeb?.nickTag
        return ProfileResponse(
            uid = openUser.uid ?: "",
            permissions = openUser.assignRoleList() ?: emptyList(),
            nickName = finalNickBase,
            nickTag = finalNickTag,
            sourceCode = outletWeb?.sourceCode ?: "",
            permanentPoints = ((userPoints?.permanentPoints ?: BigDecimal.ZERO).setScale(
                2,
                RoundingMode.HALF_UP
            ) * 100.toBigDecimal()).toInt(),
            subscribePoints = ((userPoints?.subscribePoints ?: BigDecimal.ZERO).setScale(
                2,
                RoundingMode.HALF_UP
            ) * 100.toBigDecimal()).toInt(),
            tier = userPoints?.tier ?: MembershipTier.FREE
        )
    }

    @Transactional(rollbackFor = [Exception::class])
    override suspend fun updateNickName(uid: String, nickName: String): Boolean {
        val openUser = openUserRepository.findByUid(uid) ?: return false
        val outletWeb =
            outletUserRepository.findByOpenUserIdAndUserSource(openUser.id!!, UserSource.WEB) ?: return false
        val sanitized = nickName.trim().substringBefore('#')
        if (sanitized.isEmpty()) return false
        val oldBase = outletWeb.nickName ?: ""
        val oldTag = outletWeb.nickTag
        if (sanitized == oldBase && oldTag != null) {
            val updated = outletWeb.copy(nickName = sanitized)
            outletUserRepository.update(updated)
            return true
        }
        fun zeroBitmap(): ByteArray = ByteArray(1250) { 0 }
        fun isBitSet(arr: ByteArray, tag: Int): Boolean {
            if (tag < 1 || tag > 9999) return false
            val idx = (tag - 1) / 8
            val bit = (tag - 1) % 8
            return (arr[idx].toInt() and (1 shl bit)) != 0
        }

        fun setBit(arr: ByteArray, tag: Int): ByteArray {
            val idx = (tag - 1) / 8
            val bit = (tag - 1) % 8
            val b = arr[idx].toInt() or (1 shl bit)
            arr[idx] = b.toByte()
            return arr
        }

        fun clearBit(arr: ByteArray, tag: Int): ByteArray {
            val idx = (tag - 1) / 8
            val bit = (tag - 1) % 8
            val b = arr[idx].toInt() and (255 - (1 shl bit))
            arr[idx] = b.toByte()
            return arr
        }

        fun findMinFree(arr: ByteArray): Int? {
            var t = 1
            while (t <= 9999) {
                if (!isBitSet(arr, t)) return t
                t++
            }
            return null
        }

        suspend fun getOrCreateBitmap(base: String): NickNameBitmap {
            val existing = nickNameBitmapRepository.findByBaseNameAndUserSource(base, UserSource.WEB)
            if (existing != null) return existing
            return try {
                nickNameBitmapRepository.save(
                    NickNameBitmap(userSource = UserSource.WEB, baseName = base, bitmap = zeroBitmap())
                )
            } catch (_: Exception) {
                nickNameBitmapRepository.findByBaseNameAndUserSource(base, UserSource.WEB)!!
            }
        }

        suspend fun reconcileBitmap(base: String) {
            val bmp = getOrCreateBitmap(base)
            val same = outletUserRepository.findAllByNickNameAndUserSource(base, UserSource.WEB)
            var arr = bmp.bitmap.copyOf()
            for (ou in same) {
                val tg = ou.nickTag
                if (tg != null && tg in 1..9999 && !isBitSet(arr, tg)) {
                    arr = setBit(arr, tg)
                }
            }
            if (!arr.contentEquals(bmp.bitmap)) {
                var ok = false
                var tries = 0
                while (!ok && tries < 5) {
                    try {
                        nickNameBitmapRepository.update(bmp.copy(bitmap = arr))
                        ok = true
                    } catch (_: Exception) {
                        tries++
                        val latest = nickNameBitmapRepository.findByBaseNameAndUserSource(base, UserSource.WEB) ?: break
                        var arr2 = latest.bitmap.copyOf()
                        for (ou in same) {
                            val tg = ou.nickTag
                            if (tg != null && tg in 1..9999 && !isBitSet(arr2, tg)) {
                                arr2 = setBit(arr2, tg)
                            }
                        }
                        arr = arr2
                    }
                }
            }
        }
        reconcileBitmap(sanitized)
        var allocated: Int? = null
        var attempts = 0
        while (attempts < 5) {
            val bmp = getOrCreateBitmap(sanitized)
            val arr = bmp.bitmap.copyOf()
            val candidate = findMinFree(arr) ?: return false
            val newArr = setBit(arr, candidate)
            try {
                nickNameBitmapRepository.update(bmp.copy(bitmap = newArr))
                allocated = candidate
                break
            } catch (_: Exception) {
                attempts++
            }
        }
        if (allocated == null) return false
        if (!oldBase.isBlank() && oldTag != null) {
            val bmp = getOrCreateBitmap(oldBase)
            var ok = false
            var tries = 0
            while (!ok && tries < 5) {
                val arr = bmp.bitmap.copyOf()
                val newArr = clearBit(arr, oldTag)
                try {
                    nickNameBitmapRepository.update(bmp.copy(bitmap = newArr))
                    ok = true
                } catch (_: Exception) {
                    tries++
                }
            }
        }
        val updated = outletWeb.copy(nickName = sanitized, nickTag = allocated)
        outletUserRepository.update(updated)
        return true
    }
}
