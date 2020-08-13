package com.asarkar.duel.grpc

import io.grpc.*
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.grpc.Metadata as GRPCMetadata

class LoggingInterceptor : ClientInterceptor {
    private val LOG: Logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions?,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions!!.withoutWaitForReady())
        ) {
            override fun sendMessage(req: ReqT) {
                LOG.debug("Challenger: {}", (req as ShotOrTruce).truce)

                super.sendMessage(req)
            }

            override fun start(responseListener: Listener<RespT>, headers: GRPCMetadata) {
                val listener: Listener<RespT> = object : ForwardingClientCallListener<RespT>() {
                    override fun delegate(): Listener<RespT> {
                        return responseListener
                    }

                    override fun onMessage(resp: RespT) {
                        LOG.debug("Opponent: {}", (resp as ShotOrTruce).truce)

                        super.onMessage(resp)
                    }
                }
                super.start(listener, headers)
            }
        }
    }
}