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
            if (str.isEmpty()) {
                return 0.0
            }
            return str.replace(".", "").replace("-", "")
                .replace("(", "").replace(")", "")
                .replace("/", "").replace("*", "")
                .replace(" ", "").replace("\\s".toRegex(), "")
                .replace(",", ".")
                .replace("R$", "")
                .format("%.2f").toDouble()
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
            val textWatcher: TextWatcher = object : TextWatcher {
                var isUpdating: Boolean = false

                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val str =
                        s.toString().trim()
                            .replace(".", "")
                            .replace(",", "")
                            .replace("R$", "")
                            .replace("\\s".toRegex(), "")

                    //is deleting
                    if (count == 0) isUpdating = true

                    if (isUpdating) {
                        isUpdating = false
                        return
                    }

                    val parsed: BigDecimal =
                        BigDecimal(str).setScale(2, BigDecimal.ROUND_FLOOR)
                            .divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)

                    val fieldWithMask: String =
                        NumberFormat.getCurrencyInstance(locale).format(parsed)

                    isUpdating = true
                    edTxt.setText(fieldWithMask)
                    edTxt.setSelection(fieldWithMask.length)
                }

                override fun afterTextChanged(editable: Editable) {
                    afterChanged(editable.toString())
                }
            }

            return textWatcher

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
