package com.github.stephenott.qtz.linter

import io.micronaut.context.annotation.*
import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter
import org.camunda.bpm.model.xml.instance.ModelElementInstance
import java.util.*
import javax.inject.Singleton
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import kotlin.reflect.KClass

//@TOOD Implement Regex mappings and rules

@EachProperty("orchestrator.workflow-linter.rules")
@Context
class LinterRule {
    var enabled: Boolean = true

    @NotBlank
    var description: String? = null

    @NotBlank @NotEmpty
    var elementTypes: List<ElementType>? = null

    var target: TargetRule? = null

    var headerRule: HeaderRule? = null
    var serviceTaskRule: ServiceTaskRule? = null
    var baseElementRule: BaseElementRule? = null


    @ConfigurationProperties("target")
    class TargetRule{
        var serviceTasks: TargetsServiceTaskRule? = null
        var receiveTasks: TargetsReceiveTaskRule? = null

        @ConfigurationProperties("serviceTasks")
        class TargetsServiceTaskRule {
            var types: List<String> = listOf()
        }

        @ConfigurationProperties("receiveTasks")
        class TargetsReceiveTaskRule{
            var correlationKeys: List<String> = listOf()
        }
    }

    @ConfigurationProperties("headerRule")
    class HeaderRule{
        var requiredKeys: List<String> = listOf()
        var optionalKeys: List<String> = listOf()
        var requiredKeysRegex: Regex? = null
        var optionalKeysRegex: Regex? = null
        var allowedNonDefinedKeys: Boolean = true
        var allowedDuplicateKeys: Boolean = true
    }

    @ConfigurationProperties("serviceTaskRule")
    class ServiceTaskRule {
        var allowedTypes: List<String>? = null
        var allowedTypesRegex: Regex? = null
        var allowedRetriesRegex: Regex? = null
    }

    @ConfigurationProperties("baseElementRule")
    class BaseElementRule {
        var elementNameRegex: Regex? = null
    }
}


interface ElementTypeZeebe{
    val zeebeClass: KClass<out ModelElementInstance>
}

enum class ElementType: ElementTypeZeebe {
    ServiceTask {
        override val zeebeClass: KClass<out ModelElementInstance> = io.zeebe.model.bpmn.instance.ServiceTask::class
    }
}

@Singleton
class ElementTypeTypeConverter: TypeConverter<String, ElementType>{
    override fun convert(`object`: String, targetType: Class<ElementType>?, context: ConversionContext?): Optional<ElementType> {
        return Optional.of(ElementType.valueOf(`object`))
    }
}