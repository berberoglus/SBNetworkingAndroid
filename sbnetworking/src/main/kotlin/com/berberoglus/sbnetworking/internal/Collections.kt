package com.berberoglus.sbnetworking.internal

/** The iOS `Collection.nilWhenEmpty`: returns the receiver, or null when empty. */
internal fun <T> Collection<T>.nullIfEmpty(): Collection<T>? = ifEmpty { null }

internal fun <K, V> Map<K, V>.nullIfEmpty(): Map<K, V>? = ifEmpty { null }
