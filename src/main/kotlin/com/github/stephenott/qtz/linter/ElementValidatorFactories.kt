package com.github.stephenott.qtz.linter

import org.camunda.bpm.model.xml.instance.ModelElementInstance
import org.camunda.bpm.model.xml.validation.ModelElementValidator
import org.camunda.bpm.model.xml.validation.ValidationResultCollector
import kotlin.reflect.KClass

inline fun <T: ModelElementInstance> elementValidator(clazz: KClass<T>, crossinline validationLogic: (element: T, validatorResultCollector: ValidationResultCollector) -> Unit): ModelElementValidator<T>{
    return object:ModelElementValidator<T>{
        override fun validate(element: T, validationResultCollector: ValidationResultCollector) {
            validationLogic.invoke(element, validationResultCollector)
        }

        override fun getElementType(): Class<T> {
            return clazz.java
        }
    }
}

inline fun <reified T: ModelElementInstance> elementValidator(crossinline validationLogic: (element: T, validatorResultCollector: ValidationResultCollector) -> Unit): ModelElementValidator<T>{
    return object:ModelElementValidator<T>{
        override fun validate(element: T, validationResultCollector: ValidationResultCollector) {
            validationLogic.invoke(element, validationResultCollector)
        }

        override fun getElementType(): Class<T> {
            return T::class.java
        }
    }
}


