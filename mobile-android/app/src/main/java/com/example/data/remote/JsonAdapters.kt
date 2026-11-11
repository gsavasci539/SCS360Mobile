package com.example.data.remote

import com.squareup.moshi.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Robust JSON Adapter Factory that can parse lists whether they come as:
 * - A raw array: [ {...}, {...} ]
 * - A paginated object: { "items": [ {...} ], "total": 10 }
 * - A wrapped object: { "data": [ {...} ], "status": "success" }
 * - A results object: { "results": [ {...} ] }
 * - An entity-named object: { "students": [ {...} ] }, { "classes": [ {...} ] }, etc.
 */
class FlexibleListJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val rawType = Types.getRawType(type)
        if (rawType != List::class.java && rawType != java.util.List::class.java) {
            return null
        }

        val elementType = (type as? ParameterizedType)?.actualTypeArguments?.getOrNull(0) ?: Any::class.java
        val elementAdapter = moshi.adapter<Any>(elementType)

        return object : JsonAdapter<List<Any>>() {
            override fun fromJson(reader: JsonReader): List<Any> {
                val list = mutableListOf<Any>()
                when (reader.peek()) {
                    JsonReader.Token.BEGIN_ARRAY -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            try {
                                elementAdapter.fromJson(reader)?.let { list.add(it) }
                            } catch (e: Exception) {
                                // Skip malformed item in array to prevent whole list from failing
                                try { reader.skipValue() } catch (_: Exception) {}
                            }
                        }
                        reader.endArray()
                    }
                    JsonReader.Token.BEGIN_OBJECT -> {
                        reader.beginObject()
                        var foundArray = false
                        while (reader.hasNext()) {
                            val name = reader.nextName().lowercase()
                            if (!foundArray && reader.peek() == JsonReader.Token.BEGIN_ARRAY &&
                                (name in listOf("items", "data", "results", "records", "students", "classes", "branches", "invoices", "debts", "payments", "announcements", "users", "roles", "resources", "daily_reports", "list"))
                            ) {
                                foundArray = true
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    try {
                                        elementAdapter.fromJson(reader)?.let { list.add(it) }
                                    } catch (e: Exception) {
                                        try { reader.skipValue() } catch (_: Exception) {}
                                    }
                                }
                                reader.endArray()
                            } else {
                                reader.skipValue()
                            }
                        }
                        reader.endObject()
                    }
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Unit>()
                    }
                    else -> {
                        reader.skipValue()
                    }
                }
                return list
            }

            override fun toJson(writer: JsonWriter, value: List<Any>?) {
                if (value == null) {
                    writer.nullValue()
                    return
                }
                writer.beginArray()
                for (item in value) {
                    elementAdapter.toJson(writer, item)
                }
                writer.endArray()
            }
        }
    }
}

/**
 * Coercing adapter for String fields to gracefully handle numbers, booleans, and nulls
 */
class StringCoercingAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): String {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> {
                val numStr = reader.nextString()
                numStr
            }
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                ""
            }
            else -> {
                reader.skipValue()
                ""
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: String?) {
        writer.value(value ?: "")
    }
}

/**
 * Coercing adapter for Double fields to handle strings, numbers, and nulls
 */
class DoubleCoercingAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Double {
        return when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextDouble()
            JsonReader.Token.STRING -> {
                val str = reader.nextString().replace(",", ".").trim()
                str.toDoubleOrNull() ?: 0.0
            }
            JsonReader.Token.BOOLEAN -> if (reader.nextBoolean()) 1.0 else 0.0
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                0.0
            }
            else -> {
                reader.skipValue()
                0.0
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Double?) {
        writer.value(value ?: 0.0)
    }
}

/**
 * Coercing adapter for Int fields to handle strings, numbers, and nulls
 */
class IntCoercingAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Int {
        return when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextInt()
            JsonReader.Token.STRING -> {
                val str = reader.nextString().replace(",", ".").trim()
                str.toDoubleOrNull()?.toInt() ?: 0
            }
            JsonReader.Token.BOOLEAN -> if (reader.nextBoolean()) 1 else 0
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                0
            }
            else -> {
                reader.skipValue()
                0
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Int?) {
        writer.value(value ?: 0)
    }
}
