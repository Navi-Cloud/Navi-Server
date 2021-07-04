package com.navi.server.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.nio.charset.Charset
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest
class PathServiceTest {

    @Autowired
    private lateinit var pathService: PathService

    private fun encodeString(inputString: String): String {
        return Base64.getEncoder().encodeToString(inputString.toByteArray(Charset.forName("UTF-8")))
    }

    // Base 64 Decode
    private fun decodeString(encodedString: String): String {
        return String(Base64.getDecoder().decode(encodedString.toByteArray()))
    }

    @Test
    fun is_appendPath_works_well_normal() {
        val targetPath: String = "/home/kangdroid/what/ever/hello/world"
        val targetFile: String = "test.txt"

        val encodedString: String = pathService.appendPath(targetFile, encodeString(targetPath))
        println(encodedString)

        assertThat(encodedString).isNotEqualTo("")
        assertThat(decodeString(encodedString)).isEqualTo("/home/kangdroid/what/ever/hello/world/test.txt")
    }

    @Test
    fun is_appendPath_works_well_root_append() {
        val targetPath: String = "/"
        val targetFile: String = "test.txt"

        val encodedString: String = pathService.appendPath(targetFile, encodeString(targetPath))
        println(encodedString)

        assertThat(encodedString).isNotEqualTo("")
        assertThat(decodeString(encodedString)).isEqualTo("/test.txt")
    }
}