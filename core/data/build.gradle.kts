plugins {
    alias(libs.plugins.nearfolk.android.library)
    alias(libs.plugins.nearfolk.android.hilt)
}

android {
    namespace = "app.nearfolk.core.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(libs.androidx.paging.runtime)
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.turbine)
}

dependencies {
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
