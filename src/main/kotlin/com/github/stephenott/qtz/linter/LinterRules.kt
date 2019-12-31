package com.github.stephenott.qtz.linter

import io.zeebe.model.bpmn.instance.ServiceTask
import org.camunda.bpm.model.xml.instance.ModelElementInstance
import org.camunda.bpm.model.xml.validation.ModelElementValidator
import kotlin.reflect.KClass

object LinterRules {
    fun <T: ModelElementInstance>processRequiredHeadersRule(element: KClass<T>, requiredHeaders: List<String>, targeting: LinterRule.TargetRule?): ModelElementValidator<T>?{
        if (element != ServiceTask::class){
            return null
        } else if (requiredHeaders.isNullOrEmpty()){
            return null
        }

        return elementValidator(element){e, v ->
            val sTask = e as ServiceTask
            if (targeting == null || targeting.serviceTasks?.types!!.contains(sTask.getZeebeTaskDefinition()?.type)){
                if (sTask.getZeebeTaskHeaders()?.hasRequiredKeys(requiredHeaders) == false){
                    v.addError(0, "Missing Required Headers: $requiredHeaders")
                }
            }
        }
    }

    fun <T: ModelElementInstance>processOptionalHeadersRule(element: KClass<T>, optionalHeaders: List<String>, requiredHeaders: List<String>, targeting: LinterRule.TargetRule?): ModelElementValidator<T>?{
        if (element != ServiceTask::class){
            return null
        } else if (requiredHeaders.isNullOrEmpty()){
            return null
        }

        val mergedList: List<String> = optionalHeaders + requiredHeaders

        return elementValidator(element){e, v ->
            val sTask = e as ServiceTask
            if (targeting == null || targeting.serviceTasks?.types!!.contains(sTask.getZeebeTaskDefinition()?.type)){
                if (sTask.getZeebeTaskHeaders()?.hasOptionalAndRequiredKeys(mergedList) == false){
                    v.addError(0, "Found headers that are not part of Optional Headers list: $optionalHeaders")
                }
            }
        }
    }

    fun <T: ModelElementInstance>processDuplicateHeaderKeysRule(element: KClass<T>, targeting: LinterRule.TargetRule?): ModelElementValidator<T>?{
        if (element != ServiceTask::class){
            return null
        }

        return elementValidator(element){e, v ->
            val sTask = e as ServiceTask
            if (targeting == null || targeting.serviceTasks?.types!!.contains(sTask.getZeebeTaskDefinition()?.type)) {
                if (sTask.getZeebeTaskHeaders()?.noDuplicateKeys() == false) {
                    v.addError(0, "Duplicates Keys were detected")
                }
            }
        }
    }

    fun <T: ModelElementInstance>processServiceTaskAllowedTypesListRule(element: KClass<T>, allowedTypes: List<String>, targeting: LinterRule.TargetRule?): ModelElementValidator<T>?{
        if (element != ServiceTask::class){
            return null
        }

        return elementValidator(element){e, v ->
            val sTask = e as ServiceTask
            sTask.getZeebeTaskDefinition()?.let {
                if (it.type !in allowedTypes){
                    v.addError(0, "Service Task Type ${it.type} is not in allowed types $allowedTypes")
                }
            }
        }
    }
}