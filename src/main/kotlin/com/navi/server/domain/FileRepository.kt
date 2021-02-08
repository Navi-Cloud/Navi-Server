package com.navi.server.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FileRepository : JpaRepository<FileEntity, Long> {
    @Query("SELECT f FROM FileEntity f ORDER BY f.id DESC")
    fun findAllDesc(): List<FileEntity>

    @Query("FROM FileEntity WHERE prevToken= ?1")
    fun findInsideFiles(token: String) : List<FileEntity>

    @Query("FROM FileEntity WHERE token= ?1")
    fun findFile(token: String) : FileEntity
}