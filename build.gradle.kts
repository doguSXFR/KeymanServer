plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.wave"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
        name = "jitpack"
    }
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_22)
        localImageName.set("keyman-server")
        imageTag.set("0.0.1-preview")
        portMappings.set(
            listOf(
                io.ktor.plugin.features.DockerPortMapping(
                    5294,
                    9999,
                    io.ktor.plugin.features.DockerPortMappingProtocol.TCP
                )
            )
        )
    }
}

dependencies {
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.simple.cache)
    implementation(libs.ktor.simple.redis.cache)
    implementation(libs.ktor.server.websockets)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.cors)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.netty)
    implementation(libs.hikari.cp)
    implementation(libs.logback.classic)
    implementation(libs.bouncycastle)
    implementation(libs.mysql.connector.java)
    implementation(libs.postgresql)
    implementation(libs.spring.security.core)
    implementation(libs.hayden.khealth)
    implementation("com.impossibl.pgjdbc-ng", "pgjdbc-ng", "0.8.9")
    implementation(libs.ktor.server.request.validation)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
