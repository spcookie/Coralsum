package coralsum.service

import coralsum.common.request.CreatePointsKeyConfigReq
import coralsum.common.request.GeneratePointsKeysReq
import coralsum.common.request.RedeemPointsResp
import coralsum.common.request.ToggleKeysReq
import coralsum.entity.PointsKey
import coralsum.entity.PointsKeyConfig

interface IPointsKeyService {
    suspend fun createConfig(req: CreatePointsKeyConfigReq, operator: String?): PointsKeyConfig
    suspend fun listConfigs(): List<PointsKeyConfig>
    suspend fun generateKeys(req: GeneratePointsKeysReq, operator: String?): List<PointsKey>
    suspend fun listKeys(): List<PointsKey>
    suspend fun toggleKeys(req: ToggleKeysReq)
    suspend fun redeem(uid: String, ip: String?, keyCode: String): RedeemPointsResp
}