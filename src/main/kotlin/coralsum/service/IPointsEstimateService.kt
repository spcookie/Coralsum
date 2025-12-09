package coralsum.service

import coralsum.common.request.EstimatePointsReq
import coralsum.common.response.EstimatePointsResp

interface IPointsEstimateService {
    suspend fun estimate(req: EstimatePointsReq): EstimatePointsResp
}
