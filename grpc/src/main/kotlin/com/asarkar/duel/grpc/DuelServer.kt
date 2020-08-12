package com.asarkar.duel.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.properties.Delegates


class DuelServer {
    private val LOG: Logger = LoggerFactory.getLogger(DuelServer::class.java)
    var port by Delegates.notNull<Int>()
    private lateinit var server: Server

    fun blockingStart() {
        port = ServerSocket(0).let {
            val tmp = it.localPort
            it.close()
            tmp
        }

        server = ServerBuilder.forPort(port)
            .addService(Opponent())
            .build()
        Runtime.getRuntime().addShutdownHook(Thread { stop() })
        server.start()
        server.awaitTermination()
    }

    fun blockUntilReady(timeout: Int): Boolean {
        val start = Instant.now()
        var ready = false
        while (!ready && abs(Duration.between(Instant.now(), start).toMillis()) < timeout) {
            Socket().use { socket ->
                try {
                    socket.connect(InetSocketAddress("localhost", port), 100)
                    LOG.info("Server listening on port: {}", server.port)
                    ready = true
                } catch (ex: Exception) {
                }
            }
        }

        return ready
    }

    private fun stop() {
        server.shutdown()
            .awaitTermination(2, TimeUnit.SECONDS)
        LOG.info("Server shutdown complete")
    }
}