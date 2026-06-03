package com.berberoglus.sbnetworking

/** Marker for a typed endpoint that decodes into [R]. Optional; Retrofit `*Api` is preferred. */
interface Endpoint<R : Any> {
    fun toSpec(): EndpointSpec
}

/** The iOS `EndpointConvertible`: a feature request model that builds its own [EndpointSpec]. */
interface EndpointConvertible {
    fun toEndpoint(): EndpointSpec
}
