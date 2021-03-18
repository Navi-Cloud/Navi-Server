package com.navi.server.component

import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
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
        fileService.fileRepository.deleteAll()
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
        val listFile: List<FileResponseDTO> = fileService.findAllDesc().body

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
        val listFile: List<FileResponseDTO> = fileService.findAllDesc().body

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
        val result = fileService.findInsideFiles(fileService.getSHA256(rootPath)).body

        val findDto = result.find { it.fileName == childFiles[0].absolutePath }
        findDto?.let { assertThat(findDto.mimeType).isEqualTo("text/plain") } ?: throw Exception("ERROR::NOFILE")

        val findDto2 = result.find { it.fileName == childFiles[1].absolutePath }
        findDto2?.let { assertThat(findDto2.mimeType).isEqualTo("text/css") } ?: throw Exception("ERROR::NOFILE")

        val findDto3 = result.find { it.fileName == childFiles[2].absolutePath }
        findDto3?.let { assertThat(findDto3.mimeType).isEqualTo("application/pdf") } ?: throw Exception("ERROR::NOFILE")
    }

    @Test
    fun deleteByTokenWorksWell() {
        // Let
        val tmpPath: File = File(System.getProperty("java.io.tmpdir"), "naviTesting.txt")
        val targetSHA256: String = fileService.getSHA256(tmpPath.absolutePath)
        fileService.save(
            FileSaveRequestDTO(
                //id = 50,
                fileName = tmpPath.name,
                fileType = "File",
                mimeType = "mime/application/json",
                token = targetSHA256,
                prevToken = fileService.getSHA256(tmpPath.parent),
                lastModifiedTime = 5000,
                fileCreatedDate = "createdDateTest",
                fileSize = "50B"
            )
        )
        val beforeId: Long = fileService.fileRepository.count()

        // do work
        fileService.deleteByToken(targetSHA256)
        println("BeforeID: $beforeId, Actual: ${fileService.fileRepository.count()}")

        // Assert!
        assertThat(fileService.fileRepository.count()).isEqualTo(beforeId-1)
    }

    @Test
    fun findByTokenWorksWell() {
        // Let
        val tmpPath: File = File(System.getProperty("java.io.tmpdir"), "naviTesting.txt")
        val targetSHA256: String = fileService.getSHA256(tmpPath.absolutePath)
        fileService.save(
            FileSaveRequestDTO(
                //id = 50,
                fileName = tmpPath.name,
                fileType = "File",
                mimeType = "mime/application/json",
                token = targetSHA256,
                prevToken = fileService.getSHA256(tmpPath.parent),
                lastModifiedTime = 5000,
                fileCreatedDate = "createdDateTest",
                fileSize = "50B"
            )
        )

        // do work
        val responseDto: FileResponseDTO = fileService.findByToken(targetSHA256)

        // Assert
        assertThat(responseDto.fileName).isEqualTo(tmpPath.name)
        assertThat(responseDto.fileType).isEqualTo("File")
    }
}