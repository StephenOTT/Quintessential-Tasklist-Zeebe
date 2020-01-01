package com.github.stephenott.qtz.zeebe

import com.github.stephenott.qtz.linter.*
import io.kotlintest.specs.StringSpec
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.zeebe.model.bpmn.Bpmn
import io.zeebe.model.bpmn.instance.BaseElement
import io.zeebe.model.bpmn.instance.Gateway
import org.camunda.bpm.model.xml.instance.ModelElementInstance
import org.camunda.bpm.model.xml.validation.ModelElementValidator
import org.camunda.bpm.model.xml.validation.ValidationResultCollector
import java.io.File

@MicronautTest
class LinterTest1(
        private var server: EmbeddedServer
): StringSpec({
    "Test Linter"{

        val file = File("src/test/resources/test1.bpmn")

//        val rules: List<LinterRule> =
//                LinterConfigurationParser.getLintRuleBeans(server.applicationContext)
//
//        val validators: List<ModelElementValidator<out ModelElementInstance>> =
//                LinterConfigurationParser.lintRulesToValidators(rules)
//
//
//        WorkflowLinter(file).lintWithValidators(validators)

        val cleanerValidators = listOf(
                elementValidator<BaseElement> { element, validatorResultCollector ->
                    validatorResultCollector.addError(5000, "cleaner code")
                }
        )

        val cleanModel = Cleaner(cleanerValidators).cleanModel(file)
        println(Bpmn.convertToString(cleanModel))

    }
})