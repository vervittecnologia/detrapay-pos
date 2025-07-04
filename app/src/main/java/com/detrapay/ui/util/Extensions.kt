package com.detrapay.ui.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}


fun isValidCpf(cpf: String): Boolean {
    try {
        val cpfClean = cpf.replace(".", "").replace("-", "")

        //## check if size is eleven
        if (cpfClean.length != 11)
            return false

        val distinctVals = cpfClean.toCharArray().distinct()
        if (distinctVals.size == 1) return false

        //## check if is number
        try {
            val number  = cpfClean.toLong()
        }catch (e : Exception){
            return false
        }

        //continue
        var dvCurrent10 = cpfClean.substring(9,10).toInt()
        var dvCurrent11= cpfClean.substring(10,11).toInt()

        //the sum of the nine first digits determines the tenth digit
        val cpfNineFirst = IntArray(9)
        var i = 9
        while (i > 0 ) {
            cpfNineFirst[i-1] = cpfClean.substring(i-1, i).toInt()
            i--
        }
        //multiple the nine digits for your weights: 10,9..2
        var sumProductNine = IntArray(9)
        var weight = 10
        var position = 0
        while (weight >= 2){
            sumProductNine[position] = weight * cpfNineFirst[position]
            weight--
            position++
        }
        //Verify the nineth digit
        var dvForTenthDigit = sumProductNine.sum() % 11
        dvForTenthDigit = 11 - dvForTenthDigit //rule for tenth digit
        if(dvForTenthDigit > 9)
            dvForTenthDigit = 0
        if (dvForTenthDigit != dvCurrent10)
            return false

        //### verify tenth digit
        var cpfTenFirst = cpfNineFirst.copyOf(10)
        cpfTenFirst[9] = dvCurrent10
        //multiple the nine digits for your weights: 10,9..2
        var sumProductTen = IntArray(10)
        var w = 11
        var p = 0
        while (w >= 2){
            sumProductTen[p] = w * cpfTenFirst[p]
            w--
            p++
        }
        //Verify the nineth digit
        var dvForeleventhDigit = sumProductTen.sum() % 11
        dvForeleventhDigit = 11 - dvForeleventhDigit //rule for tenth digit
        if(dvForeleventhDigit > 9)
            dvForeleventhDigit = 0
        if (dvForeleventhDigit != dvCurrent11)
            return false

        return true
    }catch (e:Exception){
        return false
    }
}

private fun getRealVerifierNumber(calculationTotal: Int): Int {
    val modOfFirstDvision = calculationTotal % 11
    return if (modOfFirstDvision < 2) {
        0
    } else {
        11 - modOfFirstDvision
    }
}

private fun getCpfNumbersCalculation(rangeEnd: Int, numbers: String) =
    IntProgression.fromClosedRange(0, rangeEnd, 1)
        .reversed()
        .map { position ->
            val multiplier = (rangeEnd - position) + 2
            multiplier * numbers[position].digitToInt()
        }
        .reduce(Integer::sum)

fun isValidCpnj(cnpjInput: String): Boolean {
    try {
        val cnpj = cnpjInput.replace(" ", "")
            .replace(".", "")
            .replace(",", "")
            .replace("-", "")
            .replace("/", "")
            .replace("\\s".toRegex(), "")

        val distinctVals = cnpj.toCharArray().distinct()
        if (distinctVals.size == 1) return false

        val firstBaseCnpjNumbersMultipliers = arrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        val secondBaseCnpjNumbersMultipliers = arrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)

        val firstTwelveNumbersOfCnpj = cnpj.subSequence(0, 12).toString()
        val actualFirstVerifier = cnpj[12].digitToInt()
        val actualSecondVerifier = cnpj[13].digitToInt()

        val firstCalculation = calculateCnpjBaseNumbers(firstBaseCnpjNumbersMultipliers, firstTwelveNumbersOfCnpj)
        val realFirstVerifier = getRealVerifierNumber(firstCalculation)
        if (realFirstVerifier != actualFirstVerifier) {
            return false
        }

        val firstThirteenNumbersOfCnpj = cnpj.subSequence(0, 13).toString()
        val secondCalculation = calculateCnpjBaseNumbers(secondBaseCnpjNumbersMultipliers, firstThirteenNumbersOfCnpj)

        val realSecondVerifier = getRealVerifierNumber(secondCalculation)
        return (actualSecondVerifier == realSecondVerifier)
    }catch (e:Exception){
        return false
    }
}

private fun calculateCnpjBaseNumbers(baseMultipliers: Array<Int>, numers: String) =
    baseMultipliers.mapIndexed { position, value -> numers[position].digitToInt() * value }
        .reduce(Integer::sum)