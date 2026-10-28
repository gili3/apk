package com.eleven.store.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {

    @Test
    fun orderStatus_from_isCaseInsensitive() {
        assertEquals(OrderStatus.DELIVERED, OrderStatus.from("delivered"))
        assertEquals(OrderStatus.PAYMENT_FAILED, OrderStatus.from("PAYMENT_FAILED"))
    }

    @Test
    fun orderStatus_from_unknownFallsBackToUnderReview() {
        assertEquals(OrderStatus.UNDER_REVIEW, OrderStatus.from("something_else"))
    }

    @Test
    fun address_displayName_fallsBackToLegacyNameField() {
        assertEquals("أحمد", Address(fullName = "أحمد", name = "قديم").displayName)
        assertEquals("قديم", Address(fullName = "", name = "قديم").displayName)
    }

    @Test
    fun cartItem_total_isPriceTimesQuantity() {
        assertEquals(75.0, CartItem(price = 25.0, quantity = 3).total, 0.0001)
    }
}
