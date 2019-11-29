package com.github.stephenott.qtz.tasks.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class ZeebeVariablesAttributeConverter : AttributeConverter<ZeebeVariables, ByteArray> {

    companion object {
        //@TODO refactor to a common mapper for db operations
        val mapper: ObjectMapper = jacksonObjectMapper()
    }

    override fun convertToDatabaseColumn(attribute: ZeebeVariables?): ByteArray? {
        return if (attribute == null){
            null
        } else{
            mapper.writeValueAsBytes(attribute)
        }
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): ZeebeVariables? {
        return if (dbData == null || dbData.isEmpty()){
            null
        } else {
            mapper.readValue(dbData)
        }
    }
}

@Converter(autoApply = true)
class UserTaskMetadataAttributeConverter : AttributeConverter<UserTaskMetadata, ByteArray> {

    companion object {
        //@TODO refactor to a common mapper for db operations
        val mapper: ObjectMapper = jacksonObjectMapper()
    }

    override fun convertToDatabaseColumn(attribute: UserTaskMetadata?): ByteArray? {
        return if (attribute == null){
            null
        } else{
            mapper.writeValueAsBytes(attribute)
        }
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): UserTaskMetadata? {
        return if (dbData == null || dbData.isEmpty()){
            null
        } else {
            mapper.readValue(dbData)
        }
    }
}