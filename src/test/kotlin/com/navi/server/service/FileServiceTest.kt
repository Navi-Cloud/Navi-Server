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

    // Test variable
    private val fileNameTest: String = "TESTING_FILENAME"
    private val fileTypeTest: String = "Folder"
    private val mimeTypeTest: String = "text/plain"
    private val nextTokenTest: String = "TEST_TOKEN"
    private val prevTokenTest: String = "PREV_TEST_TOKEN"
    private val lastModifiedTimeTest: Long = 5000
    private val fileCreatedDateTest: String = "TEST_CREATED_DATE"
    private val fileSizeTest: String = "500Byte"

    @After
    fun clearDB() {
        fileRepository.deleteAll()
    }

    @Test
    fun isSavingWorks() {
        // save it to DB with fileService
        val retId: Long = fileService.save(
            FileSaveRequestDTO(
                id = 0,
                fileName = fileNameTest,
                fileType = fileTypeTest,
                mimeType = mimeTypeTest,
                token = nextTokenTest,
                prevToken = prevTokenTest,
                lastModifiedTime = lastModifiedTimeTest,
                fileCreatedDate = fileCreatedDateTest,
                fileSize = fileSizeTest
            )
        )

        // Get results from repository
        val results: FileEntity = fileRepository.findById(retId).get()

        // Assert
        with (results) {
            assertThat(fileName).isEqualTo(fileNameTest)
            assertThat(fileType).isEqualTo(fileTypeTest)
            assertThat(mimeType).isEqualTo(mimeTypeTest)
            assertThat(token).isEqualTo(nextTokenTest)
            assertThat(prevToken).isEqualTo(prevTokenTest)
            assertThat(lastModifiedTime).isEqualTo(lastModifiedTimeTest)
            assertThat(fileCreatedDate).isEqualTo(fileCreatedDateTest)
            assertThat(fileSize).isEqualTo(fileSizeTest)
        }
    }

    @Test
    fun isEmptyDescWorks() {
        val listFile: List<FileResponseDTO> = fileService.findAllDesc()

        assertThat(listFile.size).isEqualTo(0)
    }

    @Test
    fun isFindAllDescWorks() {
        // save it to DB with fileService
        val retId: Long = fileService.save(
            FileSaveRequestDTO(
                id = 0,
                fileName = fileNameTest,
                fileType = fileTypeTest,
                mimeType = mimeTypeTest,
                token = nextTokenTest,
                prevToken = prevTokenTest,
                lastModifiedTime = lastModifiedTimeTest,
                fileCreatedDate = fileCreatedDateTest,
                fileSize = fileSizeTest
            )
        )

        // Get Results from fileService
        val listFile: List<FileResponseDTO> = fileService.findAllDesc()

        // Assert
        assertThat(listFile.size).isEqualTo(1)
        with (listFile[0]) {
            assertThat(fileName).isEqualTo(fileNameTest)
            assertThat(fileType).isEqualTo(fileTypeTest)
            assertThat(mimeType).isEqualTo(mimeTypeTest)
            assertThat(token).isEqualTo(nextTokenTest)
            assertThat(prevToken).isEqualTo(prevTokenTest)
            assertThat(lastModifiedTime).isEqualTo(lastModifiedTimeTest)
            assertThat(fileCreatedDate).isEqualTo(fileCreatedDateTest)
            assertThat(fileSize).isEqualTo(fileSizeTest)
        }
    }

    @Test
    fun isSavingAllWorks() {
        val digestValue: Int = 10
        val targetIncreaseValue: Int = digestValue * 10
        val fileSaveRequestDtoList: ArrayList<FileSaveRequestDTO> = ArrayList()

        // actual list size > digestValue
        for (i in 0 until targetIncreaseValue) {
            fileSaveRequestDtoList.add(
                FileSaveRequestDTO(
                    id = 0,
                    fileName = fileNameTest,
                    fileType = fileTypeTest,
                    mimeType = mimeTypeTest,
                    token = nextTokenTest,
                    prevToken = prevTokenTest,
                    lastModifiedTime = lastModifiedTimeTest,
                    fileCreatedDate = fileCreatedDateTest,
                    fileSize = fileSizeTest
                )
            )
        }

        // Save Value
        fileService.saveAll(fileSaveRequestDtoList, digestValue)
        assertThat(fileRepository.count()).isEqualTo(targetIncreaseValue.toLong())

        // actual list size < digestValue
        fileRepository.deleteAll()
        fileService.saveAll(fileSaveRequestDtoList)
        assertThat(fileRepository.count()).isEqualTo(targetIncreaseValue.toLong())
    }
}