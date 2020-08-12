package com.asarkar.duel.grpc

import com.google.common.util.concurrent.MoreExecutors
import io.grpc.BindableService
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.multiton
import java.util.concurrent.Executor
import kotlin.concurrent.thread

data class ServerParams(val service: BindableService, val executor: Executor = MoreExecutors.directExecutor())
data class ClientParams(val port: Int, val executor: Executor = MoreExecutors.directExecutor())

val di = DI {
    bind<Challenger>() with multiton { params: ClientParams ->
        Challenger(params.port)
    }

    bind<DuelServer>() with multiton { _: ServerParams ->
        val server = DuelServer()
        thread(block = { server.blockingStart() })
        check(server.blockUntilReady(2000)) { "Server is not ready" }
        server
    }
}