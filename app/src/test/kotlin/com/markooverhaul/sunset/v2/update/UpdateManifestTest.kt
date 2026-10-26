package com.markooverhaul.sunset.v2.update

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses the published manifest shape`() {
        val manifest = json.decodeFromString<UpdateManifest>(
            """
            {
              "versionCode": 2,
              "versionName": "0.2.0",
              "apkUrl": "https://example.com/sunset-0.2.0.apk",
              "sha256": "abc123",
              "notes": "Live fixes",
              "mandatory": false
            }
            """.trimIndent(),
        )
        assertEquals(2, manifest.versionCode)
        assertEquals("0.2.0", manifest.versionName)
        assertEquals("https://example.com/sunset-0.2.0.apk", manifest.apkUrl)
        assertEquals("abc123", manifest.sha256)
        assertFalse(manifest.mandatory)
    }

    @Test
    fun `optional fields tolerate missing values and unknown keys`() {
        val manifest = json.decodeFromString<UpdateManifest>(
            """{"versionCode":3,"apkUrl":"https://example.com/a.apk","futureField":true}""",
        )
        assertEquals(3, manifest.versionCode)
        assertEquals("", manifest.sha256)
        assertEquals("", manifest.notes)
        assertFalse(manifest.mandatory)
    }

    @Test
    fun `only a higher version code is an update`() {
        val manifest = UpdateManifest(versionCode = 4, apkUrl = "https://example.com/a.apk")
        assertTrue(manifest.isNewerThan(3))
        assertFalse(manifest.isNewerThan(4))
        assertFalse(manifest.isNewerThan(5))
    }
}
