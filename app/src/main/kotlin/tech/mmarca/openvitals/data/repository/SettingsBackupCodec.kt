package tech.mmarca.openvitals.data.repository

import org.json.JSONArray
import org.json.JSONObject

/** Versioned, typed settings only. Parse the entire document before opening any preference editor. */
internal object SettingsBackupCodec {
    const val MAX_BYTES = 2 * 1024 * 1024
    private const val FORMAT = "openvitals-settings"
    private const val VERSION = 1

    fun encode(stores: Map<String, Map<String, *>>): String {
        val payload = JSONObject()
        stores.toSortedMap().forEach { (store, values) ->
            val entries = JSONObject()
            values.toSortedMap().forEach entryLoop@ { (key, value) ->
                val type = SettingsBackupSchema.type(store, key) ?: return@entryLoop
                entries.put(key, JSONObject().put("type", type).put("value", if (value is Set<*>) JSONArray(value.sortedBy { it.toString() }) else value))
            }
            payload.put(store, entries)
        }
        return JSONObject().put("format", FORMAT).put("version", VERSION).put("settings", payload).toString(2)
    }

    fun decode(text: String): Map<String, Map<String, Any>> {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val root = JSONObject(text)
        require(root.getString("format") == FORMAT && root.get("version") == VERSION)
        val stores = root.getJSONObject("settings")
        require(stores.length() > 0)
        return stores.keys().asSequence().associateWith { store ->
            require(store in SettingsBackupSchema.stores)
            val entries = stores.getJSONObject(store)
            require(entries.length() <= 4096)
            entries.keys().asSequence().associateWith { key ->
                require(key.length <= 256)
                val type = requireNotNull(SettingsBackupSchema.type(store, key))
                val entry = entries.getJSONObject(key)
                require(entry.getString("type") == type)
                decodeValue(type, entry.get("value"))
            }
        }
    }

    private fun decodeValue(type: String, value: Any): Any = when (type) {
        "Boolean" -> (value as? Boolean) ?: error("Invalid boolean")
        "Int" -> number(value).also { require(it.toLong() in Int.MIN_VALUE..Int.MAX_VALUE && it == it.toInt().toDouble()) }.toInt()
        "Long" -> (value as? Number)?.toString()?.toLongOrNull() ?: error("Invalid long")
        "Float" -> number(value).toFloat().also { require(it.isFinite()) }
        "String" -> (value as? String)?.also { require(it.length <= 256_000) } ?: error("Invalid string")
        "StringSet" -> (value as? JSONArray)?.let { array ->
            require(array.length() <= 4096)
            (0 until array.length()).map { (array.get(it) as? String)?.also { item -> require(item.length <= 16_384) } ?: error("Invalid set") }.toSet()
        } ?: error("Invalid set")
        else -> error("Unknown type")
    }

    private fun number(value: Any): Double = (value as? Number)?.toDouble()?.also { require(it.isFinite()) }
        ?: error("Invalid number")
}
