package com.github.stephenott.qtz.forms.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javax.persistence.AttributeConverter
import javax.persistence.Converter

//@TODO Refactor to support a Generic converter that impl can extend from.
@Converter(autoApply = true)
class FormSchemaAttributeConverter : AttributeConverter<FormSchema, ByteArray> {

    companion object {
        //@TODO refactor to a common mapper for db operations
        val mapper: ObjectMapper = jacksonObjectMapper()
    }

    override fun convertToDatabaseColumn(attribute: FormSchema?): ByteArray? {
        return if (attribute == null){
            null
        } else{
            mapper.writeValueAsBytes(attribute)
        }
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): FormSchema? {
        return if (dbData == null || dbData.isEmpty()){
            null
        } else {
            mapper.readValue(dbData)
        }
    }
}