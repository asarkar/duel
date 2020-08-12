package com.asarkar.duel.grpc

import com.google.common.util.concurrent.MoreExecutors
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.multiton
import java.util.concurrent.Executor

data class ServerParams(val service: BindableService, val executor: Executor = MoreExecutors.directExecutor())
data class ChannelParams(val executor: Executor = MoreExecutors.directExecutor())

val testDI = DI {
    bind<ManagedChannel>() with multiton { params: ChannelParams ->
        InProcessChannelBuilder
            .forName("duel")
            .executor(params.executor)
            .build()
    }

    bind<Server>() with multiton { params: ServerParams ->
        InProcessServerBuilder
            .forName("duel")
            .addService(params.service)
            .executor(params.executor)
            .build()
    }
}