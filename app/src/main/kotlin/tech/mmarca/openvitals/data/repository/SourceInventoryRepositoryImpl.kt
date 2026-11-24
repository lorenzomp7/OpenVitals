package tech.mmarca.openvitals.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.contract.SourceInventoryRepository
import tech.mmarca.openvitals.domain.model.SourceInventory
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

@Singleton
class SourceInventoryRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
) : SourceInventoryRepository {
    override suspend fun loadSourceInventory(
        candidates: Map<String, Set<String>>,
        windowDays: Int,
    ): SourceInventory = hc.sourceInventoryReader.read(
        grantedPermissions = hc.grantedPermissions(),
        candidates = candidates,
        windowDays = windowDays,
    )
}
