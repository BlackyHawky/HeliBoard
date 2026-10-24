// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import helium314.keyboard.dictionarypack.DictionaryPackConstants
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.emoji.EmojiSearchActivity
import helium314.keyboard.latin.utils.FoldableUtils
import helium314.keyboard.latin.utils.FoldableUtils.FoldableObserver
import helium314.keyboard.latin.utils.Log

// todo: also for apps, contacts and personal dictionary (because dicts often aren't updated when they should be)
class StuffReceiver(latinIME: LatinIME) {
    private val dictionaryPackInstallReceiver = DictionaryPackInstallBroadcastReceiver(latinIME)
    private val ringerModeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (AudioManager.RINGER_MODE_CHANGED_ACTION == action) {
                var dnd: Boolean
                try {
                    dnd = android.provider.Settings.Global.getInt(context.contentResolver, "zen_mode") != 0
                } catch (_: android.provider.Settings.SettingNotFoundException) {
                    dnd = false
                    Log.w(TAG, "zen_mode setting not found, assuming disabled")
                }
                Log.i(TAG, "ringer mode changed, zen_mode on: $dnd")
                AudioAndHapticFeedbackManager.getInstance().onRingerModeChanged(dnd)
            }
        }
    }

    private val emojiSearchReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        private fun isImeClosed(intent: Intent) = intent.getBooleanExtra(EmojiSearchActivity.IME_CLOSED_KEY, false)

        private fun getSelectedEmoji(intent: Intent) = intent.getStringExtra(EmojiSearchActivity.EMOJI_KEY)

        private fun isEmojiSearch() = EmojiSearchActivity.decodePrivateImeOptions(latinIME.currentInputEditorInfo).height > 0

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(
                TAG, "after activity closing. isEmojiSearch: ${isEmojiSearch()}. Intent: $intent" +
                    intent?.let { ". imeClosed: ${isImeClosed(intent)}. selected emoji: ${getSelectedEmoji(intent)}" }
            )
            if (intent?.action != EmojiSearchActivity.EMOJI_SEARCH_DONE_ACTION || !isEmojiSearch()) return
            if (isImeClosed(intent)) {
                latinIME.requestHideSelf(0)
            } else {
                latinIME.mHandler.postDelayed({ KeyboardSwitcher.getInstance().setEmojiKeyboard() }, 100)
                if (intent.hasExtra(EmojiSearchActivity.EMOJI_KEY)) {
                    latinIME.onTextInput(intent.getStringExtra(EmojiSearchActivity.EMOJI_KEY))
                }
            }
        }
    }

    private val restartAfterDeviceUnlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            // Restart the keyboard if credential encrypted storage is unlocked. This reloads the
            // dictionary and other data from credential-encrypted storage (with the onCreate()
            // method).
            if (Intent.ACTION_USER_UNLOCKED == action) {
                val myPid = Process.myPid()
                Log.i(TAG, "Killing my process: pid=$myPid")
                Process.killProcess(myPid)
            } else {
                Log.e(TAG, "Unexpected intent $intent")
            }
        }
    }

    private var foldableObserver: FoldableObserver? = null

    fun register(context: Context) {
        context.registerReceiver(ringerModeChangeReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))

        val dictFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
        dictFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        dictFilter.addAction(DictionaryPackConstants.NEW_DICTIONARY_INTENT_ACTION)
        dictFilter.addDataScheme(SCHEME_PACKAGE)
        // RECEIVER_EXPORTED is necessary because apparently Android 15 (and others?) don't recognize
        // if the sender and receiver are the same app, see https://github.com/HeliBorg/HeliBoard/pull/1756
        ContextCompat.registerReceiver(context, dictionaryPackInstallReceiver, dictFilter, ContextCompat.RECEIVER_EXPORTED)

        ContextCompat.registerReceiver(context, emojiSearchReceiver, IntentFilter(EmojiSearchActivity.EMOJI_SEARCH_DONE_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context.registerReceiver(restartAfterDeviceUnlockReceiver, IntentFilter(Intent.ACTION_USER_UNLOCKED))

        if (FoldableUtils.isFoldable)
            foldableObserver = FoldableObserver(context)
    }

    fun unregisterAll(context: Context) {
        context.unregisterReceiver(ringerModeChangeReceiver)
        foldableObserver?.unregister(context)
        foldableObserver = null
        context.unregisterReceiver(dictionaryPackInstallReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context.unregisterReceiver(restartAfterDeviceUnlockReceiver)
        context.unregisterReceiver(emojiSearchReceiver)
    }

    companion object {
        private val TAG = StuffReceiver::class.java.simpleName
        // The name of the scheme used by the Package Manager to warn of a new package installation, replacement or removal.
        private const val SCHEME_PACKAGE: String = "package"
    }
}
