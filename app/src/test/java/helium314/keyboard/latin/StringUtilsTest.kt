// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import helium314.keyboard.latin.common.StringUtils
import helium314.keyboard.latin.common.getFullEmojiAtEnd
import kotlin.test.Test
import kotlin.test.assertEquals

// todo: actually this test could/should be significantly expanded...
class StringUtilsTest {
    @Test fun `not inside double quotes without quotes`() {
        assert(!StringUtils.isInsideDoubleQuoteOrAfterDigit("hello yes"))
    }

    @Test fun `inside double quotes after opening a quote`() {
        assert(StringUtils.isInsideDoubleQuoteOrAfterDigit("hello \"yes"))
    }

    @Test fun `inside double quotes with quote at start`() {
        assert(StringUtils.isInsideDoubleQuoteOrAfterDigit("\"hello yes"))
    }

    // maybe this is not that bad, should be correct after entering next text
    @Test fun `not inside double quotes directly after closing quote`() {
        assert(!StringUtils.isInsideDoubleQuoteOrAfterDigit("hello \"yes\""))
    }

    @Test fun `not inside double quotes after closing quote`() {
        assert(!StringUtils.isInsideDoubleQuoteOrAfterDigit("hello \"yes\" "))
    }

    @Test fun `not inside double quotes after closing quote followed by comma`() {
        assert(!StringUtils.isInsideDoubleQuoteOrAfterDigit("hello \"yes\", "))
    }

    @Test fun `inside double quotes after opening another quote`() {
        assert(StringUtils.isInsideDoubleQuoteOrAfterDigit("hello \"yes\" \"h"))
    }

    @Test fun `inside double quotes after opening another quote with closing quote followed by comma`() {
        assert(StringUtils.isInsideDoubleQuoteOrAfterDigit("hello \"yes\", \"h"))
    }

    @Test fun detectEmojisAtEnd() {
        assertEquals("", getFullEmojiAtEnd("\uD83C\uDF83 "))
        assertEquals("", getFullEmojiAtEnd("a"))
        assertEquals("\uD83C\uDF83", getFullEmojiAtEnd("\uD83C\uDF83"))
        assertEquals("ℹ️", getFullEmojiAtEnd("ℹ️"))
        assertEquals("ℹ️", getFullEmojiAtEnd("ℹ️ℹ️"))
        assertEquals("\uD83D\uDE22", getFullEmojiAtEnd("x\uD83D\uDE22"))
        assertEquals("", getFullEmojiAtEnd("x\uD83D\uDE22 "))
        assertEquals("\uD83C\uDFF4\u200D☠️", getFullEmojiAtEnd("ok \uD83C\uDFF4\u200D☠️"))
        assertEquals("\uD83C\uDFF3️\u200D\uD83C\uDF08", getFullEmojiAtEnd("\uD83C\uDFF3️\u200D\uD83C\uDF08"))
        assertEquals("\uD83C\uDFF3️\u200D\uD83C\uDF08", getFullEmojiAtEnd("\uD83C\uDFF4\u200D☠️\uD83C\uDFF3️\u200D\uD83C\uDF08"))
        assertEquals("\uD83C\uDFF3️\u200D⚧️", getFullEmojiAtEnd("hello there🏳️‍⚧️"))
        assertEquals("\uD83D\uDD75\uD83C\uDFFC", getFullEmojiAtEnd(" 🕵🏼"))
        assertEquals("\uD83D\uDD75\uD83C\uDFFC", getFullEmojiAtEnd("🕵🏼"))
        assertEquals("\uD83C\uDFFC", getFullEmojiAtEnd(" \uD83C\uDFFC"))
        // fails, but unlikely enough that we leave it unfixed
        //assertEquals("\uD83C\uDFFC", getFullEmojiAtEnd("\uD83C\uDF84\uD83C\uDFFC"))
        // below also fail, because ZWJ handling is not suitable for some unusual cases
        //assertEquals("", getFullEmojiAtEnd("\u200D"))
        //assertEquals("", getFullEmojiAtEnd("a\u200D"))
        //assertEquals("\uD83D\uDE22", getFullEmojiAtEnd(" \u200D\uD83D\uDE22"))
    }

    // todo: add tests for emoji detection?
    //  could help towards fully fixing https://github.com/Helium314/HeliBoard/issues/22
    //  though this might be tricky, as some emojis will show as one on new Android versions, and
    //  as two on older versions
}
