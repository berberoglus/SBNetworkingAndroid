package com.berberoglus.sbnetworking

/**
 * The iOS `ModelConvertible`/`ResponseProtocol.toModel()` analog. Optional marker — the preferred
 * approach (Networking_Domain_Rules.md §5.2) is free extension functions in `data/mapper`.
 */
interface ModelConvertible<M> {
    fun toDomain(): M
}
