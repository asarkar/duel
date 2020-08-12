package com.asarkar.duel.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import io.grpc.Channel as GrpcChannel

private fun <T> SendChannel<T>.offerOrFail(item: T) = check(this.offer(item)) { "Failed to offer item: $item" }

class Challenger constructor(
    private val channel: GrpcChannel
) : Closeable {
    constructor(port: Int) : this(
        ManagedChannelBuilder.forAddress("localhost", port)
            .usePlaintext()
            .build()
    )

    init {
        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    override fun close() {
        if (channel is ManagedChannel) {
            channel.shutdown()
                .awaitTermination(2, TimeUnit.SECONDS)
                .also { if (!it) channel.shutdownNow() }
        }
    }

    suspend fun shoot(shotsFiredAndReceived: SendChannel<Exchange>) {
        val channel = Channel<ShotOrTruce>(UNLIMITED)
        val stub = DuelGrpc.newStub(this.channel)
        val requestObserver = stub.shoot(object : StreamObserver<ShotOrTruce> {
            override fun onNext(msg: ShotOrTruce) {
                channel.offerOrFail(msg)
            }

            override fun onError(t: Throwable) {
                channel.close(t)
            }

            override fun onCompleted() {}
        })
        requestObserver.onNext(ShotOrTruce.newBuilder().setTruce(false).build())
        shotsFiredAndReceived.offerOrFail(Exchange(true, false))

        val ioScope = CoroutineScope(Dispatchers.IO)
        val job = ioScope.launch {
            for (msg in channel) {
                shotsFiredAndReceived.send(Exchange(false, msg.truce))
                var truce = msg.truce

                if (!truce) {
                    truce = Random.nextBoolean()
                    requestObserver.onNext(ShotOrTruce.newBuilder().setTruce(truce).build())
                    shotsFiredAndReceived.send(Exchange(true, truce))
                }

                if (truce) {
                    requestObserver.onCompleted()
                    channel.close()
                    ioScope.cancel()
                }
            }
        }
        job.join()
    }

    @ExperimentalCoroutinesApi
    suspend fun shootFlow(shotsFiredAndReceived: SendChannel<Exchange>) {
        val shotsFired = MutableStateFlow(
            ShotOrTruce.newBuilder().setTruce(false).setRand(Random.nextInt()).build()
        )
        val stub = DuelGrpcKt.DuelCoroutineStub(channel)

        val shotsReceived = stub.shoot(shotsFired.onEach {
            shotsFiredAndReceived.offerOrFail(Exchange(true, it.truce))
        })
        val coroutineScope = CoroutineScope(Job())

        val job = shotsReceived
            .onEach {
                shotsFiredAndReceived.offerOrFail(
                    Exchange(
                        false,
                        it.truce
                    )
                )
            }
            .takeWhile { !it.truce }
            .onEach {
                val `yield` = Random.nextBoolean()
                shotsFired.value = ShotOrTruce.newBuilder().setTruce(`yield`).setRand(Random.nextInt()).build()
            }
            .launchIn(coroutineScope)

        job.join()
        coroutineScope.cancel()
    }
}