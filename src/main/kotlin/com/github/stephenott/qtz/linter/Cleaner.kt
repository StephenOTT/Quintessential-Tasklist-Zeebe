package com.github.stephenott.qtz.linter

import io.zeebe.model.bpmn.Bpmn
import io.zeebe.model.bpmn.BpmnModelInstance
import io.zeebe.model.bpmn.instance.*
import org.camunda.bpm.model.xml.impl.instance.DomElementImpl
import org.camunda.bpm.model.xml.instance.DomElement
import org.camunda.bpm.model.xml.instance.ModelElementInstance
import org.camunda.bpm.model.xml.validation.ModelElementValidator
import org.dom4j.dom.DOMElement
import java.io.File
import kotlin.reflect.full.isSubclassOf

/**
 * Cleaner will provide capability to clean sections of a BPMN that throw a specific error
 *
 * Cleaning means to replace a Element with a new instance of the element, but with zero configuration / a blank instance of the element
 */
class Cleaner(private val validators: List<ModelElementValidator<*>>,
              private val CLEANER_CODE: Int = 5000) {

    fun cleanModel(bpmnFile: File): BpmnModelInstance {
        return cleanModel(Bpmn.readModelFromFile(bpmnFile))
    }

    fun cleanModel(model: BpmnModelInstance): BpmnModelInstance {
        model.validate(validators).results
                .forEach { (element, results) ->
                    //@TODO add support for Gateway/sequence flow support to keep configs for default
                    println(element.elementType.instanceType)
                    println((element as BaseElement).id)
                    println("NEW ELEMENT")
                    results.filter { it.code == CLEANER_CODE }.forEach cleaners@{ eResult ->
                        val newInstance = eResult.element.modelInstance.newInstance<BaseElement>(eResult.element.elementType, (eResult.element as BaseElement).id)

                        if (eResult.element.elementType.instanceType == Process::class.java) {
                            return@cleaners
                        }

                        if (eResult.element.elementType.instanceType.kotlin.isSubclassOf(Expression::class)) {
                            return@cleaners
                        }

                        println("cat")

                        if (eResult.element.elementType.instanceType == Collaboration::class.java) {
                            return@cleaners
                        }

                        if (eResult.element.elementType.instanceType == MultiInstanceLoopCharacteristics::class.java) {
                            return@cleaners
                        }

                        if (eResult.element.elementType.instanceType.kotlin.isSubclassOf(EventDefinition::class)) {
                            return@cleaners
                        }

                        if (eResult.element.elementType.instanceType.kotlin == CategoryValue::class) {
                            (newInstance as CategoryValue).value = (eResult.element as CategoryValue).value
                        }

                        if (eResult.element.elementType.instanceType.kotlin == LaneSet::class) {
                            (newInstance as LaneSet).lanes.addAll((eResult.element as LaneSet).lanes)
                        }

                        if (eResult.element.elementType.instanceType.kotlin == Lane::class) {
                            (newInstance as Lane).flowNodeRefs.addAll((eResult.element as Lane).flowNodeRefs)
                        }

                        if (eResult.element.elementType.instanceType.kotlin == Participant::class) {
                            //@TODO review how to do this without using the attributeValue processRef, and use proper typing.
                            // unclear on how to create a "process" and link it based on the processRef attribute.
                            if ((eResult.element as Participant).getAttributeValue("processRef") != null){
                                (newInstance as Participant).setAttributeValue("processRef", (eResult.element as Participant).getAttributeValue("processRef"))
                            }
                        }

                        if (eResult.element.elementType.instanceType.kotlin.isSubclassOf(BoundaryEvent::class)) {
                            (newInstance as BoundaryEvent).attachedTo = (eResult.element as BoundaryEvent).attachedTo
                            (newInstance as BoundaryEvent).setCancelActivity((eResult.element as BoundaryEvent).cancelActivity())

                            (eResult.element as BoundaryEvent).eventDefinitions.forEach {
                                val newEventInstance: EventDefinition = eResult.element.modelInstance.newInstance(it.elementType, it.id)
                                newInstance.addChildElement(newEventInstance)
                            }
                        }

                        if (eResult.element.elementType.instanceType.kotlin.isSubclassOf(Activity::class)) {

                            val loopChars = (eResult.element as Activity).getChildElementsByType(MultiInstanceLoopCharacteristics::class.java)

                            if (loopChars.isNotEmpty()) {
                                check(loopChars.size == 1, lazyMessage = { "Bad BPMN...MultiInstanceLoopCharacteristics found more than 1 configuration..." })
                                val newLoopChars = eResult.element.modelInstance.newInstance(MultiInstanceLoopCharacteristics::class.java, loopChars.single().id)
                                newLoopChars.isSequential = loopChars.single().isSequential
                                newInstance.addChildElement(newLoopChars)
                            }
                        }

                        if (eResult.element.elementType.instanceType == SequenceFlow::class.java) {
                            (newInstance as SequenceFlow).source = (eResult.element as SequenceFlow).source
                            (newInstance as SequenceFlow).target = (eResult.element as SequenceFlow).target
                        }

                        if (eResult.element.elementType.instanceType == MessageFlow::class.java) {
                            (newInstance as MessageFlow).source = (eResult.element as MessageFlow).source
                            (newInstance as MessageFlow).target = (eResult.element as MessageFlow).target
                        }

                        if (eResult.element.elementType.instanceType == Association::class.java) {
                            (newInstance as Association).source = (eResult.element as Association).source
                            (newInstance as Association).target = (eResult.element as Association).target
                            (newInstance as Association).associationDirection = (eResult.element as Association).associationDirection
                        }

                        if (eResult.element.elementType.instanceType == TextAnnotation::class.java) {
                            (newInstance as TextAnnotation).text = (eResult.element as TextAnnotation).text
                            (newInstance as TextAnnotation).textFormat = (eResult.element as TextAnnotation).textFormat
                        }

                        if (eResult.element.elementType.instanceType.kotlin.isSubclassOf(Gateway::class)) {
                            val defaultAttributeValue = "default"
                            println("DEFAULT: ${eResult.element.getAttributeValue("default")}")
                            if (eResult.element.getAttributeValue(defaultAttributeValue) != null) {
                                newInstance.setAttributeValue(defaultAttributeValue, eResult.element.getAttributeValue(defaultAttributeValue))
                            }
                        }

                        if (eResult.element.getAttributeValue("name") != null) {
                            newInstance.setAttributeValue("name", eResult.element.getAttributeValue("name"))
                        }

                        println("reached replacement")
                        when (element.elementType.instanceType.kotlin) {
                            SubProcess::class -> {
                                val elements = (element as SubProcess).flowElements
                                elements.forEach { newInstance.addChildElement(it) }
                                element.parentElement.replaceChildElement(element, newInstance)
                            }
                            Message::class -> {
                                // This ensures the Message is actually removed from the BPMN.
                                // If you remove all instances of usage of the Message (such as on Receive Tasks and Message Catch Events), the Message element is still present in the BPMN xml.
                                element.parentElement.removeChildElement(element)
                            }
                            else -> {
                                element.parentElement.replaceChildElement(element, newInstance)
                            }
                        }
                    }
                }
        return model
    }

}