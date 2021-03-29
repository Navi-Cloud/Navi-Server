package com.navi.server.component

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths

@Component
class FilePathResolver {
    @Autowired
    private lateinit var fileConfigurationComponent: FileConfigurationComponent

    // Convert Windows physical path to server-using path
    fun convertPhysicsPathToServerPath(physicsPath: String, userName: String): String {
        // DB File name should starts from '/'
        val serverRootUserName: String = "${fileConfigurationComponent.serverRoot}/$userName"
        val userRootPath: String = physicsPath.substring(serverRootUserName.length, physicsPath.length)

        return userRootPath.replace("\\", "/")
    }

    fun convertPhysicsPathToPrevServerPath(physicsPath: String, userName: String): String {
        val prevPath: Path = Paths.get(physicsPath).parent
        val convertedPath: String = convertPhysicsPathToServerPath(prevPath.toString(), userName)

        return if (convertedPath.isEmpty()) {
            "/"
        } else {
            convertedPath
        }
    }
}