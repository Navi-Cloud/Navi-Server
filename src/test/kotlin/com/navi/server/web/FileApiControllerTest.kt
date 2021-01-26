package com.navi.server.web

import com.fasterxml.jackson.core.type.TypeReference
import com.navi.server.domain.FileEntity
import com.navi.server.domain.FileRepository
import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
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
import org.springframework.http.HttpStatus
import java.lang.reflect.Type

import org.springframework.boot.test.web.client.getForEntity
import java.util.ArrayList

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileApiControllerTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var fileRepository: FileRepository

    @After
    fun cleanUp() = fileRepository.deleteAll()

    @Test
    fun testSaveFile() {
        val fileName = "fileName"
        val fileType = "fileType"
        val nextToken = "token"
        val lastModifiedTime = "time"

        val requestDto = FileSaveRequestDTO(
            fileName = fileName,
            fileType = fileType,
            nextToken = nextToken,
            lastModifiedTime = lastModifiedTime
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
        assertThat(result.nextToken).isEqualTo(nextToken)
        assertThat(result.lastModifiedTime).isEqualTo(lastModifiedTime)
    }

    @Test
    fun testFindAllDesc() {
        //insert data
        val fileName1 = "fileName1"
        val fileName2 = "fileName2"
        val fileName3 = "fileName3"
        val fileName4 = "fileName4"
        fileRepository.save(FileEntity(fileName = fileName1, fileType = "fileType", nextToken = "token", lastModifiedTime = "Time"))
        fileRepository.save(FileEntity(fileName = fileName2, fileType = "fileType", nextToken = "token", lastModifiedTime = "Time"))
        fileRepository.save(FileEntity(fileName = fileName3, fileType = "fileType", nextToken = "token", lastModifiedTime = "Time"))
        fileRepository.save(FileEntity(fileName = fileName4, fileType = "fileType", nextToken = "token", lastModifiedTime = "Time"))

        //send api request
        val url = "http://localhost:$port/api/navi/fileList"
        //val listType: Type = object : TypeToken<List<FileResponseDTO?>?>() {}.getType()
        val typeRef: Type = object : TypeReference<ArrayList<String?>?>() {}.getType()
        var responseEntity : ResponseEntity<List<FileResponseDTO>> = restTemplate.getForEntity(url, typeRef)
        //var tmp : List<FileResponseDTO> = restTemplate.getForEntity(url, List::class.java) as List<FileResponseDTO>

        //Assert
        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responseEntity.body.size).isEqualTo(4)
        //val resultList = responseEntity.body
        //println(resultList.get(0).fileName)

    }
}
