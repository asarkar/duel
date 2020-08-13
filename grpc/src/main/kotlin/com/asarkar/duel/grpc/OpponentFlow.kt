package com.asarkar.duel.grpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlin.random.Random

class OpponentFlow : DuelGrpcKt.DuelCoroutineImplBase() {
    @ExperimentalCoroutinesApi
    override fun shoot(requests: Flow<ShotOrTruce>): Flow<ShotOrTruce> {
        val shotsFired = MutableStateFlow(
            ShotOrTruce.newBuilder().setTruce(false).setRand(-1).build()
        )

        val coroutineScope = CoroutineScope(Dispatchers.IO)
        requests
            .flowOn(Dispatchers.IO)
            .takeWhile { !it.truce }
            .onEach {
                val `yield` = Random.nextBoolean()
                shotsFired.value =
                    ShotOrTruce.newBuilder()
                        .setTruce(`yield`).setRand(Random.nextInt(1, Int.MAX_VALUE)).build()
            }
            .launchIn(coroutineScope)
        return shotsFired
            .filter { it.rand > 0 }
            .onEach { if (it.truce) coroutineScope.cancel() }
    }
}