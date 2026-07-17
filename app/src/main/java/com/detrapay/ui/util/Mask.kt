package com.detrapay.ui.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class Mask {
    companion object {
        val locale = Locale("pt", "BR")

        fun replaceChars(str: String): String {
            return str.replace(".", "").replace("-", "")
                .replace("(", "").replace(")", "")
                .replace("/", "").replace("*", "")
                .replace(" ", "").replace("\\s".toRegex(), "")
                .replace("R$", "")
                .replace(",", "")
        }

        fun doubleValue(str: String): Double {
            return toSafeDouble(str)
        }

        /**
         * Safely converts a monetary string to Double.
         * Handles both standard decimal (10.44) and BRL format (10,44 or 1.010,44).
         */
        fun toSafeDouble(str: String): Double {
            if (str.isEmpty()) return 0.0
            
            val clean = str.replace("R$", "").replace("\\s".toRegex(), "").trim()
            if (clean.isEmpty()) return 0.0

            return try {
                if (clean.contains(",")) {
                    // Brazilian format: 1.250,50 -> 1250.50
                    clean.replace(".", "").replace(",", ".").toDouble()
                } else {
                    // Standard decimal: 1250.50 -> 1250.50
                    // We assume that if there's no comma, the dot is the decimal separator.
                    clean.toDouble()
                }
            } catch (e: Exception) {
                0.0
            }
        }

        fun cpfCnpjMask(edTxt: EditText): TextWatcher {
            val textWatcher: TextWatcher = object : TextWatcher {
                var isUpdating: Boolean = false
                var oldString: String = ""

                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    // CPF MASK
                    val cpfMask = "###.###.###-##"
                    val cnpjMask = "##.###.###/####-##"

                    val mask = if (edTxt.length() <= 14) {
                        cpfMask
                    } else {
                        // CNPJ MASK
                        cnpjMask
                    }

                    val str = replaceChars(s.toString())
                    var fieldWithMask = ""

                    if (count == 0)//is deleting
                        isUpdating = true

                    if (isUpdating) {
                        oldString = str
                        isUpdating = false
                        return
                    }

                    var i = 0
                    for (m: Char in mask.toCharArray()) {
                        if (m != '#' && str.length > oldString.length) {
                            fieldWithMask += m
                            continue
                        }
                        try {
                            fieldWithMask += str.get(i)
                        } catch (e: Exception) {
                            break
                        }
                        i++
                    }

                    isUpdating = true
                    edTxt.setText(fieldWithMask)
                    edTxt.setSelection(fieldWithMask.length)

                }

                override fun afterTextChanged(editable: Editable) {

                }
            }

            return textWatcher

        }

        fun moneyMask(edTxt: EditText, afterChanged: (value: String) -> Unit): TextWatcher {
            return object : TextWatcher {
                private var current = ""

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    if (s.toString() != current) {
                        edTxt.removeTextChangedListener(this)

                        val cleanString = s.toString().replace("[R$,.\\s]".toRegex(), "")
                        
                        val formatted = if (cleanString.isNotEmpty()) {
                            try {
                                val parsed = BigDecimal(cleanString).setScale(2, BigDecimal.ROUND_FLOOR)
                                    .divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)
                                
                                val numberFormat = NumberFormat.getNumberInstance(locale)
                                numberFormat.minimumFractionDigits = 2
                                numberFormat.maximumFractionDigits = 2
                                numberFormat.format(parsed)
                            } catch (e: Exception) {
                                ""
                            }
                        } else {
                            ""
                        }

                        current = formatted
                        edTxt.setText(formatted)
                        edTxt.setSelection(formatted.length)

                        edTxt.addTextChangedListener(this)
                        afterChanged(formatted)
                    }
                }
            }
        }


        fun mask(mask: String, edTxt: EditText): TextWatcher {

            val textWatcher: TextWatcher = object : TextWatcher {
                var isUpdating: Boolean = false
                var oldString: String = ""

                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val str = replaceChars(s.toString())
                    var fieldWithMask = ""

                    if (count == 0)//is deleting
                        isUpdating = true
                    if (isUpdating) {
                        oldString = str
                        isUpdating = false
                        return
                    }

                    var i = 0
                    for (m: Char in mask.toCharArray()) {
                        if (m != '#' && str.length > oldString.length) {
                            fieldWithMask += m
                            continue
                        }
                        try {
                            fieldWithMask += str.get(i)
                        } catch (e: Exception) {
                            break
                        }
                        i++
                    }

                    isUpdating = true
                    edTxt.setText(fieldWithMask)
                    edTxt.setSelection(fieldWithMask.length)

                }

                override fun afterTextChanged(editable: Editable) {

                }
            }

            return textWatcher
        }
    }
}
