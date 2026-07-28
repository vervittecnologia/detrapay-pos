package com.detrapay.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class ModeFreeDomainArchitectureTest {
    @Test
    fun `domain session and home state contain no application modes`() {
        val roots = listOf(
            File("src/main/java/com/detrapay/data/model"),
            File("src/main/java/com/detrapay/data/repositories/AuthRepository.kt"),
            File("src/main/java/com/detrapay/data/repositories/LoginRepository.kt"),
            File("src/main/java/com/detrapay/ui/home/HomeState.kt"),
            File("src/main/java/com/detrapay/ui/home/HomeViewModel.kt"),
            File("src/main/java/com/detrapay/ui/home/Home" + "Mode" + "Router.kt"),
        )
        val forbidden = listOf(
            "Seller" + "App" + "Mode",
            "app" + "Mo" + "de",
            "seller" + "App" + "Mode",
            "Home" + "Mode" + "Router",
        )
        val source = roots.flatMap { root ->
            when {
                !root.exists() -> emptyList()
                root.isFile -> listOf(root)
                else -> root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
            }
        }.joinToString("\n") { it.readText() }

        forbidden.forEach { token ->
            assertFalse("Found forbidden mode token: $token", source.contains(token))
        }
    }
}
