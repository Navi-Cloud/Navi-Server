package com.navi.server.component

import com.navi.server.dto.FileSaveRequestDTO
import com.navi.server.service.FileService
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.annotation.PostConstruct
import javax.xml.bind.DatatypeConverter

@Component
@ConfigurationProperties("navi")
class FileConfigurationComponent(val fileService: FileService) {
    lateinit var serverRoot: String

    @PostConstruct
    fun initPostConstruct() {
        if (System.getProperty("navi.isTesting") == "test") {
            serverRoot = File(System.getProperty("java.io.tmpdir"), "naviServerTesting").absolutePath
            return
        }
        populateInitialDB()
    }

    fun getSHA256(input: String): String {
        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256").also {
            it.update(input.toByteArray())
        }
        return DatatypeConverter.printHexBinary(messageDigest.digest())
    }

    fun populateInitialDB(): Long {
        val fileObject: File = File(serverRoot)
        val fileSaveList: ArrayList<FileSaveRequestDTO> = ArrayList()
        if (!fileObject.exists()) {
            throw IllegalArgumentException("Server Root: $serverRoot does not exist!")
        }

        //save root token
        fileService.rootToken = getSHA256(serverRoot)

        fileObject.walk().forEach {
            val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
            println(it.absolutePath)
            if (it.absolutePath == serverRoot) {
                println(it.absolutePath)
                fileSaveList.add(
                        FileSaveRequestDTO(0, it.absolutePath, "Folder", getSHA256(serverRoot), "", simpleDateFormat.format(it.lastModified()))
                        )
            } else {
                with (it) {
                    fileSaveList.add(
                        FileSaveRequestDTO(
                            id = 0,
                            fileName = absolutePath,
                            fileType = if (isDirectory) "Folder" else "File",
                            token = getSHA256(absolutePath),
                            prevToken = getSHA256(parent),
                            lastModifiedTime = simpleDateFormat.format(lastModified())
                        )
                    )
                }

            }
        }
        fileService.saveAll(fileSaveList)
        return fileSaveList.size.toLong()
    }

}