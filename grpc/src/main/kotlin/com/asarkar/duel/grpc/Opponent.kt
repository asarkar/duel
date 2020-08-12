package com.asarkar.duel.grpc

import io.grpc.stub.StreamObserver
import kotlin.random.Random

class Opponent : DuelGrpc.DuelImplBase() {
    override fun shoot(responseObserver: StreamObserver<ShotOrTruce>): StreamObserver<ShotOrTruce> {
        return object : StreamObserver<ShotOrTruce> {
            override fun onNext(msg: ShotOrTruce) {
                if (!msg.truce) {
                    val `yield` = Random.nextBoolean()
                    responseObserver.onNext(ShotOrTruce.newBuilder().setTruce(`yield`).build())
                }
            }

            override fun onError(t: Throwable) {
                responseObserver.onError(t)
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
            }
        }
    }
}