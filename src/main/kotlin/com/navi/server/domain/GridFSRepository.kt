package com.navi.server.domain

import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import com.mongodb.client.gridfs.model.GridFSFile
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.stereotype.Repository
import java.io.InputStream
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberProperties

@Repository
class GridFSRepository(
    private val gridFsTemplate: GridFsTemplate,
    private val gridFsOperations: GridFsOperations
) {
    fun saveToGridFS(fileObject: FileObject, inputStream: InputStream) {
        val dbMetaData: DBObject = convertFileObjectToMetaData(fileObject)

        val id: ObjectId = gridFsTemplate.store(
            inputStream, fileObject.fileName, dbMetaData
        )
    }

    fun removeFile(userId: String, targetToken: String, targetPrevToken: String) {
        val targetFileInformation: FileObject = getMetadataSpecific(userId, targetToken, targetPrevToken)
        when (targetFileInformation.fileType) {
            "Folder" -> removeFolder(userId, targetFileInformation)
            "File" -> removeSingleFile(userId, targetToken, targetPrevToken)
        }
    }

    fun searchFile(userId: String, fileName: String): List<FileObject> {
        val query: Query = Query().apply {
            addCriteria(
                Criteria().andOperator(
                    Criteria.where("metadata.userId").`is`(userId),
                    Criteria.where("metadata.fileName").regex(fileName)
                )
            )
        }

        return gridFsTemplate.find(query).map {
            convertMetaDataToFileObject(it.metadata)
        }.toList()
    }

    // For querying specific file using token & prevToken [i.e direct token search]
    fun getMetadataSpecific(userId: String, targetToken: String, targetPrevToken: String?): FileObject {
        val query: Query = getSearchQuery(userId, targetToken, targetPrevToken)

        val gridFSFile: GridFSFile = gridFsTemplate.findOne(query)

        return convertMetaDataToFileObject(gridFSFile.metadata)
    }

    // For querying specific file using token
    fun getMetadataSpecificFromToken(userId: String, targetToken: String): FileObject {
        val query: Query = Query().apply {
            addCriteria(
                Criteria().andOperator(
                    Criteria.where("metadata.userId").`is`(userId),
                    Criteria.where("metadata.token").`is`(targetToken),
                    Criteria.where("metadata.fileType").`is`("Folder")
                )
            )
        }

        val gridFSFile: GridFSFile = gridFsTemplate.findOne(query)

        return convertMetaDataToFileObject(gridFSFile.metadata)
    }

    /**
     * migrateFile Migrate file, whether original file could be deleted or preserved
     * In case original file is reserved, it means user wants to copy file, or else => move.
     * fromToken: Specific file token
     * toPrevToken: Destination folder
     * copied file will be placed inside destination folder.
     */
    fun migrateFile(fileObject: FileObject, fromToken: String, fromPrevToken: String, preserveFile: Boolean) {
        val query: Query = getSearchQuery(fileObject.userId, fromToken, fromPrevToken)
        val gridFSFile: GridFSFile = gridFsTemplate.findOne(query)

        val fileInputStream: InputStream = GridFsResource(gridFSFile).inputStream

        if (!preserveFile) {
            removeSingleFile(fileObject.userId, fromToken, fromPrevToken)
        }

        // Save
        saveToGridFS(fileObject, fileInputStream)
    }

    private fun getSearchQuery(userId: String, targetToken: String, targetPrevToken: String?): Query = Query().apply {
        addCriteria(
            Criteria().andOperator(
                Criteria.where("metadata.userId").`is`(userId),
                Criteria.where("metadata.token").`is`(targetToken),
                Criteria.where("metadata.prevToken").`is`(targetPrevToken)
            )
        )
    }

    fun getRootToken(userId: String): FileObject {
        val query: Query = Query().apply {
            addCriteria(
                Criteria().andOperator(
                    Criteria.where("metadata.userId").`is`(userId),
                    Criteria.where("metadata.fileName").`is`("/"),
                    Criteria.where("metadata.fileType").`is`("Folder")
                )
            )
        }

        val gridFSFile: GridFSFile = gridFsTemplate.findOne(query)

        return convertMetaDataToFileObject(gridFSFile.metadata)
    }

    // For querying inside-folder file
    fun getMetadataInsideFolder(userId: String, targetPrevToken: String): List<FileObject> {
        val query: Query = Query().apply {
            addCriteria(
                Criteria().andOperator(
                    Criteria.where("metadata.userId").`is`(userId),
                    Criteria.where("metadata.prevToken").`is`(targetPrevToken)
                )
            )
        }

        return gridFsTemplate.find(query).map {
            convertMetaDataToFileObject(it.metadata)
        }.toList()
    }

    fun getFullTargetStream(userId: String, fileObject: FileObject): InputStream {
        val query: Query = Query().apply {
            addCriteria(
                Criteria().andOperator(
                    Criteria.where("metadata.userId").`is`(userId),
                    Criteria.where("metadata.prevToken").`is`(fileObject.prevToken),
                    Criteria.where("metadata.token").`is`(fileObject.token),
                )
            )
        }

        val file: GridFSFile = gridFsTemplate.findOne(query)
        return gridFsOperations.getResource(file).inputStream
    }

    // Reflection Helper
    private fun convertFileObjectToMetaData(fileObject: FileObject): DBObject {
        val dbObject: DBObject = BasicDBObject()
        FileObject::class.memberProperties.forEach {
            dbObject.put(it.name, it.get(fileObject))
        }

        return dbObject
    }

    private fun convertMetaDataToFileObject(metadata: Document?): FileObject {
        val defaultConstructor: KFunction<FileObject> = FileObject::class.constructors.first()

        val argument = defaultConstructor
            .parameters
            .map {
                it to (metadata?.get(it.name) ?: "")
            }
            .toMap()

        return defaultConstructor.callBy(argument)
    }

    private fun removeSingleFile(userId: String, targetToken: String, targetPrevToken: String) {
        val query: Query = Query().apply {
            addCriteria(
                Criteria().andOperator(
                    Criteria.where("metadata.userId").`is`(userId),
                    Criteria.where("metadata.token").`is`(targetToken),
                    Criteria.where("metadata.prevToken").`is`(targetPrevToken)
                )
            )
        }
        gridFsTemplate.delete(query)
    }

    private fun removeFolder(userId: String, folderInformation: FileObject) {
        // We are deleting folderInformation itself
        removeSingleFile(userId, folderInformation.token, folderInformation.prevToken)

        // Now we have to delete where each file.prevToken = folder.token
        val query: Query = Query().apply {
            addCriteria(
                Criteria().andOperator(
                    Criteria.where("metadata.userId").`is`(userId),
                    Criteria.where("metadata.prevToken").`is`(folderInformation.token)
                )
            )
        }

        gridFsTemplate.delete(query)
    }
}