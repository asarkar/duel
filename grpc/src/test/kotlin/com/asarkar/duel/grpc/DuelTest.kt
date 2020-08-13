package com.asarkar.duel.grpc

import com.asarkar.grpc.test.GrpcCleanupExtension
import com.asarkar.grpc.test.Resources
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.debug.DebugProbes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.kodein.di.factory
import java.util.stream.Stream
import kotlin.reflect.KSuspendFunction2
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExtendWith(GrpcCleanupExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DuelTest {
    private lateinit var resources: Resources

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("funProvider")
    fun testShoot(
        fn: KSuspendFunction2<Challenger, SendChannel<Exchange>, Unit>,
        svc: BindableService,
        unused: String
    ) {
        val name: String = InProcessServerBuilder.generateName()
        val serverFactory: (ServerParams) -> Server by testDI.factory()
        val server = serverFactory(ServerParams(name, svc))
        resources.register(server)
        server.start()

        val channelFactory: (ChannelParams) -> ManagedChannel by testDI.factory()
        val grpcChannel = channelFactory(ChannelParams(name))
        resources.register(grpcChannel)
        val client = Challenger(grpcChannel)

        val channel = Channel<Exchange>(UNLIMITED)
        runBlocking { fn(client, channel) }

        val exchanges = mutableListOf<Exchange>()
        while (!channel.isEmpty) {
            exchanges.add(channel.poll()!!)
        }
        val (challenger, opponent) = exchanges
            .partition { it.challenger }
        channel.close()

        assertThat(challenger).hasSizeGreaterThanOrEqualTo(1)
        assertThat(opponent).hasSizeGreaterThanOrEqualTo(1)

        if (challenger.size == opponent.size) {
            assertThat(challenger).allMatch { !it.truce }
            assertThat(opponent).anyMatch { it.truce }
        } else {
            assertThat(opponent).allMatch { !it.truce }
            assertThat(challenger).anyMatch { it.truce }
        }
    }

    companion object {
        @JvmStatic
        fun funProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Challenger::shoot, Opponent(), "shoot"),
                Arguments.of(Challenger::shootFlow, OpponentFlow(), "shootFlow"),
                Arguments.of(Challenger::shootChannel, OpponentFlow(), "shootChannel")
            )
        }
    }

    @ExperimentalCoroutinesApi
    @ExperimentalTime
    fun <T> CoroutineScope.debugWithTimeout(
        timeout: Duration,
        block: suspend () -> T
    ) = launch {
        DebugProbes.install()
        val deferred = async { block() }
        delay(timeout)
        DebugProbes.dumpCoroutines()
        println("\nDumping only deferred")
        DebugProbes.printJob(deferred)
        DebugProbes.uninstall()
    }
}