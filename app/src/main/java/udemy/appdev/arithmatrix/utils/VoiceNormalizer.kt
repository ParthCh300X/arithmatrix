package udemy.appdev.arithmatrix.utils

import java.util.Locale

object VoiceNormalizer {

    fun normalize(raw: String): String {
        var s = raw.lowercase(Locale.getDefault())

        // word-to-number first so "forty five" → "45" before function matching
        s = wordsToNumbers(s)

        s = s
            // ── scientific phrases with "of" ──────────────────────────────
            .replace(Regex("(?:sine?|sin)\\s+of\\s+(\\d+(?:\\.\\d+)?)"))           { "sin(${it.groupValues[1]})" }
            .replace(Regex("(?:cosine?|cos)\\s+of\\s+(\\d+(?:\\.\\d+)?)"))         { "cos(${it.groupValues[1]})" }
            .replace(Regex("(?:tangent|tan)\\s+of\\s+(\\d+(?:\\.\\d+)?)"))         { "tan(${it.groupValues[1]})" }
            .replace(Regex("(?:square\\s*root|sqrt)\\s*of\\s*(\\d+(?:\\.\\d+)?)")) { "sqrt(${it.groupValues[1]})" }
            .replace(Regex("log\\s+of\\s+(\\d+(?:\\.\\d+)?)"))                     { "log(${it.groupValues[1]})" }
            .replace(Regex("ln\\s+of\\s+(\\d+(?:\\.\\d+)?)"))                      { "ln(${it.groupValues[1]})" }
            // ── scientific phrases with just a space (no "of") ────────────
            // "sin 75", "cos 90", "log 100"
            .replace(Regex("(?:sine?|sin)\\s+(\\d+(?:\\.\\d+)?)"))                 { "sin(${it.groupValues[1]})" }
            .replace(Regex("(?:cosine?|cos)\\s+(\\d+(?:\\.\\d+)?)"))               { "cos(${it.groupValues[1]})" }
            .replace(Regex("(?:tangent|tan)\\s+(\\d+(?:\\.\\d+)?)"))               { "tan(${it.groupValues[1]})" }
            .replace(Regex("(?:square\\s*root|sqrt)\\s*(\\d+(?:\\.\\d+)?)"))       { "sqrt(${it.groupValues[1]})" }
            .replace(Regex("log\\s+(\\d+(?:\\.\\d+)?)"))                           { "log(${it.groupValues[1]})" }
            .replace(Regex("ln\\s+(\\d+(?:\\.\\d+)?)"))                            { "ln(${it.groupValues[1]})" }
            // ── power expressions ─────────────────────────────────────────
            .replace(Regex("(\\d+(?:\\.\\d+)?)\\s+squared"))                       { "${it.groupValues[1]}^2" }
            .replace(Regex("(\\d+(?:\\.\\d+)?)\\s+cubed"))                         { "${it.groupValues[1]}^3" }
            .replace(Regex("(\\d+(?:\\.\\d+)?)\\s+to\\s+the\\s+power\\s+of\\s+(\\d+(?:\\.\\d+)?)")) {
                "${it.groupValues[1]}^${it.groupValues[2]}"
            }
            // ── standard operator aliases ─────────────────────────────────
            .replace(Regex("\\bx\\b"), "*")
            .replace("×", "*")
            .replace("times", "*")
            .replace("into", "*")
            .replace("divided by", "/")
            .replace("divide by", "/")
            .replace("÷", "/")
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("point", ".")
            // strip all spaces last
            .replace(Regex("\\s+"), "")
            .trim()

        return s
    }

    fun wordsToNumbers(input: String): String {
        val ones = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3",
            "four" to "4", "five" to "5", "six" to "6", "seven" to "7",
            "eight" to "8", "nine" to "9", "ten" to "10", "eleven" to "11",
            "twelve" to "12", "thirteen" to "13", "fourteen" to "14",
            "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
            "eighteen" to "18", "nineteen" to "19"
        )
        val tens = mapOf(
            "twenty" to "20", "thirty" to "30", "forty" to "40",
            "fifty" to "50", "sixty" to "60", "seventy" to "70",
            "eighty" to "80", "ninety" to "90"
        )

        var result = input

        // compound: "twenty five" → "25"
        for ((tensWord, tensVal) in tens) {
            for ((onesWord, onesVal) in ones) {
                result = result.replace(
                    "$tensWord $onesWord",
                    (tensVal.toInt() + onesVal.toInt()).toString()
                )
            }
            result = result.replace(tensWord, tensVal)
        }
        for ((word, num) in ones) {
            result = result.replace(Regex("\\b$word\\b"), num)
        }
        // "three hundred" → "300"
        result = result.replace(Regex("(\\d+)\\s*hundred")) { mr ->
            (mr.groupValues[1].toInt() * 100).toString()
        }

        return result
    }
}