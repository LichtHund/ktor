/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.resources

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

/**
 * Registers a route [body] for a resource defined by class [T].
 *
 * Class [T] **must** be annotated with [Resource].
 */
public inline fun <reified T : Any> Route.resource(noinline body: Route.() -> Unit): Route {
    val resources = application.feature(Resources)
    val serializer = serializer<T>()
    val path = resources.resourcesFormat.encodeToPathPattern(serializer)
    val queryParameters = resources.resourcesFormat.encodeToQueryParameters(serializer)
    val route = createRouteFromPath(path)

    return queryParameters.fold(route) { entry, query ->
        val selector = if (query.isOptional) {
            OptionalParameterRouteSelector(query.name)
        } else {
            ParameterRouteSelector(query.name)
        }
        entry.createChild(selector)
    }.apply(body)
}

/**
 * Registers a typed handler [body] for a `GET` resource defined by class [T].
 *
 * Class [T] **must** be annotated with [Resource].
 *
 * @param body receives an instance of typed resource [T] as first parameter.
 */
public inline fun <reified T : Any> Route.get(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
): Route {
    return resource<T> {
        method(HttpMethod.Get) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `OPTIONS` resource defined by class [T].
 *
 * Class [T] **must** be annotated with [Resource].
 *
 * @param body receives an instance of typed resource [T] as first parameter.
 */
public inline fun <reified T : Any> Route.options(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
): Route {
    return resource<T> {
        method(HttpMethod.Options) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `HEAD` resource defined by class [T].
 *
 * Class [T] **must** be annotated with [Resource].
 *
 * @param body receives an instance of typed resource [T] as first parameter.
 */
public inline fun <reified T : Any> Route.head(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
): Route {
    return resource<T> {
        method(HttpMethod.Head) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `POST` resource defined by class [T].
 *
 * Class [T] **must** be annotated with [Resource].
 *
 * @param body receives an instance of typed resource [T] as first parameter.
 */
public inline fun <reified T : Any> Route.post(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
): Route {
    return resource<T> {
        method(HttpMethod.Post) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `PUT` resource defined by class [T].
 *
 * Class [T] **must** be annotated with [Resource].
 *
 * @param body receives an instance of typed resource [T] as first parameter.
 */
public inline fun <reified T : Any> Route.put(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
): Route {
    return resource<T> {
        method(HttpMethod.Put) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `DELETE` resource defined by class [T].
 *
 * Class [T] **must** be annotated with [Resource].
 *
 * @param body receives an instance of typed resource [T] as first parameter.
 */
public inline fun <reified T : Any> Route.delete(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
): Route {
    return resource<T> {
        method(HttpMethod.Delete) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `PATCH` resource defined by class [T].
 *
 * Class [T] **must** be annotated with [Resource].
 *
 * @param body receives an instance of typed resource [T] as first parameter.
 */
public inline fun <reified T : Any> Route.patch(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
): Route {
    return resource<T> {
        method(HttpMethod.Patch) {
            handle(body)
        }
    }
}

/**
 * Registers a handler [body] for a resource defined by class [dataClass].
 *
 * @param body receives an instance of typed resource [dataClass] as first parameter.
 */
public inline fun <reified T : Any> Route.handle(
    crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
) {
    intercept(ApplicationCallPipeline.Features) {
        val resources = application.feature(Resources)
        try {
            val resource = resources.resourcesFormat.decodeFromParameters<T>(serializer(), call.parameters)
            call.attributes.put(ResourceInstanceKey, resource)
        } catch (cause: Throwable) {
            throw BadRequestException("Can't transform call to resource", cause)
        }
    }

    handle {
        @Suppress("UNCHECKED_CAST")
        val resource = call.attributes[ResourceInstanceKey] as T
        body(resource)
    }
}

@PublishedApi
internal val ResourceInstanceKey: AttributeKey<Any> = AttributeKey("ResourceInstance")
