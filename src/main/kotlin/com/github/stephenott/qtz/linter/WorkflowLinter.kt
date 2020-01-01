package com.github.stephenott.qtz.linter

import io.zeebe.model.bpmn.Bpmn
import io.zeebe.model.bpmn.BpmnModelInstance
import io.zeebe.model.bpmn.instance.BaseElement
import org.camunda.bpm.model.xml.validation.ModelElementValidator
import java.io.File

class WorkflowLinter(file: File) {

    private val bpmmModelInstance: BpmnModelInstance = Bpmn.readModelFromFile(file)

    fun lintWithValidators(validators: List<ModelElementValidator<*>>){
        val result = bpmmModelInstance.validate(validators)
        result.results.forEach { (model, results) ->
            println("Element ---> ${model.elementType.typeName} ${(model.takeIf { it is BaseElement } as BaseElement).id}")
            results.forEach { validationResult ->
                println("""
                    Type: ${validationResult.type}
                    Code: ${validationResult.code}
                    Element Type: ${model.elementType.typeName}
                    Element Id: ${(model.takeIf { it is BaseElement } as BaseElement).id}
                    Message: ${validationResult.message}
                """.trimIndent())
            }
        }
    }
}