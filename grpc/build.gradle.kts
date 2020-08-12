import com.google.protobuf.gradle.*

plugins {
    id("com.google.protobuf") version Plugins.PROTOBUF_VERSION
    kotlin("jvm")
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:${Dependencies.GRPC_VERSION}"))
    api("io.grpc:grpc-api:${Dependencies.GRPC_VERSION}")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    runtimeOnly("io.grpc:grpc-netty")
    implementation("jakarta.annotation:jakarta.annotation-api:${Dependencies.JAKARATA_ANNOTATION_API_VERSION}")
    implementation("com.google.protobuf:protobuf-java:${Dependencies.PROTOBUF_VERSION}")
    implementation("io.grpc:grpc-kotlin-stub:${Dependencies.GRPC_KOTLIN_VERSION}")
    testImplementation("com.asarkar.grpc:grpc-test:${Dependencies.GRPC_TEST_VERSION}")
    testImplementation("io.grpc:grpc-core")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")
    testImplementation(kotlin("reflect"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Dependencies.PROTOBUF_VERSION}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Dependencies.GRPC_VERSION}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Dependencies.GRPC_KOTLIN_VERSION}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs(
                file("build/generated/source/proto/main")
                    .walkTopDown()
                    .maxDepth(1)
                    .asIterable()
            )
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xopt-in=kotlin.time.ExperimentalTime")
    }
}