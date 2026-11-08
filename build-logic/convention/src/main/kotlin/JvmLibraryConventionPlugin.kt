import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = BuildConfigValues.JAVA_VERSION
            targetCompatibility = BuildConfigValues.JAVA_VERSION
        }
        configureKotlinCompiler()
        dependencies {
            add("testImplementation", libs.lib("junit"))
        }
    }
}
