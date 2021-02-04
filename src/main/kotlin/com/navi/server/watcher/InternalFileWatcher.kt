package com.navi.server.watcher

import com.navi.server.service.FileService

abstract class InternalFileWatcher(val fileToWatch: String = "/tmp", val fileService: FileService) {
    var isContinue: Boolean = true

    abstract fun watchFolder()
    abstract fun closeWatcher()
}