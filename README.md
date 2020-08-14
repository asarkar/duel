duel
===

[![](https://github.com/asarkar/duel/workflows/CI%20Pipeline/badge.svg)](https://github.com/asarkar/duel/actions?query=workflow%3A%22CI+Pipeline%22)

Simulates a [duel](https://en.wikipedia.org/wiki/Duel) and streams live updates. Live: https://asarkar-duel.herokuapp.com/

### Technologies Used

1. [Ktor](https://ktor.io/): For server-side Websocket and session.
2. [Kodein-DI](https://github.com/Kodein-Framework/Kodein-DI): For KOtlin DEpendency INjection.
3. [grpc-kotlin](https://github.com/grpc/grpc-kotlin): For bidirectional gRPC streaming modelled as Kotlin `Flow`.
4. [Vue.js](https://vuejs.org/): For client UI.

### Running Locally

Run `./gradlew clean run` and go to the URL shown on the console once the application has started.

To run from inside IntelliJ, create a new Run Configuration as shown below:

![DuelApp](DuelApp.jpg)

### Deploying to Heroku

1. Commit all changes locally.
2. `heroku login`
3. `git push heroku master`
4. `heroku logs --tail`
5. `heroku open`

See [this](https://help.heroku.com/PBGP6IDE/how-should-i-generate-an-api-key-that-allows-me-to-use-the-heroku-platform-api) 
article about how to generate an API key. This key can then be set as `HEROKU_API_KEY` environment variable.

### Distributed Tracing using OpenTelemetry and Jaeger

[opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) works OOTB.

**Steps:**

1. Download [opentelemetry-javaagent-all.jar](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent-all.jar).
2. Start Jaegar Docker container locally:
   ```
   docker run --rm -it --name jaeger \
   -p 16686:16686 \
   -p 14250:14250 \
   jaegertracing/all-in-one
   ```
3. Start application:
   ```
   java -javaagent:/path/to/opentelemetry-javaagent-all.jar \
   -Dotel.exporter=jaeger \
   -Dotel.jaeger.service.name=duel \
   -Dotel.jaeger.endpoint=localhost:14250 \
   -jar build/libs/duel.jar
   ```
4. Go to http://0.0.0.0:8080/ and click on Start button.
5. Go to http://localhost:16686/ and search for traces.