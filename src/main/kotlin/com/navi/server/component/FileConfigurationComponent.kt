package com.navi.server.component

import com.navi.server.dto.FileSaveRequestDTO
import com.navi.server.service.FileService
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.io.File
import java.text.SimpleDateFormat
import javax.annotation.PostConstruct

@Component
@ConfigurationProperties("navi")
class FileConfigurationComponent(val fileService: FileService) {
    lateinit var serverRoot: String

    @PostConstruct
    fun initPostConstruct() {
        if (System.getProperty("navi.isTesting") == "test") {
            return
        }
        populateInitialDB()
    }

    fun populateInitialDB() {
        val fileObject: File = File(serverRoot)
        if (!fileObject.exists()) {
            throw IllegalArgumentException("Server Root: $serverRoot does not exist!")
        }

        fileObject.list()?.forEach {
            val tmpFileObject: File = File(serverRoot, it)
            val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
            fileService.save(
                FileSaveRequestDTO(
                    id = 0,
                    fileName = tmpFileObject.absolutePath,
                    fileType = if (tmpFileObject.isDirectory) {
                        "Folder"
                    } else {
                        "File"
                    },
                    nextToken = "TMP_TOKEN",
                    lastModifiedTime = simpleDateFormat.format(tmpFileObject.lastModified())
                )
            )
        }
    }
}