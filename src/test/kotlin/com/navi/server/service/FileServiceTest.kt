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
import org.springframework.core.io.Resource
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

    @Test
    fun isConvertingCorrect() {
        val testFileSizeMib: Long = 1024 * 1024 * 2 // 2 Mib
        val testFileSizeKib: Long = 1024 * 4 // 4.0Kib
        val testFileSizeB: Long = 800 //800B
        val testFileSizeZero: Long = 0
        assertThat(fileService.convertSize(testFileSizeMib)).isEqualTo("2.0MiB")
        assertThat(fileService.convertSize(testFileSizeKib)).isEqualTo("4.0KiB")
        assertThat(fileService.convertSize(testFileSizeB)).isEqualTo("800B")
        assertThat(fileService.convertSize(testFileSizeZero)).isEqualTo("0B")
    }

    @Test
    fun isGettingSHA256WorksWell() {
        val targetPlainText: String = "TestingKDR"
        val hashedString: String = fileService.getSHA256(targetPlainText)

        assertThat(hashedString).isNotEqualTo(null)
        assertThat(hashedString).isNotEqualTo("")
        assertThat(targetPlainText).isNotEqualTo(hashedString)
    }


    @Test
    fun isDeleteByTokenWorksWell() {
        fileRepository.save(
            FileEntity(
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

        // do work
        val response: Long = fileService.deleteByToken(nextTokenTest)
        assertThat(fileRepository.count()).isEqualTo(0L)
    }

    @Test
    fun isFindByTokenWorksWell() {
        fileRepository.save(
            FileEntity(
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

        // do work
        val response: FileResponseDTO = fileService.findByToken(nextTokenTest)
        assertThat(response.fileName).isEqualTo(fileNameTest)
        assertThat(response.fileType).isEqualTo(fileTypeTest)
        assertThat(response.mimeType).isEqualTo(mimeTypeTest)
        assertThat(response.token).isEqualTo(nextTokenTest)
        assertThat(response.prevToken).isEqualTo(prevTokenTest)
        assertThat(response.lastModifiedTime).isEqualTo(lastModifiedTimeTest)
        assertThat(response.fileCreatedDate).isEqualTo(fileCreatedDateTest)
        assertThat(response.fileSize).isEqualTo(fileSizeTest)
    }

    @Test
    fun isFindInsideFilesWorksWell() {
        fileRepository.save(
            FileEntity(
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

        // do work
        val response: List<FileResponseDTO> = fileService.findInsideFiles(prevTokenTest)
        assertThat(response.size).isEqualTo(1L)
        assertThat(response[0].fileName).isEqualTo(fileNameTest)
        assertThat(response[0].fileType).isEqualTo(fileTypeTest)
        assertThat(response[0].mimeType).isEqualTo(mimeTypeTest)
        assertThat(response[0].token).isEqualTo(nextTokenTest)
        assertThat(response[0].prevToken).isEqualTo(prevTokenTest)
        assertThat(response[0].lastModifiedTime).isEqualTo(lastModifiedTimeTest)
        assertThat(response[0].fileCreatedDate).isEqualTo(fileCreatedDateTest)
        assertThat(response[0].fileSize).isEqualTo(fileSizeTest)
    }

    // Do we even need this?
    @Test
    fun isGettingSettingsRootTokenWorksWell() {
        fileService.rootToken = "2021"
        assertThat(fileService.rootToken).isEqualTo("2021")
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
        val uploadFileName = "uploadTest-service.txt"
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

        // Make one test file to root
        val fileName: String = "downloadTest-service.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
        val fileContent = "Test Download!"
        fileObject.writeText(fileContent);
        if (!fileObject.exists()) {
            fileObject.createNewFile()
        }

        fileConfigurationComponent.populateInitialDB()

        // file Download
        val targetToken = fileService.getSHA256(fileObject.absolutePath)
        val result = fileService.fileDownload(targetToken)

        // Assert
        val fileResponseDTO = result?.first
        val resource = result?.second

        fileResponseDTO?.let {
            assertThat(fileResponseDTO.fileName).isEqualTo(fileObject.absolutePath)
        } ?: throw Exception("No File: ${fileObject.absolutePath}")

        resource?.let {
            val resultContent = resource.inputStream.readBytes().toString(Charsets.UTF_8)
            assertThat(resultContent).isEqualTo(fileContent)
        } ?: throw Exception("No FILE")
    }

    @Test
    fun invalidFileUpload(){
        fileConfigurationComponent.populateInitialDB()

        // Create one test Folder to root
        val folderName : String = "Upload"
        val folderObject: File = File(fileConfigurationComponent.serverRoot, folderName)
        if (!folderObject.exists()) {
            folderObject.mkdir()
        }

        // file upload
        val uploadFolderToken = fileService.getSHA256(folderObject.absolutePath) // invalid path (no such folder in DB)
        val multipartFile = MockMultipartFile(
            "uploadFileName", "uploadFileName", "text/plain", "uploadFileContent".toByteArray())
        val result = fileService.fileUpload(uploadFolderToken, multipartFile)

        // Assert
        assertThat(result).isEqualTo(-1)

    }

    @Test
    fun invalidFileDownload(){
        fileConfigurationComponent.populateInitialDB()

        val fileName: String = "invalidDownloadTest.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)

        // file Download 1
        var targetToken = fileService.getSHA256(fileObject.absolutePath) // invalid path (no such file in server DB)
        var result = fileService.fileDownload(targetToken)

        // Assert
        assertThat(result).isEqualTo(null)


        // upload to DB
        fileRepository.save(
            FileEntity(
                fileName = fileName,
                fileType = "type",
                mimeType = "text/plain",
                token = targetToken,
                prevToken = "prevToken",
                lastModifiedTime = 1L,
                fileCreatedDate = "now",
                fileSize = "size"
            )
        )

        // file Download 2
        targetToken = fileService.getSHA256(fileObject.absolutePath) // invalid file (no such file at server)
        //expecting FileNotFoundException
        result = fileService.fileDownload(targetToken)

        // Assert
        assertThat(result).isEqualTo(null)

    }
}