package com.tono.app.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhatsNewTest {

    @Test
    fun first_install_does_not_show() {
        assertFalse(shouldShowWhatsNew("0.6.0", "0.6.0", lastSeenVersion = null))
    }

    @Test
    fun upgrade_with_matching_bundle_shows() {
        assertTrue(shouldShowWhatsNew("0.6.0", "0.6.0", lastSeenVersion = "0.5.0"))
    }

    @Test
    fun same_version_does_not_show() {
        assertFalse(shouldShowWhatsNew("0.6.0", "0.6.0", lastSeenVersion = "0.6.0"))
    }

    @Test
    fun bundled_placeholder_does_not_show() {
        assertFalse(shouldShowWhatsNew("0.6.0", "0.0.0", lastSeenVersion = "0.5.0"))
    }

    @Test
    fun prerelease_suffix_requires_exact_match() {
        assertTrue(shouldShowWhatsNew("0.6.0-beta", "0.6.0-beta", lastSeenVersion = "0.5.0"))
        assertFalse(shouldShowWhatsNew("0.6.0-beta", "0.6.0", lastSeenVersion = "0.5.0"))
    }

    @Test
    fun parse_full_payload() {
        val info = parseWhatsNew("""{"version":"0.6.0","zh":["修复崩溃"],"en":["Fix crash"]}""")
        assertEquals("0.6.0", info?.version)
        assertEquals(listOf("修复崩溃"), info?.zh)
        assertEquals(listOf("Fix crash"), info?.en)
        assertFalse(info!!.isEmpty)
    }

    @Test
    fun parse_placeholder_is_empty() {
        assertTrue(parseWhatsNew("""{"version":"0.0.0","zh":[],"en":[]}""")!!.isEmpty)
    }

    @Test
    fun parse_garbage_returns_null() {
        assertNull(parseWhatsNew("not json"))
    }

    @Test
    fun pick_by_language_falls_back() {
        assertEquals(listOf("中"), pickByLanguage("zh", listOf("中"), listOf("en")))
        assertEquals(listOf("en"), pickByLanguage("ja", listOf("中"), listOf("en")))
        assertEquals(listOf("en"), pickByLanguage("zh", emptyList(), listOf("en")))
        assertEquals(listOf("中"), pickByLanguage("en", listOf("中"), emptyList()))
    }
}
