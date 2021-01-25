package com.navi.server.component

import com.navi.server.dto.FileResponseDTO
import com.navi.server.service.FileService
import org.assertj.core.api.Assertions.assertThat
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

    @Test
    fun isListingWorks() {
        // Create trash directory
        val trashRootObject: File = File(fileConfigurationComponent.serverRoot)
        trashRootObject.mkdir()

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

        if (trashRootObject.exists()) {
            trashRootObject.deleteRecursively()
        }
    }
}