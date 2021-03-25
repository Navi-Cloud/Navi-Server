package com.navi.server.component

import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.service.FileService
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@RunWith(SpringRunner::class)
@SpringBootTest
class FileConfigurationTest {
    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var fileService: FileService

    private lateinit var trashRootObject: File

    @Before
    fun initEnvironment() {
        // Create trash directory
        trashRootObject = File(fileConfigurationComponent.serverRoot)
        trashRootObject.mkdir()
    }

    @After
    fun destroyEnvironment() {
        if (trashRootObject.exists()) {
            trashRootObject.deleteRecursively()
        }
        userTemplateRepository.clearAll()
    }

    @Test
    fun is_getMimeType_returns_file_error() {
        assertThat(fileConfigurationComponent.getMimeType(File("")))
            .isEqualTo("File")
    }

    @Test
    fun is_getMimeType_returns_correctly() {
        val tmpFileObject: File = File(System.getProperty("java.io.tmpdir"), "test.txt")
        tmpFileObject.writeText("Test!")
        assertThat(fileConfigurationComponent.getMimeType(tmpFileObject))
            .isEqualTo("text/plain")
    }

    @Test
    fun is_initStructure_works_well_empty_users() {
        // Do
        fileConfigurationComponent.initStructure()
        val fileList: List<File> = trashRootObject.walk().toList()

        assertThat(fileList.size).isEqualTo(1L)
    }

    @Test
    fun is_initStructure_works_well_single_users() {
        userTemplateRepository.save(
            User(
                userName = "KangDroid",
                userPassword = "testingPassword"
            )
        )

        fileConfigurationComponent.initStructure()

        val fileList: List<File> = trashRootObject.walk().toList()
        assertThat(fileList.size).isEqualTo(2L)

        val list: List<User> = userTemplateRepository.findAll()
        assertThat(list.size).isEqualTo(1L)
        assertThat(list[0].fileList.size).isEqualTo(1L)
        assertThat(list[0].fileList[0].fileName).isEqualTo("/")
    }

    @Test
    fun is_initStructure_works_well_single_users_multi_file() {
        userTemplateRepository.save(
            User(
                userName = "KangDroid",
                userPassword = "testingPassword"
            )
        )
        val userRootFile: File = File(fileConfigurationComponent.serverRoot, "KangDroid")
        userRootFile.mkdir()
        val testFile: File = File(userRootFile.absolutePath, "test.txt")
        testFile.writeText("TESTING!")


        fileConfigurationComponent.initStructure()

        val fileList: List<File> = trashRootObject.walk().toList()
        assertThat(fileList.size).isEqualTo(3L)

        val list: List<User> = userTemplateRepository.findAll()
        assertThat(list.size).isEqualTo(1L)
        assertThat(list[0].fileList.size).isEqualTo(2L)
        assertThat(list[0].fileList[0].fileName).isEqualTo("/")
        assertThat(list[0].fileList[1].fileName).isEqualTo("/test.txt")
        assertThat(list[0].fileList[1].prevToken).isEqualTo(fileService.getSHA256("/"))
    }

    @Test
    fun is_initStructure_works_well_single_users_multiBig_file() {
        userTemplateRepository.save(
            User(
                userName = "KangDroid",
                userPassword = "testingPassword"
            )
        )
        val path: Path = Paths.get(fileConfigurationComponent.serverRoot, "KangDroid", "testing")
        val userRootFile: File = path.toFile()
        userRootFile.mkdirs()
        val testFile: File = File(userRootFile.absolutePath, "test.txt")
        testFile.writeText("TESTING!")


        fileConfigurationComponent.initStructure()

        val fileList: List<File> = trashRootObject.walk().toList()
        assertThat(fileList.size).isEqualTo(4L)

        val list: List<User> = userTemplateRepository.findAll()
        assertThat(list.size).isEqualTo(1L)
        assertThat(list[0].fileList.size).isEqualTo(3L)
        assertThat(list[0].fileList[0].fileName).isEqualTo("/")
        assertThat(list[0].fileList[2].fileName).isEqualTo("/testing/test.txt")
        assertThat(list[0].fileList[2].prevToken).isEqualTo(fileService.getSHA256("/testing"))
    }

    @Test
    fun is_initStructure_works_well_multi_users() {
        userTemplateRepository.save(
            User(
                userName = "KangDroid",
                userPassword = "testingPassword"
            )
        )

        userTemplateRepository.save(
            User(
                userName = "KangDroid2",
                userPassword = "testingPassword"
            )
        )

        fileConfigurationComponent.initStructure()

        val fileList: List<File> = trashRootObject.walk().toList()
        assertThat(fileList.size).isEqualTo(3L)

        val list: List<User> = userTemplateRepository.findAll()
        assertThat(list.size).isEqualTo(2L)
        assertThat(list[0].fileList.size).isEqualTo(1L)
        assertThat(list[0].fileList[0].fileName).isEqualTo("/")

        assertThat(list[1].fileList.size).isEqualTo(1L)
        assertThat(list[1].fileList[0].fileName).isEqualTo("/")
    }
}