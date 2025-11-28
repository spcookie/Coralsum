package coralsum.service

import coralsum.common.dto.PageResp
import coralsum.common.request.CreatePointsKeyConfigReq
import coralsum.common.request.GeneratePointsKeysReq
import coralsum.common.request.RedeemPointsResp
import coralsum.common.request.ToggleKeysReq
import coralsum.entity.PointsKey
import coralsum.entity.PointsKeyConfig

interface IPointsKeyService {
    suspend fun createConfig(req: CreatePointsKeyConfigReq, operator: String?): PointsKeyConfig
    suspend fun listConfigs(): List<PointsKeyConfig>
    suspend fun listConfigsPaged(
        page: Int,
        size: Int,
        name: String?,
        sortBy: String?,
        order: String?,
    ): PageResp<PointsKeyConfig>
    suspend fun generateKeys(req: GeneratePointsKeysReq, operator: String?): List<PointsKey>
    suspend fun listKeys(): List<PointsKey>
    suspend fun listKeysPaged(page: Int, size: Int, key: String?, order: String?): PageResp<PointsKey>
    suspend fun toggleKeys(req: ToggleKeysReq)
    suspend fun redeem(uid: String, ip: String?, keyCode: String): RedeemPointsResp
}