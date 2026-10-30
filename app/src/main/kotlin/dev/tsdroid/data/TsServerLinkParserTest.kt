package dev.tsdroid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TsServerLinkParserTest {

    private fun parse(raw: String?): TsServerLink =
        TsServerLinkParser.parse(raw) ?: error("expected a parsable link: <$raw>")

    @Test
    fun `a bare host gains the default port`() {
        val link = parse("ts3server://voice.example.com")
        assertEquals("voice.example.com:9987", link.address)
        assertNull(link.nickname)
        assertNull(link.password)
        assertNull(link.channel)
    }

    @Test
    fun `a port in the authority is kept`() {
        assertEquals("host.example.com:1337", parse("ts3server://host.example.com:1337").address)
    }

    @Test
    fun `a port parameter is used when the authority carries none`() {
        assertEquals("host.example.com:1337", parse("ts3server://host.example.com?port=1337").address)
    }

    @Test
    fun `the authority port wins over a contradicting port parameter`() {
        assertEquals("host:1337", parse("ts3server://host:1337?port=42").address)
    }

    @Test
    fun `leading zeros in a port are normalised away`() {
        assertEquals("host:42", parse("ts3server://host:042").address)
        assertEquals("host:9987", parse("ts3server://host?port=09987").address)
    }

    @Test
    fun `an out-of-range or non-numeric port counts as absent`() {
        assertEquals("host:9987", parse("ts3server://host?port=70000").address)
        assertEquals("host:9987", parse("ts3server://host?port=0").address)
        assertEquals("host:9987", parse("ts3server://host?port=abc").address)
    }

    @Test
    fun `a non-numeric port keeps the authority as typed`() {
        // Not dropped, and not silently trimmed either: the form shows the bad value so the user can fix it by hand
        assertEquals("host:notaport:9987", parse("ts3server://host:notaport").address)
    }

    @Test
    fun `a trailing colon on the host is forgiven`() {
        assertEquals("host:9987", parse("ts3server://host:").address)
        assertEquals("host:1337", parse("ts3server://host:?port=1337").address)
    }

    @Test
    fun `bracketed ipv6 literals keep their port`() {
        assertEquals("[2001:db8::1]:9988", parse("ts3server://[2001:db8::1]:9988").address)
    }

    @Test
    fun `nickname password and channel ride along as parameters`() {
        val link = parse("ts3server://host?nickname=Nick&password=pw&channel=Lobby")
        assertEquals("host:9987", link.address)
        assertEquals("Nick", link.nickname)
        assertEquals("pw", link.password)
        assertEquals("Lobby", link.channel)
    }

    @Test
    fun `percent escapes decode as utf-8`() {
        val link = parse("ts3server://host?nickname=My%20Name&channel=R%C3%A4ume%20%E4%B8%AD%E6%96%87")
        assertEquals("My Name", link.nickname)
        assertEquals("Räume 中文", link.channel)
    }

    @Test
    fun `a plus is a literal plus, not a space`() {
        // The TeamSpeak client decodes percent-escapes only, so this parser must not turn a `+` into a space the desktop client would keep.
        assertEquals("C+Plus", parse("ts3server://host?nickname=C+Plus").nickname)
    }

    @Test
    fun `malformed percent escapes survive as typed`() {
        assertEquals("100%wrong", parse("ts3server://host?nickname=100%wrong").nickname)
        assertEquals("50%", parse("ts3server://host?channel=50%").channel)
    }

    @Test
    fun `empty parameter values count as absent`() {
        val link = parse("ts3server://host?nickname=&password=pw")
        assertNull(link.nickname)
        assertEquals("pw", link.password)
    }

    @Test
    fun `addbookmark and channelpassword are accepted but ignored`() {
        val link = parse("ts3server://host?addbookmark=1&channelpassword=secret&nickname=Nick")
        assertEquals("Nick", link.nickname)
        assertNull(link.channel)
        assertNull(link.password)
    }

    @Test
    fun `keys are matched case-insensitively and later duplicates win`() {
        val link = parse("ts3server://host?Nickname=First&NICKNAME=Second&PASSWORD=pw")
        assertEquals("Second", link.nickname)
        assertEquals("pw", link.password)
    }

    @Test
    fun `a path before the query does not hide the query`() {
        assertEquals("host:1337", parse("ts3server://host/?port=1337").address)
    }

    @Test
    fun `a fragment belongs to no value`() {
        assertEquals("Nick", parse("ts3server://host?nickname=Nick#section").nickname)
    }

    @Test
    fun `the scheme matches case-insensitively and the host keeps its case`() {
        assertEquals("Host:9987", parse("TS3SERVER://Host").address)
    }

    @Test
    fun `surrounding whitespace is tolerated`() {
        assertEquals("host:9987", parse("  ts3server://host  ").address)
    }

    @Test
    fun `foreign or missing schemes are refused`() {
        assertNull(TsServerLinkParser.parse("https://host"))
        assertNull(TsServerLinkParser.parse("teamspeak://host"))
        assertNull(TsServerLinkParser.parse("ts3server:host"))
        assertNull(TsServerLinkParser.parse(""))
        assertNull(TsServerLinkParser.parse(null))
    }

    @Test
    fun `an empty host is refused`() {
        assertNull(TsServerLinkParser.parse("ts3server://"))
        assertNull(TsServerLinkParser.parse("ts3server://?port=1337"))
    }
}
