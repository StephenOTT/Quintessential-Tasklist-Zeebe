package com.github.stephenott.qtz.forms.submission.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class SubmissionObjectAttributeConverter : AttributeConverter<SubmissionObject, ByteArray> {

    companion object {
        //@TODO refactor to a common mapper for db operations
        val mapper: ObjectMapper = jacksonObjectMapper()
    }

    override fun convertToDatabaseColumn(attribute: SubmissionObject?): ByteArray? {
        return if (attribute == null){
            null
        } else{
            mapper.writeValueAsBytes(attribute)
        }
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): SubmissionObject? {
        return if (dbData == null || dbData.isEmpty()){
            null
        } else {
            mapper.readValue(dbData)
        }
    }
}