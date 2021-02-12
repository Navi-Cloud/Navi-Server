package com.navi.server.web

import com.navi.server.component.FileConfigurationComponent
import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import org.apache.catalina.webresources.FileResource
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import org.assertj.core.api.Assertions.assertThat;
import org.junit.Before
import org.springframework.http.HttpStatus
import java.lang.reflect.Type

import org.springframework.boot.test.web.client.getForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity.status
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockMvcBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.File
import org.springframework.web.context.WebApplicationContext
import java.io.BufferedReader


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileApiControllerTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var fileRepository: FileRepository

    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var trashRootObject: File

    private lateinit var mockMvc : MockMvc

    @Before
    fun initEnvironment() {
        // Create trash directory
        trashRootObject = File(fileConfigurationComponent.serverRoot)
        trashRootObject.mkdir()
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @After
    fun destroyEnvironment() {
        if (trashRootObject.exists()) {
            trashRootObject.deleteRecursively()
        }
        fileRepository.deleteAll()
    }

    /*
    @After
    fun cleanUp() = fileRepository.deleteAll()

     */

    @Test
    fun testSaveFile() {
        val fileName = "fileName"
        val fileType = "fileType"
        val mimeType = "text/plain"
        val nextToken = "token"
        val testingToken = "TestingPrevToken"
        val lastModifiedTime: Long = 5000
        val fileCreatedDate = "testCreatedTime"
        val fileSize: String = "5000mb"

        val requestDto = FileSaveRequestDTO(
            fileName = fileName,
            fileType = fileType,
            mimeType = mimeType,
            token = nextToken,
            prevToken = testingToken,
            lastModifiedTime = lastModifiedTime,
            fileCreatedDate = fileCreatedDate,
            fileSize = fileSize
        )
        val url = "http://localhost:$port/api/navi/files"

        //send api request
        val responseEntity : ResponseEntity<Long> = restTemplate.postForEntity(url, requestDto, Long::class.java)

        //assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isGreaterThan(0L)

        val result : FileEntity = fileRepository.findAll().get(0)
        assertThat(result.fileName).isEqualTo(fileName)
        assertThat(result.fileType).isEqualTo(fileType)
        assertThat(result.mimeType).isEqualTo(mimeType)
        assertThat(result.token).isEqualTo(nextToken)
        assertThat(result.lastModifiedTime).isEqualTo(lastModifiedTime)
        assertThat(result.fileCreatedDate).isEqualTo(fileCreatedDate)
        assertThat(result.fileSize).isEqualTo(fileSize)
    }

    @Test
    fun testFindAllDesc() {
        //insert data
        val fileName = listOf<String>("fileName1", "fileName2", "fileName3", "fileName4")
        fileName.forEach {
            val id = fileRepository.save(FileEntity(fileName = it, fileType = "fileType", mimeType = "text/plain", token = "token", prevToken = "token", lastModifiedTime = 5000, fileCreatedDate = "testCreatedTime", fileSize = "5000"))
        }
        //send api request
        val url = "http://localhost:$port/api/navi/fileList"
        var responseEntity : ResponseEntity<Array<FileResponseDTO>> = restTemplate.getForEntity(url, Array<FileResponseDTO>::class.java)

        //Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body.size).isEqualTo(4)
        var i: Int = fileName.size-1
        responseEntity.body.forEach {
            assertThat(it.fileName).isEqualTo(fileName[i--])
        }
    }

    @Test
    fun testFindRootToken() {
        // At least create one empty file to root
        val fileName: String = "KDRTesting.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
        if (!fileObject.exists()) {
            fileObject.createNewFile()
        }
        // Do work
        val listSize: Long = fileConfigurationComponent.populateInitialDB()

        // Get Api
        val url = "http://localhost:$port/api/navi/rootToken"
        var responseEntity : ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)


        // Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body).isEqualTo(fileConfigurationComponent.getSHA256(fileConfigurationComponent.serverRoot))

    }

    @Test
    fun testFindInsideFiles() {
        // before findInsideFiles Test, Create test file/folder

        // one empty file to root
        val fileName: String = "Testing.txt"
        val fileObject: File = File(fileConfigurationComponent.serverRoot, fileName)
        if (!fileObject.exists()) {
            fileObject.createNewFile()
        }

        // Create one empty Folder to root
        val folderName : String = "TestFolder"
        val folderObject: File = File(fileConfigurationComponent.serverRoot, folderName)
        if (!folderObject.exists()) {
            folderObject.mkdir()
        }

        // Creat 3 Child Files in Folder [folderName]
        val folderPath = folderObject.absolutePath
        val childFiles = listOf<File>(
            File(folderPath, "file1.txt"),
            File(folderPath, "file2.txt"),
            File(folderPath, "file3.txt"),
        )
        childFiles.forEach {
            if (!it.exists()) {
                it.createNewFile()
            }
        }

        // Do work
        val listSize: Long = fileConfigurationComponent.populateInitialDB()
        println("listSize: $listSize")


        // Api test 1 :: under Root
        val rootToken = fileConfigurationComponent.getSHA256(fileConfigurationComponent.serverRoot)
        val url = "http://localhost:$port/api/navi/findInsideFiles/${rootToken}"
        var responseEntity : ResponseEntity<Array<FileResponseDTO>> = restTemplate.getForEntity(url, Array<FileResponseDTO>::class.java)


        // Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body.size).isEqualTo(2) //one File and one Folder
        var result = responseEntity.body
        result.forEach {
            assertThat(it.prevToken).isEqualTo(rootToken)
        }


        //Api test 2 :: under folderName
        val folderToken = fileConfigurationComponent.getSHA256(folderPath)
        val url2 = "http://localhost:$port/api/navi/findInsideFiles/${folderToken}"
        var responseEntity2 : ResponseEntity<Array<FileResponseDTO>> = restTemplate.getForEntity(url2, Array<FileResponseDTO>::class.java)

        // Assert
        assertThat(responseEntity2.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity2.body.size).isEqualTo(3) //3 files

        var result2 = responseEntity2.body
        result2.forEach {
            assertThat(it.prevToken).isEqualTo(folderToken)
        }
    }

    @Test
    fun testFileUpload(){
        fileConfigurationComponent.populateInitialDB()

        // Make uploadFile
        val uploadFileName = "uploadApiTest.txt"
        val uploadFileContent = "test upload API!"
        val testMultipartFile = MockMultipartFile(
            "uploadFile",
            uploadFileName,
            "text/plain",
            uploadFileContent.toByteArray()
        )

        // Perform
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/navi/fileUpload")
            .file(testMultipartFile)
        ).andExpect { status(HttpStatus.OK) }


        // Assert
        val targetFile = File(trashRootObject.absolutePath, uploadFileName)

        val resultFromDB = fileRepository.findAll().find { it.fileName == targetFile.absolutePath }
        resultFromDB?.let {
            assertThat(resultFromDB.fileName).isEqualTo(targetFile.absolutePath)
        } ?: throw Exception("ERROR:: no $uploadFileName")

        val resultFromServer = trashRootObject.listFiles().find { it.isFile && it.absolutePath == targetFile.absolutePath }
        resultFromServer?.let {
            assertThat(resultFromServer.absolutePath).isEqualTo(targetFile.absolutePath)
            val resultContent = resultFromServer.inputStream().bufferedReader().use(BufferedReader::readText)
            assertThat(resultContent).isEqualTo(uploadFileContent)
        } ?: throw Exception("ERROR:: no ${targetFile.absolutePath}")

    }

    @Test
    fun testFileDownload(){



    }
}
