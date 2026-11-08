import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ApplicationExtension> {
            compileSdk = BuildConfigValues.COMPILE_SDK
            defaultConfig {
                minSdk = BuildConfigValues.MIN_SDK
                targetSdk = BuildConfigValues.TARGET_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables.useSupportLibrary = true
            }
            compileOptions {
                sourceCompatibility = BuildConfigValues.JAVA_VERSION
                targetCompatibility = BuildConfigValues.JAVA_VERSION
            }
            buildFeatures {
                compose = true
                buildConfig = true
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
            }
            packaging {
                resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*")
            }
        }
        configureKotlinCompiler()
        dependencies {
            add("implementation", platform(libs.lib("androidx-compose-bom")))
        }
    }
}
