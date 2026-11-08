package app.teslamate.client.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Paths match TeslaMateApi `webserver.go`:
 * GET /api/v1/cars
 * GET /api/v1/cars/:CarID/status
 * GET /api/v1/cars/:CarID/drives?page&show
 * GET /api/v1/cars/:CarID/drives/:DriveID
 * GET /api/v1/cars/:CarID/charges?page&show
 * GET /api/v1/cars/:CarID/charges/:ChargeID
 *
 * Read endpoints return HTTP 200 with `{"error":"..."}` when the query fails.
 * Command and logging endpoints are the ones that require API_TOKEN.
 */
interface TeslaMateApi {
    @GET("api/v1/cars")
    suspend fun getCars(): CarsResponseDto

    @GET("api/v1/cars/{carId}/status")
    suspend fun getStatus(@Path("carId") carId: Int): StatusResponseDto

    @GET("api/v1/cars/{carId}/drives")
    suspend fun getDrives(
        @Path("carId") carId: Int,
        @Query("page") page: Int,
        @Query("show") show: Int,
    ): DrivesResponseDto

    @GET("api/v1/cars/{carId}/drives/{driveId}")
    suspend fun getDrive(
        @Path("carId") carId: Int,
        @Path("driveId") driveId: Int,
    ): DriveDetailResponseDto

    @GET("api/v1/cars/{carId}/charges")
    suspend fun getCharges(
        @Path("carId") carId: Int,
        @Query("page") page: Int,
        @Query("show") show: Int,
    ): ChargesResponseDto

    @GET("api/v1/cars/{carId}/charges/{chargeId}")
    suspend fun getCharge(
        @Path("carId") carId: Int,
        @Path("chargeId") chargeId: Int,
    ): ChargeDetailResponseDto
}
