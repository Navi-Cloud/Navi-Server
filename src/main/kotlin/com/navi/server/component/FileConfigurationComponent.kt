package com.navi.server.component

import com.navi.server.dto.FileSaveRequestDTO
import com.navi.server.service.FileService
import org.apache.tika.Tika
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
import javax.annotation.PostConstruct

@Component
@ConfigurationProperties("navi")
class FileConfigurationComponent(val fileService: FileService) {
//    lateinit var serverRoot: String
//
//    @PostConstruct
//    fun initPostConstruct() {
//        if (System.getProperty("navi.isTesting") == "test") {
//            serverRoot = File(System.getProperty("java.io.tmpdir"), "naviServerTesting").absolutePath
//            fileService.rootPath = serverRoot
//            fileService.rootToken = fileService.getSHA256(serverRoot)
//            return
//        }
//        populateInitialDB()
//    }
//
//    fun populateInitialDB(): Long {
//        val fileObject: File = File(serverRoot)
//        val fileSaveList: ArrayList<FileSaveRequestDTO> = ArrayList()
//        if (!fileObject.exists()) {
//            throw IllegalArgumentException("Server Root: $serverRoot does not exist!")
//        }
//        val tika: Tika = Tika()
//
//        //save root token
//        fileService.rootPath = serverRoot
//        fileService.rootToken = fileService.getSHA256(serverRoot)
//
//        fileObject.walk().forEach {
//            val basicFileAttribute: BasicFileAttributes =
//                Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
//
//            val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
//            println(it.absolutePath)
//            if (it.absolutePath == serverRoot) {
//                println(it.absolutePath)
//                fileSaveList.add(
//                    FileSaveRequestDTO(
//                        fileName = it.absolutePath,
//                        fileType = "Folder",
//                        mimeType = "Folder",
//                        token = fileService.getSHA256(serverRoot),
//                        prevToken = "",
//                        lastModifiedTime = it.lastModified(),
//                        fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
//                        fileSize = fileService.convertSize(basicFileAttribute.size())
//                    )
//                )
//            } else {
//                with(it) {
//                    fileSaveList.add(
//                        FileSaveRequestDTO(
//                            fileName = absolutePath,
//                            fileType = if (isDirectory) "Folder" else "File",
//                            mimeType = if (isDirectory) "Folder" else {
//                                try {
//                                    tika.detect(it)
//                                } catch (e: Exception) {
//                                    println("Failed to detect mimeType for: ${e.message}")
//                                    "File"
//                                }
//                            },
//                            token = fileService.getSHA256(absolutePath),
//                            prevToken = fileService.getSHA256(parent),
//                            lastModifiedTime = lastModified(),
//                            fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
//                            fileSize = fileService.convertSize(basicFileAttribute.size())
//                        )
//                    )
//                }
//
//            }
//        }
//        fileService.saveAll(fileSaveList)
//        return fileSaveList.size.toLong()
//    }

}