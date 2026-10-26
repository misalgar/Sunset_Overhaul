import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// The provider host is machine-local: it comes from local.properties (gitignored) so the repository
// stays provider-neutral. Without it the release config simply permits no cleartext host.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val providerHostValue = localProperties.getProperty("sunset.provider.host")?.trim().orEmpty()
val tmdbReadToken = localProperties.getProperty("tmdb.read.token")?.trim().orEmpty()
val tmdbApiKey = localProperties.getProperty("tmdb.api.key")?.trim().orEmpty()
val otaManifestUrl = localProperties.getProperty("sunset.ota.manifest")?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "https://raw.githubusercontent.com/misalgar/sunset_overhaul-releases/main/latest.json"
val releaseStoreFile = localProperties.getProperty("sunset.signing.storeFile")?.trim().orEmpty()
val releaseStorePassword = localProperties.getProperty("sunset.signing.storePassword")?.trim().orEmpty()
val releaseKeyAlias = localProperties.getProperty("sunset.signing.keyAlias")?.trim().orEmpty()
val releaseKeyPassword = localProperties.getProperty("sunset.signing.keyPassword")?.trim().orEmpty()
val hasReleaseSigning = releaseStoreFile.isNotEmpty() &&
    releaseStorePassword.isNotEmpty() &&
    releaseKeyAlias.isNotEmpty() &&
    releaseKeyPassword.isNotEmpty()

abstract class GenerateNetworkSecurityConfig : DefaultTask() {
    @get:Input
    abstract val providerHost: Property<String>

    @get:Input
    abstract val allowAllCleartext: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val host = providerHost.get().trim()
        if (host.isNotEmpty() && !host.matches(Regex("[A-Za-z0-9.-]+"))) {
            throw GradleException("sunset.provider.host must be a hostname or IP address")
        }
        val baseConfig = allowAllCleartext.get()
        val domainBlock = if (host.isEmpty() || baseConfig) {
            ""
        } else {
            """
            |    <domain-config cleartextTrafficPermitted="true">
            |        <domain includeSubdomains="true">$host</domain>
            |    </domain-config>
            """.trimMargin()
        }
        val xmlDir = outputDir.get().asFile.resolve("xml").apply { mkdirs() }
        xmlDir.resolve("network_security_config.xml").writeText(
            """
            |<?xml version="1.0" encoding="utf-8"?>
            |<!-- Generated at build time from local.properties; do not edit. -->
            |<network-security-config>
            |    <base-config cleartextTrafficPermitted="$baseConfig" />
            |$domainBlock
            |</network-security-config>
            |
            """.trimMargin(),
        )
    }
}

androidComponents {
    onVariants { variant ->
        val taskName = "generate${variant.name.replaceFirstChar { it.uppercase() }}NetworkSecurityConfig"
        val generateConfig = tasks.register<GenerateNetworkSecurityConfig>(taskName) {
            providerHost.set(providerHostValue)
            // The panel is HTTP-only and redirects to rotating raw-IP hosts. A static allowlist is
            // not viable, and the release APK is sideloaded, so cleartext is permitted app-wide.
            allowAllCleartext.set(true)
            outputDir.set(layout.buildDirectory.dir("generated/res/networkSecurityConfig/${variant.name}"))
        }
        variant.sources.res?.addGeneratedSourceDirectory(
            generateConfig,
            GenerateNetworkSecurityConfig::outputDir,
        )
    }
}

android {
    namespace = "com.markooverhaul.sunset.v2"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.markooverhaul.sunset.v2"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 2
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // Machine-local TMDB credentials; never committed. Empty values disable enrichment.
        buildConfigField("String", "TMDB_READ_TOKEN", "\"${tmdbReadToken.replace("\"", "\\\"")}\"")
        buildConfigField("String", "TMDB_API_KEY", "\"${tmdbApiKey.replace("\"", "\\\"")}\"")
        buildConfigField("String", "OTA_MANIFEST_URL", "\"${otaManifestUrl.replace("\"", "\\\"")}\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Kept off for now so stack traces stay readable during the family/friends phase.
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {
    // Core
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:player"))

    // Features
    implementation(project(":feature:account"))
    implementation(project(":feature:home"))
    implementation(project(":feature:live"))
    implementation(project(":feature:guide"))
    implementation(project(":feature:movies"))
    implementation(project(":feature:series"))
    implementation(project(":feature:details"))
    implementation(project(":feature:search"))
    implementation(project(":feature:mylist"))
    implementation(project(":feature:player"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:parental"))
    implementation(project(":feature:profiles"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.tv.material)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}
