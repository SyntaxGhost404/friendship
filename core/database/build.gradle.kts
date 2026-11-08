plugins {
    alias(libs.plugins.nearfolk.android.library)
    alias(libs.plugins.nearfolk.android.hilt)
    alias(libs.plugins.nearfolk.android.room)
}

android {
    namespace = "app.nearfolk.core.database"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.paging.runtime)
    implementation(libs.kotlinx.coroutines.android)
}
