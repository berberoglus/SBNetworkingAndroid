package com.berberoglus.sbnetworking

/**
 * Optional marker for a transport DTO that converts itself into a domain model. The preferred
 * approach (Networking_Domain_Rules.md §5.2) is free extension functions in `data/mapper`.
 */
interface ModelConvertible<M> {
    fun toDomain(): M
}
