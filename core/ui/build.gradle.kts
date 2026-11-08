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
    api(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}

dependencies {
    implementation(libs.androidx.compose.material3)
}
