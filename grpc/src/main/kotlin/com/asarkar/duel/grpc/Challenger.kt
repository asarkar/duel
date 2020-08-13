package com.asarkar.duel.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import io.grpc.Channel as GrpcChannel

private fun <T> SendChannel<T>.offerOrFail(item: T) = check(this.offer(item)) { "Failed to offer item: $item" }

class Challenger constructor(
    private val channel: GrpcChannel
) : Closeable {
    private val LOG: Logger = LoggerFactory.getLogger(Challenger::class.java)

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
        LOG.info("Client shutdown complete")
    }

    suspend fun shoot(shotsFiredAndReceived: SendChannel<Exchange>) {
        val shotsFired = Channel<ShotOrTruce>(UNLIMITED)
        val stub = DuelGrpc.newStub(this.channel)
        val requestObserver = stub.shoot(object : StreamObserver<ShotOrTruce> {
            override fun onNext(msg: ShotOrTruce) {
                shotsFired.offerOrFail(msg)
            }

            override fun onError(t: Throwable) {
                shotsFired.close(t)
            }

            override fun onCompleted() {}
        })
        requestObserver.onNext(ShotOrTruce.newBuilder().setTruce(false).build())
        shotsFiredAndReceived.offerOrFail(Exchange(true, false))

        val ioScope = CoroutineScope(Dispatchers.IO)
        val job = ioScope.launch {
            for (msg in shotsFired) {
                shotsFiredAndReceived.send(Exchange(false, msg.truce))
                var truce = msg.truce

                if (!truce) {
                    truce = Random.nextBoolean()
                    requestObserver.onNext(ShotOrTruce.newBuilder().setTruce(truce).build())
                    shotsFiredAndReceived.send(Exchange(true, truce))
                }

                if (truce) {
                    requestObserver.onCompleted()
                    shotsFired.close()
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
                .also { LOG.debug("{}", false) }
        )
        val stub = DuelGrpcKt.DuelCoroutineStub(channel)

        val shotsReceived = stub.shoot(shotsFired.onEach {
            shotsFiredAndReceived.offerOrFail(Exchange(true, it.truce))
        })
        val ioScope = CoroutineScope(Dispatchers.IO)

        val job = shotsReceived
            .flowOn(Dispatchers.IO)
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
                LOG.debug("{}", `yield`)
                shotsFired.value = ShotOrTruce.newBuilder().setTruce(`yield`).setRand(Random.nextInt()).build()
                if (`yield`) {
                    yield() // Give the last message a chance to be sent
                    ioScope.cancel()
                }
            }
            .launchIn(ioScope)

        job.join()
    }

    suspend fun shootChannel(shotsFiredAndReceived: SendChannel<Exchange>) {
        val shotsFired = Channel<ShotOrTruce>(UNLIMITED)
        val stub = DuelGrpcKt.DuelCoroutineStub(channel)

        shotsFired.offerOrFail(ShotOrTruce.newBuilder().setTruce(false).setRand(Random.nextInt()).build())
            .also { LOG.debug("{}", false) }
        val shotsReceived = stub.shoot(shotsFired.receiveAsFlow()
            .flowOn(Dispatchers.IO)
            .onEach {
                shotsFiredAndReceived.offerOrFail(Exchange(true, it.truce))
            })
        val ioScope = CoroutineScope(Dispatchers.IO)

        val job = shotsReceived
            .flowOn(Dispatchers.IO)
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
                LOG.debug("{}", `yield`)
                shotsFired.offerOrFail(ShotOrTruce.newBuilder().setTruce(`yield`).setRand(Random.nextInt()).build())
                if (`yield`) {
                    yield() // Give the last message a chance to be sent
                    shotsFired.close()
                    ioScope.cancel()
                }
            }
            .launchIn(ioScope)

        job.join()
    }
}