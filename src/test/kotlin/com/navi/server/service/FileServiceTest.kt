package com.navi.server.service

import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class FileServiceTest {
    @Autowired
    private lateinit var fileRepository: FileRepository

    @Autowired
    private lateinit var fileService: FileService

    @After
    fun clearDB() {
        fileRepository.deleteAll()
    }

    @Test
    fun isSavingWorks() {
        // Let
        val fileNameI: String = "TESTING_FILENAME"
        val fileTypeI: String = "Folder"
        val nextTokenI: String = "TEST_TOKEN"
        val lastModifiedTimeI: String = "TEST_TIME"

        // save it to DB with fileService
        val retId: Long = fileService.save(
            FileSaveRequestDTO(
                id = 0,
                fileName = fileNameI,
                fileType = fileTypeI,
                nextToken = nextTokenI,
                lastModifiedTime = lastModifiedTimeI
            )
        )

        // Get results from repository
        val results: FileEntity = fileRepository.findById(retId).get()

        // Assert
        with (results) {
            assertThat(fileName).isEqualTo(fileNameI)
            assertThat(fileType).isEqualTo(fileTypeI)
            assertThat(nextToken).isEqualTo(nextTokenI)
            assertThat(lastModifiedTime).isEqualTo(lastModifiedTimeI)
        }
    }

    @Test
    fun isEmptyDescWorks() {
        val listFile: List<FileResponseDTO> = fileService.findAllDesc()

        assertThat(listFile.size).isEqualTo(0)
    }

    @Test
    fun isFindAllDescWorks() {
        // Let
        val fileNameI: String = "TESTING_FILENAME"
        val fileTypeI: String = "Folder"
        val nextTokenI: String = "TEST_TOKEN"
        val lastModifiedTimeI: String = "TEST_TIME"

        // save it to DB with fileService
        val retId: Long = fileService.save(
            FileSaveRequestDTO(
                id = 0,
                fileName = fileNameI,
                fileType = fileTypeI,
                nextToken = nextTokenI,
                lastModifiedTime = lastModifiedTimeI
            )
        )

        // Get Results from fileService
        val listFile: List<FileResponseDTO> = fileService.findAllDesc()

        // Assert
        assertThat(listFile.size).isEqualTo(1)
        with (listFile[0]) {
            assertThat(fileName).isEqualTo(fileNameI)
            assertThat(fileType).isEqualTo(fileTypeI)
            assertThat(nextToken).isEqualTo(nextTokenI)
            assertThat(lastModifiedTime).isEqualTo(lastModifiedTimeI)
        }
    }
}