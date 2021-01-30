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
}