plugins {
    alias(libs.plugins.nearfolk.android.library)
    alias(libs.plugins.nearfolk.android.hilt)
}

android {
    namespace = "app.nearfolk.core.datastore"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
