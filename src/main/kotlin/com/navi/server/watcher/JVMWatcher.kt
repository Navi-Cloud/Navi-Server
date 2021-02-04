package com.navi.server.watcher

import com.navi.server.dto.FileResponseDTO
import com.navi.server.dto.FileSaveRequestDTO
import com.navi.server.service.FileService
import com.sun.nio.file.SensitivityWatchEventModifier
import org.apache.tika.Tika
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat

/**
 * This class is for OS does not support C/C++ Based Native File Watching Service.
 * JVM Should just work for any OS supports Java, but it is relatively slower comparing to
 * Native File Watching Service.
 */
class JVMWatcher(rootDirectory: String, fileService: FileService): InternalFileWatcher(rootDirectory, fileService) {
    var watchKey: WatchKey? = null
    private val tika: Tika = Tika()
    val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")

    override fun watchFolder() {
        println("Starting")
        val map: HashMap<WatchKey, Path> = HashMap()
        val targetPath: String = fileToWatch
        val watchService: WatchService = FileSystems.getDefault().newWatchService()

        File(targetPath).walk().forEach { it ->
            if (it.isDirectory) {
                Paths.get(it.absolutePath).also { pathTmp ->
                    val key: WatchKey = pathTmp.register(
                        watchService,
                        arrayOf<WatchEvent.Kind<*>>(
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE
                        ),
                        SensitivityWatchEventModifier.HIGH
                    )
                    map[key] = pathTmp
                    println("Registered: ${pathTmp.toAbsolutePath()}")
                }
            }
        }

        while (isContinue) {
            try {
                watchKey = watchService.take() // Start wait
            } catch (e: InterruptedException) {
                println("Interrupted!")
            }
            val parentPath: Path = map[watchKey] ?: run {
                println("ERROR: NO KEY")
                return
            }
            val events = watchKey?.pollEvents() // Get Events

            if (events != null) {
                for (event in events) {
                    val kind = event.kind()
                    val absolutePath: Path = parentPath.resolve(event.context() as Path)
                    val fileObject: File = File(absolutePath.toUri())
                    val basicFileAttribute: BasicFileAttributes = Files.readAttributes(fileObject.toPath(), BasicFileAttributes::class.java)

                    when (kind) {
                        // Note: FileSaveRequestDto does not require ID anyway though
                        StandardWatchEventKinds.ENTRY_CREATE -> {
                            fileService.save(FileSaveRequestDTO(
                                fileName = absolutePath.toString(),
                                fileType = if (fileObject.isDirectory) "Folder" else "File",
                                mimeType = if(fileObject.isDirectory) "Folder" else {
                                    try {
                                        tika.detect(fileObject)
                                    } catch (e: Exception) {
                                        println("Failed to detect mimeType for: ${e.message}")
                                        "File"
                                    }
                                },
                                token = fileService.getSHA256(fileObject.absolutePath),
                                prevToken = fileService.getSHA256(fileObject.parent),
                                lastModifiedTime = fileObject.lastModified(),
                                fileCreatedDate = simpleDateFormat.format(basicFileAttribute.creationTime().toMillis()),
                                fileSize = fileService.convertSize(basicFileAttribute.size())
                            ))
                            println("Created: $absolutePath")
                        }
                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            fileService.deleteByToken(fileService.getSHA256(fileObject.absolutePath))
                            println("Deleted: $absolutePath")
                        }
                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            val token: String = fileService.getSHA256(fileObject.absolutePath)
                            val toEdit: FileResponseDTO = fileService.findByToken(token).also {
                                it.lastModifiedTime = fileObject.lastModified()
                                it.fileSize = fileService.convertSize(basicFileAttribute.size())
                            }
                            fileService.save(FileSaveRequestDTO(toEdit))
                            println("Modified: $absolutePath")
                        }
                    }
                }
            }
            watchKey?.reset()
        }
    }

    override fun closeWatcher() {
        println("Canceled")
        isContinue = false
        watchKey?.cancel()
    }
}