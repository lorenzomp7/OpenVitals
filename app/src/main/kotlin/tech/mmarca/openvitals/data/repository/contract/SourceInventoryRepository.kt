package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.model.SourceInventory

/** The diagnostics' "what does each source write" table. */
interface SourceInventoryRepository {
    /**
     * For each data type OpenVitals reads, the latest record time per source app over the
     * last [windowDays] days. [candidates] maps a type ("heart rate") to the apps that declare
     * writing it, so one that sent nothing is looked for before it is called silent.
     * A diagnostic, read on demand: about one Health Connect call per type.
     */
    suspend fun loadSourceInventory(candidates: Map<String, Set<String>>, windowDays: Int = 30): SourceInventory
}
