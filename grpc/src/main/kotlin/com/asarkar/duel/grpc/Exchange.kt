package com.asarkar.duel.grpc

import java.io.Serializable

data class Exchange(val challenger: Boolean, val truce: Boolean) : Serializable {
    fun toJson(): String = """{"challenger": $challenger, "truce": $truce}"""
}