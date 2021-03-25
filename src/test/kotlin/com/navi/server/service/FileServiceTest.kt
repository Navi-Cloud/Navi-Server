package com.navi.server.service

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileObject
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.error.exception.NotFoundException
import com.navi.server.security.JWTTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit4.SpringRunner
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@RunWith(SpringRunner::class)
@SpringBootTest
class FileServiceTest {
//
//    @Autowired
//    private lateinit var fileRepository: FileRepository

    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    @Autowired
    private lateinit var jwtTokenProvider: JWTTokenProvider

    private lateinit var trashRootObject: File

    private fun registerAndLogin(): String {
        val mockUser: User = User(
            userName = "KangDroid",
            userPassword = ""
        )
        // Register
        userTemplateRepository.save(
            User(
                userName = "KangDroid",
                userPassword = ""
            )
        )

        // Token
        return jwtTokenProvider.createToken(mockUser.userName, mockUser.roles.toList())
    }

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
    fun is_findRootToken_throws_40x_root_token() {
        val loginToken: String = registerAndLogin()

        runCatching {
            fileService.findRootToken(loginToken)
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }.onSuccess {
            fail("INIT did not proceeded, but somehow it succeed?")
        }
    }

    @Test
    fun is_findRootToken_throws_404_unknown_token() {
        runCatching {
            fileService.findRootToken("what_token")
        }.onSuccess {
            fail("Wrong token passed but this test passed somehow.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Username is NOT Found!")
        }
    }

    @Test
    fun is_findRootToken_OK() {
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        runCatching {
            fileService.findRootToken(loginToken)
        }.onSuccess {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.hasBody()).isEqualTo(true)
            assertThat(it.body).isNotEqualTo("")
        }.onFailure {
            println(it.stackTraceToString())
            fail("This test should return OK because we saved and init-ed its structure, but somehow it failed!")
        }
    }

    // findAllDesc Test
    @Test
    fun is_findAllDesc_404_unknown_token() {
        runCatching {
            fileService.findAllDesc("test")
        }.onSuccess {
            fail("Wrong token passed but this test passed somehow")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
            assertThat(it.message).isEqualTo("Username is NOT Found!")
        }
    }

    @Test
    fun is_findAllDesc_OK() {
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        runCatching {
            fileService.findAllDesc(loginToken)
        }.onSuccess {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.hasBody()).isEqualTo(true)
            assertThat(it.body.size).isEqualTo(1L)
        }.onFailure {
            println(it.stackTraceToString())
            fail("This test should return OK because we saved and init-ed its structure, but somehow it failed!")
        }
    }

    // findInsideFiles test
    @Test
    fun is_findInsideFiles_404_no_file() {
        val loginToken: String = registerAndLogin()

        runCatching {
            fileService.findInsideFiles(loginToken, "somewhat_wrong_token")
        }.onSuccess {
            fail("PrevToken is fake, but somehow it succeed!")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_findInsideFiles_ok() {
        val loginToken: String = registerAndLogin()
        File(fileConfigurationComponent.serverRoot, "KangDroid").mkdir()
        val path: Path = Paths.get(fileConfigurationComponent.serverRoot, "KangDroid", "test.txt")
        val fileObject: File = path.toFile()
        fileObject.writeText("test")
        fileConfigurationComponent.initStructure()

        runCatching {
            fileService.findInsideFiles(loginToken, fileService.getSHA256("/"))
        }.onFailure {
            fail("This test should be succeed!")
        }.onSuccess {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.hasBody()).isEqualTo(true)
            assertThat(it.body.size).isEqualTo(1L)
            assertThat(it.body[0].fileName).isEqualTo("/test.txt")
        }
    }

    // convertFileNameToFullPath test
    @Test
    fun is_convertFileNameToFullPath_works_well() {
        val userFilePath: String = "/kdr/test/test.txt"
        val workedString: String = fileService.convertFileNameToFullPath("KangDroid", userFilePath)

        assertThat(workedString).isEqualTo("${fileConfigurationComponent.serverRoot}/KangDroid$userFilePath")
    }

    // fileUpload Test

    @Test
    fun is_fileUpload_throws_IOException() {
        // Let
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        val multipartFile2 = MockMultipartFile(
            "uploadFileName", "".toByteArray()
        )
        runCatching {
            fileService.fileUpload(loginToken, fileService.getSHA256("/"), multipartFile2)
        }.onSuccess {
            fail("This Should be failed,....")
        }.onFailure {
            assertThat(it.message).isEqualTo("File IO Exception")
        }
    }

    @Test
    fun is_fileUpload_working_well() {
        // Create Server Root Structure
        val loginToken: String = registerAndLogin()
        fileConfigurationComponent.initStructure()

        // make uploadFile
        val uploadFileName: String = "uploadTest-service.txt"
        val uploadFileContent: ByteArray = "file upload test file!".toByteArray()
        val multipartFile: MockMultipartFile = MockMultipartFile(
            uploadFileName, uploadFileName, "text/plain", uploadFileContent
        )

        // file upload
        val uploadFolderToken = fileService.getSHA256("/") // Upload to user's root
        fileService.fileUpload(
            userToken = loginToken,
            uploadFolderToken = uploadFolderToken,
            files = multipartFile
        )

        // Whether uploaded text file is actually exists
        val userName: String = jwtTokenProvider.getUserPk(loginToken)
        val uploadedFileObject: File =
            Paths.get(fileConfigurationComponent.serverRoot, userName, uploadFileName).toFile()
        assertThat(uploadedFileObject.exists()).isEqualTo(true)

        // Now Check DB
        val user: User = userTemplateRepository.findByUserName(userName)
            ?: fail("User should NOT be null!")
        val fileList: MutableList<FileObject> = user.fileList
        assertThat(fileList.size).isEqualTo(2L)
        assertThat(fileList[1].fileName).isEqualTo("/$uploadFileName")
    }

    // getFileObjectByUserName test
    @Test
    fun is_getFileObjectByUserName_returns_404_wrong_user() {
        runCatching {
            fileService.getFileObjectByUserName("wrong_user", fileService.getSHA256("/"))
        }.onSuccess {
            fail("Wrong user specified but somehow function succeed?")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_getFileObjectByUserName_returns_fileObject() {
        // Let
        val fileObject: FileObject = FileObject(
            fileName = "test_file",
            fileType = "test_type",
            mimeType = "mime_type",
            token = "test_token",
            prevToken = "prev_token",
            lastModifiedTime = System.currentTimeMillis(),
            fileCreatedDate = "date",
            fileSize = "5m"
        )

        val user: User = User(
            userName = "KangDroid",
            userPassword = "userpassword",
            roles = setOf("ROLES_ADMIN"),
            fileList = mutableListOf(fileObject)
        )
        userTemplateRepository.save(user)

        val responseFileObject: FileObject = runCatching {
            fileService.getFileObjectByUserName(user.userName, fileObject.token)
        }.getOrElse {
            fail("All things are set-up, so it should not fail!")
        }

        assertThat(responseFileObject.fileName).isEqualTo(fileObject.fileName)
        assertThat(responseFileObject.fileType).isEqualTo(fileObject.fileType)
        assertThat(responseFileObject.mimeType).isEqualTo(fileObject.mimeType)
        assertThat(responseFileObject.token).isEqualTo(fileObject.token)
        assertThat(responseFileObject.prevToken).isEqualTo(fileObject.prevToken)
    }

    // fileDownload test
    @Test
    fun is_fileDownload_returns_NotFoundException_no_file() {
        // Let
        val fileObject: FileObject = FileObject(
            fileName = "test_file",
            fileType = "test_type",
            mimeType = "mime_type",
            token = "test_token",
            prevToken = "prev_token",
            lastModifiedTime = System.currentTimeMillis(),
            fileCreatedDate = "date",
            fileSize = "5m"
        )

        val loginToken: String = registerAndLogin()
        val user: User = userTemplateRepository.findByUserName("KangDroid")
        user.fileList.add(fileObject)
        userTemplateRepository.save(user)

        runCatching {
            fileService.fileDownload(loginToken, fileObject.token)
        }.onSuccess {
            fail("This should return 404 because we do not have actual file.")
        }.onFailure {
            assertThat(it is NotFoundException).isEqualTo(true)
        }
    }

    @Test
    fun is_fileDownload_returns_OK() {
        // Let
        val fileObject: FileObject = FileObject(
            fileName = "test_file",
            fileType = "test_type",
            mimeType = "mime_type",
            token = "test_token",
            prevToken = "prev_token",
            lastModifiedTime = System.currentTimeMillis(),
            fileCreatedDate = "date",
            fileSize = "5m"
        )

        val loginToken: String = registerAndLogin()
        val user: User = userTemplateRepository.findByUserName("KangDroid")
        user.fileList.add(fileObject)
        userTemplateRepository.save(user)
        // Write some texts
        Paths.get(fileConfigurationComponent.serverRoot, "KangDroid").toFile().mkdirs()
        val file: File = Paths.get(fileConfigurationComponent.serverRoot, "KangDroid", fileObject.fileName).toFile()
        file.writeText("Test!")

        runCatching {
            fileService.fileDownload(loginToken, fileObject.token)
        }.onFailure {
            fail("This should not fail! : ${it.stackTraceToString()}")
        }
    }

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