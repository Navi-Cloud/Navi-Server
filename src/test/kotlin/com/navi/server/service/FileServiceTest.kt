package com.navi.server.service

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit4.SpringRunner
import java.io.File

@RunWith(SpringRunner::class)
@SpringBootTest
class FileServiceTest {

    @Autowired
    private lateinit var fileRepository: FileRepository

    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

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
        fileRepository.deleteAll()
    }

    // Test variable
    private val fileNameTest: String = "TESTING_FILENAME"
    private val fileTypeTest: String = "Folder"
    private val mimeTypeTest: String = "text/plain"
    private val nextTokenTest: String = "TEST_TOKEN"
    private val prevTokenTest: String = "PREV_TEST_TOKEN"
    private val lastModifiedTimeTest: Long = 5000
    private val fileCreatedDateTest: String = "TEST_CREATED_DATE"
    private val fileSizeTest: String = "500Byte"


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
    fun fileUploadTest(){
        // Create one test Folder to root
        val folderName : String = "Upload"
        val folderObject: File = File(fileConfigurationComponent.serverRoot, folderName)
        if (!folderObject.exists()) {
            folderObject.mkdir()
        }

        fileConfigurationComponent.populateInitialDB()

        // make uploadFile
        val uploadFileName = "uploadTest.txt"
        var uploadFileContent = "file upload test file!".toByteArray()
        val multipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent)

        // file upload
        val uploadFolderToken = fileService.getSHA256(folderObject.absolutePath)
        fileService.fileUpload(uploadFolderToken, multipartFile)

        // Assert
        val targetFile = File(folderObject.absolutePath, uploadFileName)

        val resultFromDB = fileRepository.findAll().find { it.fileName == targetFile.absolutePath }
        resultFromDB?.let { assertThat(resultFromDB.fileName).isEqualTo(targetFile.absolutePath) } ?: throw Exception("ERROR:: no $uploadFileName")

        val resultFromServer = folderObject.listFiles().find { it.isFile && it.absolutePath == targetFile.absolutePath }
        resultFromServer?.let { assertThat(resultFromServer.absolutePath).isEqualTo(targetFile.absolutePath) } ?: throw Exception("ERROR:: no ${targetFile.absolutePath}")

    }

    @Test
    fun fileDownloadTest(){
        /*
        // Make one test file to root
        val fileName: String = "downloadTest.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
        val fileContent = "test"
        fileObject.writeText(fileContent);
        if (!fileObject.exists()) {
            fileObject.createNewFile()
        }

        fileConfigurationComponent.populateInitialDB()

        // file Download
        val targetToken = fileService.getSHA256(fileObject.absolutePath)
        val result = fileService.fileDownload(targetToken)

        val fileResponseDTO = result.first
        val resource = result.second

        // Assert
        fileResponseDTO?.let {
            assertThat(fileResponseDTO.fileName).isEqualTo(fileObject.absolutePath)
        } ?: throw Exception("No FILE")

        resource?.let {
            val resultContent = resource.inputStream.bufferedReader().use(BufferedReader::readText)
            assertThat(resultContent).isEqualTo(fileContent)
        } ?: throw Exception("No FILE")
        val resultFromServer = trashRootObject.listFiles().find { it.isFile && it.absolutePath == targetFile.absolutePath }
        resultFromServer?.let { assertThat(resultFromServer.absolutePath).isEqualTo(targetFile.absolutePath) } ?: throw Exception("ERROR:: no ${targetFile.absolutePath}")

         */

    }
}