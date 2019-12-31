package com.github.stephenott.qtz.zeebe

import com.github.stephenott.qtz.linter.*
import io.kotlintest.specs.StringSpec
import io.micronaut.context.BeanContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import org.camunda.bpm.model.xml.validation.ModelElementValidator
import java.io.File

@MicronautTest
class LinterTest1(
        private var server: EmbeddedServer,
        private val beanContext: BeanContext
): StringSpec({
    "Test Linter"{

        val file = File("src/test/resources/test1.bpmn")

        val linterRulesEngine: List<LinterRule> = server.applicationContext.getBeansOfType(LinterRule::class.java).toList()

        fun generateRulesFromYml(): MutableList<ModelElementValidator<*>>{
            val myList: MutableList<ModelElementValidator<*>> = mutableListOf()
            linterRulesEngine.forEach { lr ->

                //Apply the rules for Each Element type that was provided
                // Current assumption is that rules should be aware of what elements they apply to
                lr.elementTypes!!.forEach { elementType ->
                    val elementClass = elementType.zeebeClass

                    lr.headerRule?.let { headerRule ->
                        LinterRules.processRequiredHeadersRule(elementClass, headerRule.requiredKeys, lr.target)?.let { v ->
                            myList.add(v)
                        }

                        if (!headerRule.allowedNonDefinedKeys){
                            LinterRules.processOptionalHeadersRule(elementClass, headerRule.optionalKeys, headerRule.requiredKeys, lr.target)?.let { v ->
                                myList.add(v)
                            }
                        }

                        if (!headerRule.allowedDuplicateKeys){
                            LinterRules.processDuplicateHeaderKeysRule(elementClass, lr.target)?.let { v ->
                                myList.add(v)
                            }
                        }
                    }

                    lr.serviceTaskRule?.let { serviceTaskRule ->
                        //@TODO Review need to move this rule to global space for controlling global allowed types
                        serviceTaskRule.allowedTypes?.let { types ->
                            LinterRules.processServiceTaskAllowedTypesListRule(elementClass, types, lr.target)?.let { v ->
                                myList.add(v)
                            }
                        }
                    }

                }

            }
            return myList
        }

        WorkflowLinter(file).lintWithCustomRules(generateRulesFromYml())
    }
})