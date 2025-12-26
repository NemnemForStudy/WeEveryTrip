package com.example.travelapp.data.model

import com.google.gson.annotations.SerializedName

data class WithdrawResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String
)