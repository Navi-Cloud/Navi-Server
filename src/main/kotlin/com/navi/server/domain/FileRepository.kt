package com.navi.server.domain

import org.springframework.data.mongodb.repository.MongoRepository

interface FileRepository : MongoRepository<FileEntity, String> {

    fun findAllByOrderByIdDesc(): List<FileEntity>

    fun findAllByPrevToken(prevToken: String) : List<FileEntity>

    fun findByToken(token: String): FileEntity

    //@Query(value="{'token' : $0}", delete = true)
    fun deleteByToken(token: String): Long
}