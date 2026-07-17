package com.detrapay.data.model.remote

import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationResponseTest {

    private val gson = Gson()

    @Test
    fun fromJson_mapsDiscountAllowedFromPrimaryField() {
        val json = """
            {
              "data": {
                "attributes": {
                  "billing_date": "2026-03-05",
                  "cpf_cnpj": "12345678900",
                  "name": "Cliente",
                  "phone_number": "11999999999",
                  "vehicle_price": "10000.00",
                  "is_vehicle_financed": false,
                  "is_vehicle_special_plate": false,
                  "current_amount": 100.0,
                  "vehicle_type_id": 1,
                  "items": [
                    {
                      "id": 1,
                      "attributes": {
                        "name": "Servico",
                        "is_discount_allowed": true,
                        "price": 100.0
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, SimulationResponse::class.java)

        assertTrue(response.data.attributes.items.first().attributes.discountAllowed)
    }

    @Test
    fun fromJson_mapsDiscountAllowedFromAlternateFields() {
        val json = """
            {
              "data": {
                "attributes": {
                  "billing_date": "2026-03-05",
                  "cpf_cnpj": "12345678900",
                  "name": "Cliente",
                  "phone_number": "11999999999",
                  "vehicle_price": "10000.00",
                  "is_vehicle_financed": false,
                  "is_vehicle_special_plate": false,
                  "current_amount": 100.0,
                  "vehicle_type_id": 1,
                  "items": [
                    {
                      "id": 1,
                      "attributes": {
                        "name": "Servico 1",
                        "allow-discount": true,
                        "price": 100.0
                      }
                    },
                    {
                      "id": 2,
                      "attributes": {
                        "name": "Servico 2",
                        "allow_discout": false,
                        "price": 50.0
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, SimulationResponse::class.java)

        assertTrue(response.data.attributes.items[0].attributes.discountAllowed)
        assertFalse(response.data.attributes.items[1].attributes.discountAllowed)
    }

    @Test
    fun fromJson_mapsDiscountAllowedFromTypoFieldUsedByBackend() {
        val json = """
            {
              "data": {
                "attributes": {
                  "billing_date": "2026-03-05",
                  "cpf_cnpj": "12345678900",
                  "name": "Cliente",
                  "phone_number": "11999999999",
                  "vehicle_price": "10000.00",
                  "is_vehicle_financed": false,
                  "is_vehicle_special_plate": false,
                  "current_amount": 100.0,
                  "vehicle_type_id": 1,
                  "items": [
                    {
                      "id": 1,
                      "attributes": {
                        "name": "Servico",
                        "allow_discounnt": true,
                        "price": 100.0
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, SimulationResponse::class.java)

        assertTrue(response.data.attributes.items.first().attributes.discountAllowed)
    }
}
