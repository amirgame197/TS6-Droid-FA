package dev.tsdroid.data

import java.io.ByteArrayOutputStream

data class TsServerLink(
    val address: String,
    val nickname: String? = null,
    val password: String? = null,
    val channel: String? = null,
)

object TsServerLinkParser {

    const val SCHEME = "ts3server"
    const val DEFAULT_PORT = "9987"

    fun parse(raw: String?): TsServerLink? {
        val trimmed = raw?.trim().orEmpty()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd <= 0) return null
        if (!trimmed.take(schemeEnd).equals(SCHEME, ignoreCase = true)) return null

        val rest = trimmed.substring(schemeEnd + 3)
        val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }
        val authority = if (authorityEnd >= 0) rest.take(authorityEnd) else rest
        val queryStart = rest.indexOf('?')
        val params = if (queryStart >= 0) parseQuery(rest.substring(queryStart + 1)) else emptyMap()

        val (host, explicitPort) = splitAuthority(authority)
        if (host.isEmpty()) return null

        val port = explicitPort ?: params["port"]?.let(::normalizePort) ?: DEFAULT_PORT

        return TsServerLink(
            address = "$host:$port",
            nickname = params["nickname"]?.takeIf { it.isNotEmpty() },
            password = params["password"]?.takeIf { it.isNotEmpty() },
            channel = params["channel"]?.takeIf { it.isNotEmpty() },
        )
    }

    private fun splitAuthority(authority: String): Pair<String, String?> {
        val portColon = authority.lastIndexOf(':')
        if (portColon < 0) return authority to null
        val candidate = authority.substring(portColon + 1)
        val port = normalizePort(candidate)
        return when {
            port != null -> authority.take(portColon) to port
            candidate.isEmpty() -> authority.take(portColon) to null
            else -> authority to null
        }
    }

    private fun normalizePort(raw: String): String? {
        if (raw.isEmpty() || raw.any { it !in '0'..'9' }) return null
        val port = raw.toIntOrNull() ?: return null
        return if (port in 1..65535) port.toString() else null
    }

    private fun parseQuery(query: String): Map<String, String> {
        val fragmentStart = query.indexOf('#')
        val pairs = (if (fragmentStart >= 0) query.take(fragmentStart) else query).split('&')
        val params = mutableMapOf<String, String>()
        for (pair in pairs) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            if (eq < 0) {
                params[pair.lowercase()] = ""
            } else {
                params[pair.take(eq).lowercase()] = percentDecode(pair.substring(eq + 1))
            }
        }
        return params
    }

    private fun percentDecode(raw: String): String {
        if (!raw.contains('%')) return raw
        val out = ByteArrayOutputStream(raw.length)
        var i = 0
        while (i < raw.length) {
            val hi = raw.getOrNull(i + 1)?.let(::hexDigit)
            val lo = raw.getOrNull(i + 2)?.let(::hexDigit)
            if (raw[i] == '%' && hi != null && lo != null) {
                out.write((hi shl 4) or lo)
                i += 3
            } else {
                var end = i + 1
                while (end < raw.length && raw[end] != '%') end++
                out.write(raw.substring(i, end).toByteArray(Charsets.UTF_8))
                i = end
            }
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    private fun hexDigit(c: Char): Int? = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> null
    }
}
