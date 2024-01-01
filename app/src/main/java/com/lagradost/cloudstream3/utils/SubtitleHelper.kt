package com.lagradost.cloudstream3.utils

import com.lagradost.cloudstream3.mvvm.logError
import java.util.*


object SubtitleHelper {
    data class Language639(
        val languageName: String,
        val nativeName: String,
        val ISO_639_1: String,
        val ISO_639_2_T: String,
        val ISO_639_2_B: String,
        val ISO_639_3: String,
        val ISO_639_6: String,
    )

    /** lang -> ISO_639_1
     * @param looseCheck will use .contains in addition to .equals
     * */
    fun fromLanguageToTwoLetters(input: String, looseCheck: Boolean): String? {
        languages.forEach {
            if (it.languageName.equals(input, ignoreCase = true)
                || it.nativeName.equals(input, ignoreCase = true)
            ) return it.ISO_639_1
        }

        // Runs as a separate loop as to prioritize fully matching languages.
        if (looseCheck)
            languages.forEach {
                if (input.contains(it.languageName, ignoreCase = true)
                    || input.contains(it.nativeName, ignoreCase = true)
                ) return it.ISO_639_1
            }

        return null
    }

    private var ISO_639_1Map: HashMap<String, String> = hashMapOf()
    private fun initISO6391Map() {
        for (lang in languages) {
            ISO_639_1Map[lang.ISO_639_1] = lang.languageName
        }
    }

    /** ISO_639_1 -> lang*/
    fun fromTwoLettersToLanguage(input: String): String? {
        // pr-BR
        if (input.substringBefore("-").length != 2) return null
        if (ISO_639_1Map.isEmpty()) {
            initISO6391Map()
        }
        val comparison = input.lowercase(Locale.ROOT)

        return ISO_639_1Map[comparison]
    }
    private const val flagOffset = 0x1F1E6
    private const val asciiOffset = 0x41
    private const val offset = flagOffset - asciiOffset

    private val flagRegex = Regex("[\uD83C\uDDE6-\uD83C\uDDFF]{2}")

    fun getFlagFromIso(inp: String?): String? {
        if (inp.isNullOrBlank() || inp.length < 2) return null

        try {
            val ret = getFlagFromIsoShort(flags[inp])
                ?: getFlagFromIsoShort(inp.uppercase()) ?: return null

            return if (flagRegex.matches(ret)) {
                ret
            } else {
                null
            }
        } catch (e: Exception) {
            logError(e)
            return null
        }
    }

    private fun getFlagFromIsoShort(flagAscii: String?): String? {
        if (flagAscii.isNullOrBlank() || flagAscii.length < 2) return null
        return try {
            val firstChar: Int = Character.codePointAt(flagAscii, 0) + offset
            val secondChar: Int = Character.codePointAt(flagAscii, 1) + offset

            (String(Character.toChars(firstChar)) + String(Character.toChars(secondChar)))
        } catch (e: Exception) {
            logError(e)
            null
        }
    }

    private val flags = mapOf(
        "ar" to "EG",
        "en" to "GB",
        "ff" to "CN",
        "fi" to "FI",
        "tr" to "TR",
    )

    val languages = listOf(
        Language639("Arabic", "العربية", "ar", "ara", "ara", "ara", ""),
        Language639("English", "English", "en", "eng", "eng", "eng", "engs"),
        Language639("French", "français, langue française", "fr", "fra", "", "fra", "fras"),
        Language639("Turkish", "Türkçe", "tr", "tur", "tur", "tur", ""),
        )

    /**ISO_639_2_B or ISO_639_2_T or ISO_639_3-> lang*/
    fun fromThreeLettersToLanguage(input: String): String? {
        if (input.length != 3) return null
        val comparison = input.lowercase(Locale.ROOT)
        for (lang in languages) {
            if (lang.ISO_639_2_B == comparison) {
                return lang.languageName
            }
        }
        for (lang in languages) {
            if (lang.ISO_639_2_T == comparison) {
                return lang.languageName
            }
        }
        for (lang in languages) {
            if (lang.ISO_639_3 == comparison) {
                return lang.languageName
            }
        }
        return null
    }

    /** lang -> ISO_639_2_T*/
    fun fromLanguageToThreeLetters(input: String): String? {
        for (lang in languages) {
            if (lang.languageName == input || lang.nativeName == input) {
                return lang.ISO_639_2_T
            }
        }
        return null
    }

}
