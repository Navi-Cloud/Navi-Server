package com.navi.server.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class FileServiceTest {
//
//    @Autowired
//    private lateinit var fileRepository: FileRepository

    @Autowired
    private lateinit var fileService: FileService
//
//    @Autowired
//    private lateinit var fileConfigurationComponent: FileConfigurationComponent
//
//    private lateinit var trashRootObject: File
//
//    @Before
//    fun initEnvironment() {
//        // Create trash directory
//        trashRootObject = File(fileConfigurationComponent.serverRoot)
//        trashRootObject.mkdir()
//    }
//
//    @After
//    fun destroyEnvironment() {
//        if (trashRootObject.exists()) {
//            trashRootObject.deleteRecursively()
//        }
//        fileRepository.deleteAll()
//    }
//
//    // Test variable
//    private val fileNameTest: String = "TESTING_FILENAME"
//    private val fileTypeTest: String = "Folder"
//    private val mimeTypeTest: String = "text/plain"
//    private val nextTokenTest: String = "TEST_TOKEN"
//    private val prevTokenTest: String = "PREV_TEST_TOKEN"
//    private val lastModifiedTimeTest: Long = 5000
//    private val fileCreatedDateTest: String = "TEST_CREATED_DATE"
//    private val fileSizeTest: String = "500Byte"
//
//
//    @Test
//    fun isSavingWorks() {
//        // save it to DB with fileService
//        val responseEntity: ResponseEntity<FileEntity> = fileService.save(
//            FileSaveRequestDTO(
//                fileName = fileNameTest,
//                fileType = fileTypeTest,
//                mimeType = mimeTypeTest,
//                token = nextTokenTest,
//                prevToken = prevTokenTest,
//                lastModifiedTime = lastModifiedTimeTest,
//                fileCreatedDate = fileCreatedDateTest,
//                fileSize = fileSizeTest
//            )
//        )
//
//        // Get results from repository
//        val retToken = responseEntity.body.token
//        val results: FileEntity = fileRepository.findByToken(retToken)
//
//        // Assert
//        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
//
//        with(results) {
//            assertThat(fileName).isEqualTo(fileNameTest)
//            assertThat(fileType).isEqualTo(fileTypeTest)
//            assertThat(mimeType).isEqualTo(mimeTypeTest)
//            assertThat(token).isEqualTo(nextTokenTest)
//            assertThat(prevToken).isEqualTo(prevTokenTest)
//            assertThat(lastModifiedTime).isEqualTo(lastModifiedTimeTest)
//            assertThat(fileCreatedDate).isEqualTo(fileCreatedDateTest)
//            assertThat(fileSize).isEqualTo(fileSizeTest)
//        }
//    }
//
//    @Test
//    fun isEmptyDescWorks() {
//        val listFile: List<FileResponseDTO> = fileService.findAllDesc().body
//
//        assertThat(listFile.size).isEqualTo(0)
//    }
//
//    @Test
//    fun isFindAllDescWorks() {
//        // save it to DB with fileService
//        val responseEntity: ResponseEntity<FileEntity> = fileService.save(
//            FileSaveRequestDTO(
//                fileName = fileNameTest,
//                fileType = fileTypeTest,
//                mimeType = mimeTypeTest,
//                token = nextTokenTest,
//                prevToken = prevTokenTest,
//                lastModifiedTime = lastModifiedTimeTest,
//                fileCreatedDate = fileCreatedDateTest,
//                fileSize = fileSizeTest
//            )
//        )
//
//        // Get Results from fileService
//        val listFile: List<FileResponseDTO> = fileService.findAllDesc().body
//
//        // Assert
//        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
//
//        assertThat(listFile.size).isEqualTo(1)
//        with(listFile[0]) {
//            assertThat(fileName).isEqualTo(fileNameTest)
//            assertThat(fileType).isEqualTo(fileTypeTest)
//            assertThat(mimeType).isEqualTo(mimeTypeTest)
//            assertThat(token).isEqualTo(nextTokenTest)
//            assertThat(prevToken).isEqualTo(prevTokenTest)
//            assertThat(lastModifiedTime).isEqualTo(lastModifiedTimeTest)
//            assertThat(fileCreatedDate).isEqualTo(fileCreatedDateTest)
//            assertThat(fileSize).isEqualTo(fileSizeTest)
//        }
//    }
//
//    @Test
//    fun isSavingAllWorks() {
//        val digestValue: Int = 10
//        val targetIncreaseValue: Int = digestValue * 10
//        val fileSaveRequestDtoList: ArrayList<FileSaveRequestDTO> = ArrayList()
//
//        // actual list size > digestValue
//        for (i in 0 until targetIncreaseValue) {
//            fileSaveRequestDtoList.add(
//                FileSaveRequestDTO(
//                    fileName = fileNameTest,
//                    fileType = fileTypeTest,
//                    mimeType = mimeTypeTest,
//                    token = nextTokenTest,
//                    prevToken = prevTokenTest,
//                    lastModifiedTime = lastModifiedTimeTest,
//                    fileCreatedDate = fileCreatedDateTest,
//                    fileSize = fileSizeTest
//                )
//            )
//        }
//
//        // Save Value
//        fileService.saveAll(fileSaveRequestDtoList, digestValue)
//        assertThat(fileRepository.count()).isEqualTo(targetIncreaseValue.toLong())
//
//        // actual list size < digestValue
//        fileRepository.deleteAll()
//        fileService.saveAll(fileSaveRequestDtoList)
//        assertThat(fileRepository.count()).isEqualTo(targetIncreaseValue.toLong())
//    }

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
//
//
//    @Test
//    fun isDeleteByTokenWorksWell() {
//        fileRepository.save(
//            FileEntity(
//                fileName = fileNameTest,
//                fileType = fileTypeTest,
//                mimeType = mimeTypeTest,
//                token = nextTokenTest,
//                prevToken = prevTokenTest,
//                lastModifiedTime = lastModifiedTimeTest,
//                fileCreatedDate = fileCreatedDateTest,
//                fileSize = fileSizeTest
//            )
//        )
//
//        // do work
//        val response: Long = fileService.deleteByToken(nextTokenTest)
//        assertThat(fileRepository.count()).isEqualTo(0L)
//    }
//
//    @Test
//    fun isFindByTokenWorksWell() {
//        fileRepository.save(
//            FileEntity(
//                fileName = fileNameTest,
//                fileType = fileTypeTest,
//                mimeType = mimeTypeTest,
//                token = nextTokenTest,
//                prevToken = prevTokenTest,
//                lastModifiedTime = lastModifiedTimeTest,
//                fileCreatedDate = fileCreatedDateTest,
//                fileSize = fileSizeTest
//            )
//        )
//
//        // do work
//        val response: FileResponseDTO = fileService.findByToken(nextTokenTest)
//        assertThat(response.fileName).isEqualTo(fileNameTest)
//        assertThat(response.fileType).isEqualTo(fileTypeTest)
//        assertThat(response.mimeType).isEqualTo(mimeTypeTest)
//        assertThat(response.token).isEqualTo(nextTokenTest)
//        assertThat(response.prevToken).isEqualTo(prevTokenTest)
//        assertThat(response.lastModifiedTime).isEqualTo(lastModifiedTimeTest)
//        assertThat(response.fileCreatedDate).isEqualTo(fileCreatedDateTest)
//        assertThat(response.fileSize).isEqualTo(fileSizeTest)
//    }
//
//    @Test
//    fun isFindInsideFilesWorksWell() {
//        saveFileEntityToDB(prevTokenTest, "Folder")
//
//        fileRepository.save(
//            FileEntity(
//                fileName = fileNameTest,
//                fileType = fileTypeTest,
//                mimeType = mimeTypeTest,
//                token = nextTokenTest,
//                prevToken = prevTokenTest,
//                lastModifiedTime = lastModifiedTimeTest,
//                fileCreatedDate = fileCreatedDateTest,
//                fileSize = fileSizeTest
//            )
//        )
//
//        // do work
//        val responseEntity : ResponseEntity<List<FileResponseDTO>> = fileService.findInsideFiles(prevTokenTest)
//        val statusCode = responseEntity.statusCode
//        assertThat(statusCode).isEqualTo(HttpStatus.OK)
//        val response: List<FileResponseDTO> = responseEntity.body
//        assertThat(response.size).isEqualTo(1L)
//        assertThat(response[0].fileName).isEqualTo(fileNameTest)
//        assertThat(response[0].fileType).isEqualTo(fileTypeTest)
//        assertThat(response[0].mimeType).isEqualTo(mimeTypeTest)
//        assertThat(response[0].token).isEqualTo(nextTokenTest)
//        assertThat(response[0].prevToken).isEqualTo(prevTokenTest)
//        assertThat(response[0].lastModifiedTime).isEqualTo(lastModifiedTimeTest)
//        assertThat(response[0].fileCreatedDate).isEqualTo(fileCreatedDateTest)
//        assertThat(response[0].fileSize).isEqualTo(fileSizeTest)
//    }
//
//    @Test
//    fun invalid_findInsideFiles(){
//        // invalid find Inside files : invalid token
//        val invalidToken = "token"
//        runCatching {
//            fileService.findInsideFiles("token")
//        }.onSuccess{
//            fail("This Should be failed,....")
//        }.onFailure {
//            assertThat(it.message).isEqualTo("Cannot find file by this token : $invalidToken")
//        }
//    }
//
//    // Do we even need this?
//    @Test
//    fun isGettingSettingsRootTokenWorksWell() {
//        fileService.rootToken = "2021"
//        assertThat(fileService.rootToken).isEqualTo("2021")
//    }
//
//    @Test
//    fun fileUploadTest() {
//        // Create one test Folder to root
//        val folderName: String = "Upload"
//        val folderObject: File = File(fileConfigurationComponent.serverRoot, folderName)
//        if (!folderObject.exists()) {
//            folderObject.mkdir()
//        }
//
//        fileConfigurationComponent.populateInitialDB()
//
//        // make uploadFile
//        val uploadFileName = "uploadTest-service.txt"
//        var uploadFileContent = "file upload test file!".toByteArray()
//        val multipartFile = MockMultipartFile(
//            uploadFileName, uploadFileName, "text/plain", uploadFileContent
//        )
//
//        // file upload
//        val uploadFolderToken = fileService.getSHA256(folderObject.absolutePath)
//        fileService.fileUpload(uploadFolderToken, multipartFile)
//
//        // Assert
//        val targetFile = File(folderObject.absolutePath, uploadFileName)
//
//        val resultFromDB = fileRepository.findAll().find { it.fileName == targetFile.absolutePath }
//        resultFromDB?.let { assertThat(resultFromDB.fileName).isEqualTo(targetFile.absolutePath) }
//            ?: throw Exception("ERROR:: no $uploadFileName")
//
//        val resultFromServer = folderObject.listFiles().find { it.isFile && it.absolutePath == targetFile.absolutePath }
//        resultFromServer?.let { assertThat(resultFromServer.absolutePath).isEqualTo(targetFile.absolutePath) }
//            ?: throw Exception("ERROR:: no ${targetFile.absolutePath}")
//
//    }
//
//    @Test
//    fun fileDownloadTest() {
//        // Make one test file to root
//        val fileName: String = "downloadTest-service.txt"
//        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
//        val fileContent = "Test Download!"
//        fileObject.writeText(fileContent);
//        if (!fileObject.exists()) {
//            fileObject.createNewFile()
//        }
//
//        fileConfigurationComponent.populateInitialDB()
//
//        // file Download
//        val targetToken = fileService.getSHA256(fileObject.absolutePath)
//        val responseEntity : ResponseEntity<Resource> = fileService.fileDownload(targetToken)
//
//        // Assert
//        val contentDisposition : String? = responseEntity.headers.get(HttpHeaders.CONTENT_DISPOSITION)!!.get(0)
//        contentDisposition?.let {
//            val resultFileName = it.split("=")[1]
//            assertThat(resultFileName.substring(1, resultFileName.length-1)).isEqualTo(fileName)
//        } ?: throw Exception("File Name mismatch OR No File Name in ContentDisposition")
//
//        val resource : Resource? = responseEntity.body
//        resource?.let {
//            val resultContent = resource.inputStream.readBytes().toString(Charsets.UTF_8)
//            assertThat(resultContent).isEqualTo(fileContent)
//        } ?: throw Exception("No FILE")
//    }
//
//
//    @Test
//    fun invalid_FileUpload() {
//        fileConfigurationComponent.populateInitialDB()
//        // Create one test Folder to root
//        val folderName: String = "Upload"
//        val folderObject: File = File(fileConfigurationComponent.serverRoot, folderName)
//        if (!folderObject.exists()) {
//            folderObject.mkdir()
//        }
//
//        // invalid upload test 1 : invalid token
//        val uploadFolderToken = fileService.getSHA256(folderObject.absolutePath) // invalid path (no such folder in DB)
//        val multipartFile = MockMultipartFile(
//            "uploadFileName", "uploadFileName", "text/plain", "uploadFileContent".toByteArray()
//        )
//        runCatching {
//            fileService.fileUpload(uploadFolderToken, multipartFile)
//        }.onSuccess {
//            fail("This Should be failed,....")
//        }.onFailure {
//            assertThat(it.message).isEqualTo("Cannot find file by this token : $uploadFolderToken")
//        }
//
//
//        // invalid upload test 2 : invalid multipartFile (IOException)
//        val multipartFile2 = MockMultipartFile(
//            "uploadFileName", "".toByteArray()
//        )
//        runCatching {
//            fileService.fileUpload(fileService.rootToken!!, multipartFile2)
//        }.onSuccess {
//            fail("This Should be failed,....")
//        }.onFailure {
//            assertThat(it.message).isEqualTo("File IO Exception")
//        }
//    }
//
//    @Test
//    fun invalid_FileDownload() {
//        val fileName: String = "invalidDownloadTest.txt"
//        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
//
//        // invalid file Download 1 : invalid token
//        var targetToken = fileService.getSHA256(fileObject.absolutePath) // invalid path (no such file in server DB)
//        runCatching {
//            fileService.fileDownload(targetToken)
//        }.onSuccess {
//            fail("This Should be failed,....")
//        }.onFailure {
//            assertThat(it.message).isEqualTo("Cannot find file by this token : $targetToken")
//        }
//
//
//        // save to DB
//        fileRepository.save(
//            FileEntity(
//                fileName = fileName,
//                fileType = "type",
//                mimeType = "text/plain",
//                token = targetToken,
//                prevToken = "prevToken",
//                lastModifiedTime = 1L,
//                fileCreatedDate = "now",
//                fileSize = "size"
//            )
//        )
//
//
//        // invalid file Download 2 : FileNotFoundException
//        targetToken = fileService.getSHA256(fileObject.absolutePath) // invalid file (no such file at server)
//
//        //expecting FileNotFoundException
//        runCatching {
//            fileService.fileDownload(targetToken)
//        }.onSuccess {
//            fail("This Should be failed,....")
//        }.onFailure {
//            assertThat(it.message).isEqualTo("File Not Found !")
//        }
//
//    }
//
//    fun saveFileEntityToDB(filename: String, fileType: String){
//        fileRepository.save(FileEntity(
//            fileName = filename,
//            fileType = fileType,
//            mimeType = "File",
//            token = filename,
//            prevToken = "",
//            lastModifiedTime = 1L,
//            fileCreatedDate = "date",
//            fileSize = "size"
//        ))
//    }
}