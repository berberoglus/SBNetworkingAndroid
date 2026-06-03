package com.berberoglus.sbnetworking.internal

/** Returns the receiver, or null when it is empty. */
internal fun <T> Collection<T>.nullIfEmpty(): Collection<T>? = ifEmpty { null }

internal fun <K, V> Map<K, V>.nullIfEmpty(): Map<K, V>? = ifEmpty { null }
