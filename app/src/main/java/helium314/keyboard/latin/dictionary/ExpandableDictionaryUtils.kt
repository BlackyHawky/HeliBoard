// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.dictionary

import android.content.Context
import helium314.keyboard.latin.define.DebugFlags
import helium314.keyboard.latin.dictionary.history.UserHistoryDictionary
import helium314.keyboard.latin.utils.Log

object ExpandableDictionaryUtils {
    private val upToDateDicts = hashSetOf<String>()

    /** fill with dicts from file system, so we don't recreate on app start (misses changes that were made while different keyboard were active) */
    fun initUpToDateDicts(context: Context) {
        val files = context.filesDir.listFiles {
            it.isDirectory && it.name.endsWith(".dict") && !it.name.startsWith(UserHistoryDictionary.NAME)
        }
        upToDateDicts.addAll(files!!.map { it.nameWithoutExtension })
    }

    @JvmStatic
    fun setHasUpdated(dict: ExpandableBinaryDictionary) {
        if (dict !is UserHistoryDictionary)
            upToDateDicts.add(dict.dictName)
    }

    @JvmStatic
    fun needsUpdate(dict: ExpandableBinaryDictionary) = dict !is UserHistoryDictionary && !upToDateDicts.contains(dict.dictName)

    fun setNeedsUpdate(typeName: String) {
        if (DebugFlags.DEBUG_ENABLED)
            Log.d("ExpandableDictionaryUtils", "Dictionary \"$typeName\" needs update")
        upToDateDicts.removeAll { it.startsWith(typeName) }
    }
}
