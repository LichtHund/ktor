/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.json.serializer

import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

/**
 * A [JsonSerializer] implemented for kotlinx [Serializable] classes.
 */
public class KotlinxSerializer(
    private val json: Json = DefaultJson
) : JsonSerializer {

    override fun write(data: Any, contentType: ContentType, payloadType: KType?): OutgoingContent {
        @Suppress("UNCHECKED_CAST")
        return TextContent(writeContent(data, payloadType), contentType)
    }

    override fun write(data: Any, contentType: ContentType): OutgoingContent = write(data, contentType, null)

    internal fun writeContent(data: Any, typeOfBody: KType?): String {
        if (typeOfBody != null) try {
            return json.encodeToString(json.serializersModule.serializer(typeOfBody), data)
        } catch (cause: Throwable) {
            println(cause)
        }
        return json.encodeToString(buildSerializer(data, json.serializersModule), data)
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override fun read(type: TypeInfo, body: Input): Any {
        val text = body.readText()
        val deserializationStrategy = json.serializersModule.getContextual(type.type)
        val mapper = deserializationStrategy ?: (type.kotlinType?.let { serializer(it) } ?: type.type.serializer())
        return json.decodeFromString(mapper, text)!!
    }

    public companion object {
        /**
         * Default [Json] configuration for [KotlinxSerializer].
         */
        @Deprecated(
            level = DeprecationLevel.ERROR,
            message = "DefaultJsonConfiguration is deprecated. Consider using DefaultJson instead.",
            replaceWith = ReplaceWith("DefaultJson")
        )
        @Suppress("unused")
        public val DefaultJsonConfiguration: Json = Json {
            isLenient = false
            ignoreUnknownKeys = false
            allowSpecialFloatingPointValues = true
            useArrayPolymorphism = false
        }

        /**
         * Default [Json] configuration for [KotlinxSerializer].
         */
        public val DefaultJson: Json = Json {
            isLenient = false
            ignoreUnknownKeys = false
            allowSpecialFloatingPointValues = true
            useArrayPolymorphism = false
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun buildSerializer(value: Any, module: SerializersModule): KSerializer<Any> = when (value) {
    is JsonElement -> JsonElement.serializer()
    is List<*> -> ListSerializer(value.elementSerializer(module))
    is Array<*> -> value.firstOrNull()?.let { buildSerializer(it, module) } ?: ListSerializer(String.serializer())
    is Set<*> -> SetSerializer(value.elementSerializer(module))
    is Map<*, *> -> {
        val keySerializer = value.keys.elementSerializer(module)
        val valueSerializer = value.values.elementSerializer(module)
        MapSerializer(keySerializer, valueSerializer)
    }
    else -> {
        @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
        module.getContextual(value::class) ?: value::class.serializer()
    }
} as KSerializer<Any>

@OptIn(ExperimentalSerializationApi::class)
private fun Collection<*>.elementSerializer(module: SerializersModule): KSerializer<*> {
    val serializers: List<KSerializer<*>> =
        filterNotNull().map { buildSerializer(it, module) }.distinctBy { it.descriptor.serialName }

    if (serializers.size > 1) {
        error(
            "Serializing collections of different element types is not yet supported. " +
                "Selected serializers: ${serializers.map { it.descriptor.serialName }}"
        )
    }

    val selected = serializers.singleOrNull() ?: String.serializer()

    if (selected.descriptor.isNullable) {
        return selected
    }

    @Suppress("UNCHECKED_CAST")
    selected as KSerializer<Any>

    if (any { it == null }) {
        return selected.nullable
    }

    return selected
}
