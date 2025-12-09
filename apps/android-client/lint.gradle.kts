// Linting configuration for Android project
tasks.register("ktlint", JavaExec::class) {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = configurations.getByName("ktlint")
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt")
}

tasks.register("ktlintFormat", JavaExec::class) {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = configurations.getByName("ktlint")
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", "src/**/*.kt")
}

configurations {
    create("ktlint")
}

dependencies {
    "ktlint"("com.pinterest:ktlint:0.50.0")
}

