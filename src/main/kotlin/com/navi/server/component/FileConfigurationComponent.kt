package com.navi.server.component

import com.navi.server.domain.FileEntity
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

        fileObject.walk().forEach {
            val tmpFileObject: File = it
            val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
            if (tmpFileObject.absolutePath == serverRoot) {
                fileSaveList.add(
                    FileSaveRequestDTO(0, tmpFileObject.absolutePath, "Folder", getSHA256(serverRoot), "", simpleDateFormat.format(tmpFileObject.lastModified()))
                )
            } else {
                fileSaveList.add(
                    FileSaveRequestDTO(
                        id = 0,
                        fileName = tmpFileObject.absolutePath,
                        fileType = if (tmpFileObject.isDirectory) "Folder" else "File",
                        nextToken = if (tmpFileObject.isDirectory) getSHA256(tmpFileObject.absolutePath) else "",
                        prevToken = if (tmpFileObject.isDirectory) getSHA256(tmpFileObject.parent) else "",
                        lastModifiedTime = simpleDateFormat.format(tmpFileObject.lastModified())
                    )
                )
            }
        }
        fileService.saveAll(fileSaveList)
        return fileSaveList.size.toLong()
    }
}