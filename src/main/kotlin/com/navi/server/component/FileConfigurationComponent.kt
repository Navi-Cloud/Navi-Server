package com.navi.server.component

import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.service.FileService
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.io.File

@Component
@ConfigurationProperties("navi")
class FileConfigurationComponent {

    var serverRoot: String = File(System.getProperty("java.io.tmpdir"), "naviServerTesting").also {
        it.mkdirs()
    }.absolutePath

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var fileService: FileService

    private val tika: Tika = Tika()

    fun getMimeType(file: File): String {
        return runCatching {
            tika.detect(file)
        }.getOrElse {
            println(it.stackTraceToString())
            "File"
        }
    }
}