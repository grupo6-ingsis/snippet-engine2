package org.gudelker.snippet.engine.utils

import org.gudelker.utilities.Version


object VersionAdapter {
    fun toVersion(version: String): Version {
        return when (version) {
            "1.0" -> Version.V1
            "1.1" -> Version.V2
            else -> throw IllegalArgumentException("Unsupported version: $version")
        }
    }
}