package com.asarkar.duel.web

import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.Routing
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.websocket.webSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach

@ExperimentalCoroutinesApi
// See https://github.com/ktorio/ktor-samples/blob/master/app/chat/src/ChatApplication.kt
internal fun Routing.duel(server: DuelServer) {
    // This defines a websocket `/ws` route that allows a protocol upgrade to convert a HTTP request/response request
    // into a bidirectional packetized connection.
    webSocket("/ws") { // this: WebSocketSession ->

        // First of all we get the session.
        val session = call.sessions.get<ClientSession>()

        // We check that we actually have a session. We should always have one,
        // since we have defined an interceptor before to set one.
        if (session == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
            return@webSocket
        }

        // We notify that a member joined by calling the server handler [memberJoin]
        // This allows to associate the session id to a specific WebSocket connection.
        server.memberJoined(session.id, this)

        try {
            // We starts receiving messages (frames).
            // Since this is a coroutine. This coroutine is suspended until receiving frames.
            // Once the connection is closed, this consumeEach will finish and the code will continue.
            incoming.consumeEach { frame ->
                // Frames can be [Text], [Binary], [Ping], [Pong], [Close].
                // We are only interested in textual messages, so we filter it.
                if (frame is Frame.Text) {
                    // Now it is time to process the text sent from the user.
                    // At this point we have context about this connection, the session, the text and the server.
                    // So we have everything we need.
                    server.duel()
                }
            }
        } finally {
            // Either if there was an error, of it the connection was closed gracefully.
            // We notify the server that the member left.
            server.memberLeft(session.id, this)
        }
    }


    // This defines a block of static resources for the '/' path (since no path is specified and we start at '/')
    static {
        // This marks index.html from the 'web' folder in resources as the default file to serve.
        defaultResource("index.html", "web")
        // This serves files from the 'web' folder in the application resources.
        resources("web")
    }
}