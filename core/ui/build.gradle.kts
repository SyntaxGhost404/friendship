plugins {
    alias(libs.plugins.nearfolk.android.library.compose)
}

android {
    namespace = "app.nearfolk.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
}

dependencies {
    implementation(libs.androidx.compose.material3)
}
