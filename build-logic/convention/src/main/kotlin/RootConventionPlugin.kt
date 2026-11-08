import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Registers project wide verification tasks on the root project.
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        tasks.register("verifyCopyPunctuation", VerifyCopyPunctuationTask::class.java) {
            group = "verification"
            description = "Fails when any source or resource file contains an em dash or an en dash."
            sources.from(
                fileTree(rootDir) {
                    include("**/src/**/*.kt", "**/src/**/res/**/*.xml")
                    exclude("**/build/**", "build-logic/**")
                },
            )
            reportFile.set(layout.buildDirectory.file("reports/copy/punctuation.txt"))
        }
        Unit
    }
}

/**
 * The interface copy rule bans the em dash (U+2014) and the en dash (U+2013).
 * Hyphens remain allowed. The check runs on Kotlin sources too, because seeded
 * content and previews live there.
 */
abstract class VerifyCopyPunctuationTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val banned = charArrayOf('\u2014', '\u2013')
        val offenders = mutableListOf<String>()
        sources.files.sorted().forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (line.indexOfAny(banned) >= 0) offenders += "${file.path}:${index + 1}: ${line.trim()}"
            }
        }
        val report = reportFile.get().asFile
        report.parentFile.mkdirs()
        report.writeText(if (offenders.isEmpty()) "No banned dashes found.\n" else offenders.joinToString("\n"))
        if (offenders.isNotEmpty()) {
            throw GradleException("Banned dash characters found:\n" + offenders.joinToString("\n"))
        }
    }
}
