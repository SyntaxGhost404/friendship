import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("androidx.room")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }
        dependencies {
            add("api", libs.lib("androidx-room-runtime"))
            add("api", libs.lib("androidx-room-ktx"))
            add("implementation", libs.lib("androidx-room-paging"))
            add("ksp", libs.lib("androidx-room-compiler"))
        }
    }
}
