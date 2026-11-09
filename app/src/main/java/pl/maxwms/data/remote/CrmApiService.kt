package pl.maxwms.data.remote

import pl.maxwms.data.remote.dto.MovementsRequest
import pl.maxwms.data.remote.dto.MovementsResponse
import pl.maxwms.data.remote.dto.PartsResponse
import pl.maxwms.data.remote.dto.PartDto
import pl.maxwms.data.remote.dto.VerifyLicenseRequest
import pl.maxwms.data.remote.dto.VerifyLicenseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import okhttp3.ResponseBody

interface CrmApiService {
    @GET("api/wms/parts")
    suspend fun getParts(): PartsResponse

    @POST("api/wms/movements")
    suspend fun postMovements(@Body body: MovementsRequest): MovementsResponse

    @POST("api/wms/verify-license")
    suspend fun verifyLicense(@Body body: VerifyLicenseRequest): VerifyLicenseResponse

    @POST("api/wms/parts")
    suspend fun createPart(@Body body: PartDto): Response<ResponseBody>

    @PUT("api/wms/parts/{id}")
    suspend fun updatePart(@Path("id") id: String, @Body body: PartDto): Response<ResponseBody>
}
