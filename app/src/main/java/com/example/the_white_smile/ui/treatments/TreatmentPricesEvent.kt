package com.example.the_white_smile.ui.treatments

sealed class TreatmentPricesEvent {
    data class Message(val messageRes: Int) : TreatmentPricesEvent()
    data class Error(val cause: Throwable?) : TreatmentPricesEvent()
}
