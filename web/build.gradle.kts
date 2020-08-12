plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:${Dependencies.KTOR_VERSION}"))
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-websockets")
    implementation(project(":grpc"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xopt-in=io.ktor.util.KtorExperimentalAPI")
    }
}

application {
    mainClass.set("io.ktor.server.netty.DevelopmentEngine")
}