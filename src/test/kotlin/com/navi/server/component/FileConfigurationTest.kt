package com.navi.server.component

import com.navi.server.dto.FileResponseDTO
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

@RunWith(SpringRunner::class)
@SpringBootTest
class FileConfigurationTest {
    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

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
        fileService.fileRepository.deleteAll()
    }

    @Test
    fun isConvertingCorrect() {
        val testFileSizeMib: Long = 1024 * 1024 * 2 // 2 Mib
        val testFileSizeKib: Long = 1024 * 4 // 4.0Kib
        val testFileSizeB: Long = 800 //800B
        assertThat(fileConfigurationComponent.convertSize(testFileSizeMib)).isEqualTo("2.0MiB")
        assertThat(fileConfigurationComponent.convertSize(testFileSizeKib)).isEqualTo("4.0KiB")
        assertThat(fileConfigurationComponent.convertSize(testFileSizeB)).isEqualTo("800B")
    }

    @Test
    fun isListingWorks() {
        // At least create one empty file to root
        val fileName: String = "KDRTesting.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
        if (!fileObject.exists()) {
            fileObject.createNewFile()
        }
        // Do work
        val listSize: Long = fileConfigurationComponent.populateInitialDB()

        // Get Results
        val listFile: List<FileResponseDTO> = fileService.findAllDesc()

        // Assert
        assertThat(listFile.size).isEqualTo(listSize)
        assertThat(listFile[0].fileName).isEqualTo(fileObject.absolutePath)
        assertThat(listFile[0].fileType).isEqualTo("File")
    }

    // The False test
    @Test
    fun isListingEmptyWorks() {
        // Do work
        val listSize: Long = fileConfigurationComponent.populateInitialDB()

        // Get Results
        val listFile: List<FileResponseDTO> = fileService.findAllDesc()

        // Assert
        assertThat(listFile.size).isEqualTo(listSize)
    }

    @Test
    fun isTikaWorks() {
        // make test files
        val testRootFolder = File(fileConfigurationComponent.serverRoot, "testTika")
        if(!testRootFolder.exists()) testRootFolder.mkdir()
        val rootPath = testRootFolder.absolutePath
        val childFiles = listOf<File>(
            File(rootPath, "file1.txt"),
            File(rootPath, "file2.css"),
            File(rootPath, "file3.pdf")
        )
        childFiles.forEach {
            if (!it.exists()) {
                it.createNewFile()
            }
        }
        val listSize: Long = fileConfigurationComponent.populateInitialDB()

        // Assert
        val result = fileService.findInsideFiles(fileConfigurationComponent.getSHA256(rootPath))

        val findDto = result.find { it.fileName == childFiles[0].absolutePath }
        if (findDto != null) {
            assertThat(findDto.mimeType).isEqualTo("text/plain")
        }
        else println("ERROR::NOFILE")

        val findDto2 = result.find { it.fileName == childFiles[1].absolutePath }
        if (findDto2 != null) {
            assertThat(findDto2.mimeType).isEqualTo("text/css")
        }
        else println("ERROR::NOFILE")

        val findDto3 = result.find { it.fileName == childFiles[2].absolutePath }
        if (findDto3 != null) {
            assertThat(findDto3.mimeType).isEqualTo("application/pdf")
        }
        else println("ERROR::NOFILE")

    }
}