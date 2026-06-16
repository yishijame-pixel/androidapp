plugins {
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
}

sourceSets {
    main {
        kotlin {
            srcDir("../app/src/main/java/com/example/funlife/social/game/engine/pacmaze")
            exclude(
                "PacMazeLevelSource.kt",
                "PacMazeOnlineLoader.kt",
                "PacMazeBoardSync.kt",
                "PacMazeOnlineInput.kt",
                "PacMazeStateSnapshot.kt",
                "PacMazeEloCalculator.kt",
                "PacMazeMazeMode.kt",
                "PacMazeMazeRunProfile.kt",
                "PacMazeMazeGenerator.kt",
                "PacMazeInputController.kt",
                "PacMazeInputBuffer.kt",
                "PacMazeJoystickMapper.kt",
                "PacMazeRawJoystickSample.kt",
            )
        }
        kotlin.srcDir("src/main/kotlin")
        resources.srcDir("../app/src/main/assets/pac_maze/arenas")
    }
    test {
        kotlin.srcDir("src/test/kotlin")
        resources.srcDir("../app/src/main/assets/pac_maze/arenas")
    }
}

tasks.test {
    useJUnit()
}
