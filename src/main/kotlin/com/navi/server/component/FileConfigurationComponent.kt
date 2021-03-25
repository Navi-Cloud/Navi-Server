package com.navi.server.component

import com.navi.server.domain.FileObject
import com.navi.server.domain.user.User
import com.navi.server.domain.user.UserTemplateRepository
import com.navi.server.service.FileService
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import javax.annotation.PostConstruct

@Component
@ConfigurationProperties("navi")
class FileConfigurationComponent {
    lateinit var serverRoot: String

    @Autowired
    private lateinit var userTemplateRepository: UserTemplateRepository

    @Autowired
    private lateinit var fileService: FileService

    @PostConstruct
    fun initServerRootDirectory() {
        if (System.getProperty("navi.isTesting") == "test") {
            serverRoot = File(System.getProperty("java.io.tmpdir"), "naviServerTesting").absolutePath
            return
        }
    }

    // Let's Just think about normal-initial use for now.
    fun initStructure() {
        val tika: Tika = Tika()
        val userList: List<User> =
            userTemplateRepository.findAllUserOnly()

        for (user in userList) {
            // Append Directory
            val userRootFile: File = File(serverRoot, user.userName)
            userRootFile.mkdir()

            // For Each User, update and insert to DB
            userRootFile.walk().forEach {
                val basicFileAttribute: BasicFileAttributes =
                    Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)

                val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")
                var userFileName: String = it.absolutePath.substring(userRootFile.absolutePath.length, it.absolutePath.length)

                // Windows Implementation
                if (userFileName.contains('\\')) {
                    val eachToken: List<String> = userFileName.split('\\')
                    userFileName = ""
                    eachToken.forEach { tokenString ->
                        if (tokenString.isNotEmpty()) {
                            userFileName += "/$tokenString"
                        }
                    }
                }

                // Unix/Windows: If empty
                if (userFileName.isEmpty()) {
                    userFileName = "/"
                }

                user.fileList.add(
                    FileObject(
                        fileName = userFileName,
                        fileType = if (it.isDirectory) "Folder" else "File",
                        mimeType = if (it.isDirectory) "Folder" else {
                            try {
                                tika.detect(it)
                            } catch (e: Exception) {
                                println("Failed to detect mimeType for: ${e.message}")
                                "File"
                            }
                        },
                        token = fileService.getSHA256(userFileName),
                        prevToken = if (it.absolutePath == userRootFile.absolutePath) "" else  {
                            fileService.getSHA256(
                                if (it.parent == userRootFile.absolutePath) {
                                    "/"
                                } else {
                                    it.parent.substring(
                                        userRootFile.absolutePath.length,
                                        it.parent.length
                                    )
                                }
                            )
                        },
                        lastModifiedTime = it.lastModified(),
                        fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
                        fileSize = fileService.convertSize(basicFileAttribute.size())
                    )
                )
            }

            // Save Back User
            userTemplateRepository.save(user)
        }
    }
}