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

    private val tika: Tika = Tika()

    fun getMimeType(file: File): String {
        return runCatching {
            tika.detect(file)
        }.getOrElse {
            println(it.stackTraceToString())
            "File"
        }
    }

    // Let's Just think about normal-initial use for now.
    fun initStructure() {
        val userList: List<User> =
            userTemplateRepository.findAllUserOnly()

        for (user in userList) {
            // Append Directory
            val userRootFile: File = File(serverRoot, user.userId)
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
                             getMimeType(it)
                        },
                        token = fileService.getSHA256(userFileName),
                        prevToken = if (it.absolutePath == userRootFile.absolutePath) "" else  {
                            fileService.getSHA256(
                                if (it.parent == userRootFile.absolutePath) {
                                    "/"
                                } else {
                                    val tokenList: List<String> =
                                        userFileName.split('/')
                                    var prevTokenString: String = ""
                                    for (tkString in tokenList.indices) {
                                        if (tkString == tokenList.size-1) {
                                            continue
                                        }
                                        if (tokenList[tkString].isNotEmpty()) {
                                            prevTokenString += "/${tokenList[tkString]}"
                                        }
                                    }
                                    println(prevTokenString)
                                    prevTokenString
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

    fun initNewUserStructure(user: User){
        // Make Root Directory to New user
        val userRootFile: File = File(serverRoot, user.userId)
        userRootFile.mkdir()

        // Insert to DB
        val basicFileAttribute: BasicFileAttributes =
            Files.readAttributes(userRootFile.toPath(), BasicFileAttributes::class.java)
        val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")

        user.fileList.add(
            FileObject(
                fileName = "/",
                fileType = "Folder",
                mimeType = "Folder",
                token = fileService.getSHA256("/"),
                prevToken = "" ,
                lastModifiedTime = userRootFile.lastModified(),
                fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
                fileSize = fileService.convertSize(basicFileAttribute.size())
            )
        )
        userTemplateRepository.save(user)
    }
}