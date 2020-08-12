import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("jvm") version Plugins.KOTLIN_VERSION
}

allprojects {
    group = "com.asarkar"
    version = "1.0-SNAPSHOT"

    repositories {
        jcenter()
    }
}

subprojects {
    configurations.all {
        afterEvaluate {
            if (isCanBeResolved) {
                attributes {
                    attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
                }
            }
        }
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "org.jetbrains.kotlinx" -> useVersion(when(requested.name) {
                    "atomicfu" -> requested.version // Dependencies.KOTLINX_ATOMICFU_VERSION
                    "kotlinx-coroutines-core-common" -> requested.version // Dependencies.KOTLINX_COROUTINES_CORE_COMMON_VERSION
                    else -> Dependencies.KOTLINX_COROUTINES_VERSION
                }!!)
                "org.jetbrains.kotlin" -> useVersion(Plugins.KOTLIN_VERSION)
            }
        }
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11

            dependencies {
                attributesSchema {
                    attribute(KotlinPlatformType.attribute)
                }
                implementation(kotlin("stdlib-jdk8"))
                implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Dependencies.KOTLINX_COROUTINES_VERSION}"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
                implementation("org.slf4j:slf4j-api:${Dependencies.SLF4J_VERSION}")
                implementation("org.kodein.di:kodein-di:${Dependencies.KODEIN_VERSION}")
                runtimeOnly("ch.qos.logback:logback-classic:${Dependencies.LOGBACK_VERSION}")
                testImplementation(platform("org.junit:junit-bom:${Dependencies.JUNIT_VERSION}"))
                testImplementation("org.junit.jupiter:junit-jupiter")
                testImplementation("org.assertj:assertj-core:${Dependencies.ASSERTJ_VERSION}")
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
            jvmTarget = "11"
        }
    }
}