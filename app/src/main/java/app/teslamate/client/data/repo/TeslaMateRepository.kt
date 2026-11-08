package app.teslamate.client.data.repo

import app.teslamate.client.data.api.AppException
import app.teslamate.client.data.api.ErrorEnvelopeDto
import app.teslamate.client.data.api.TeslaMateApi
import app.teslamate.client.data.api.toCars
import app.teslamate.client.data.api.toChargeDetail
import app.teslamate.client.data.api.toChargePage
import app.teslamate.client.data.api.toDriveDetail
import app.teslamate.client.data.api.toDrivePage
import app.teslamate.client.data.api.toStatus
import app.teslamate.client.data.settings.SettingsRepository
import app.teslamate.client.domain.Car
import app.teslamate.client.domain.CarStatus
import app.teslamate.client.domain.ChargeDetail
import app.teslamate.client.domain.ChargePage
import app.teslamate.client.domain.DriveDetail
import app.teslamate.client.domain.DrivePage
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

@Singleton
class TeslaMateRepository @Inject constructor(
    private val api: TeslaMateApi,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun getCars(): List<Car> = call { api.getCars().toCars() }

    suspend fun getStatus(carId: Int): CarStatus = call { api.getStatus(carId).toStatus() }

    suspend fun getDrives(carId: Int, page: Int, show: Int = PAGE_SIZE): DrivePage =
        call { api.getDrives(carId, page, show).toDrivePage() }

    suspend fun getDrive(carId: Int, driveId: Int): DriveDetail =
        call { api.getDrive(carId, driveId).toDriveDetail() }

    suspend fun getCharges(carId: Int, page: Int, show: Int = PAGE_SIZE): ChargePage =
        call { api.getCharges(carId, page, show).toChargePage() }

    suspend fun getCharge(carId: Int, chargeId: Int): ChargeDetail =
        call { api.getCharge(carId, chargeId).toChargeDetail() }

    private suspend fun <T> call(block: suspend () -> T): T {
        val settings = settingsRepository.snapshot()
        if (settings.baseUrl.isBlank()) throw AppException.NotConfigured()
        try {
            return block()
        } catch (e: AppException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val message = body?.let { raw ->
                runCatching {
                    app.teslamate.client.data.api.ApiJson.json.decodeFromString<ErrorEnvelopeDto>(raw)
                }.getOrNull()?.let { it.error ?: it.message }
            }
            throw AppException.Http(e.code(), message)
        } catch (e: SerializationException) {
            throw AppException.Parse(e)
        } catch (e: IOException) {
            throw AppException.Network(e)
        }
    }

    companion object {
        const val PAGE_SIZE = 40
    }
}
