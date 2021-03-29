package com.navi.server.component

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.nio.file.Path
import java.nio.file.Paths

@SpringBootTest
@RunWith(SpringRunner::class)
class FilePathResolverTest {
    @Autowired
    private lateinit var filePathResolver: FilePathResolver

    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    @Test
    fun is_convertPhysicsPathToServerPath_works_well() {
        val user: String = "KangDroid"
        val physicalPath: Path = Paths.get(fileConfigurationComponent.serverRoot, user, "test.txt")
        val expected: String = "/test.txt" // Sever value. Should be unix-path.
        val returnValue: String = filePathResolver.convertPhysicsPathToServerPath(physicalPath.toString(), user)

        assertThat(returnValue).isEqualTo(expected)
    }

    @Test
    fun is_convertPhysicsPathToPrevServerPath_works_well_root() {
        val user: String = "KangDroid"
        val physicalPath: Path = Paths.get(fileConfigurationComponent.serverRoot, user, "test.txt")
        val expected: String = "/" // Sever value. Should be unix-path.
        val returnValue: String = filePathResolver.convertPhysicsPathToPrevServerPath(physicalPath.toString(), user)

        assertThat(returnValue).isEqualTo(expected)
    }

    @Test
    fun is_convertPhysicsPathToPrevServerPath_works_well_nonroot() {
        val user: String = "KangDroid"
        val physicalPath: Path = Paths.get(fileConfigurationComponent.serverRoot, user, "test", "test.txt")
        val expected: String = "/test" // Sever value. Should be unix-path.
        val returnValue: String = filePathResolver.convertPhysicsPathToPrevServerPath(physicalPath.toString(), user)

        assertThat(returnValue).isEqualTo(expected)
    }
}