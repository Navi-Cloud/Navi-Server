package com.navi.server.domain

import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.assertj.core.api.Assertions.assertThat

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileRepositoryTest {

    @Autowired
    lateinit var fileRepository: FileRepository

    @After
    fun cleanUp() = fileRepository.deleteAll()

    @Test
    fun fileTest(){
        val name = "테스트 파일"
        val type = "file"
        val token = "nextToken"
        val time = "time"
        val testingToken = "TestingPrevToken"
        val createdDate = "TestingCreatedDate"
        val fileSize: String = "5MB"

        fileRepository.save(FileEntity(fileName = name, fileType = type, token = token, prevToken = testingToken, lastModifiedTime = time, fileCreatedDate = createdDate, fileSize = fileSize))
        fileRepository.save(FileEntity(fileName = name, fileType = type, token = token, prevToken = testingToken, lastModifiedTime = time, fileCreatedDate = createdDate, fileSize = fileSize))


        val filesList : List<FileEntity> = fileRepository.findAll()

        val file : FileEntity = filesList.get(0)
        println(file.id)
        assertThat(file.fileName).isEqualTo(name)
        assertThat(file.fileType).isEqualTo(type)
        assertThat(file.token).isEqualTo(token)
        assertThat(file.prevToken).isEqualTo(testingToken)
        assertThat(file.lastModifiedTime).isEqualTo(time)
        assertThat(file.fileCreatedDate).isEqualTo(createdDate)
        assertThat(file.fileSize).isEqualTo(fileSize)

        println(filesList.get(1).id)
    }



}