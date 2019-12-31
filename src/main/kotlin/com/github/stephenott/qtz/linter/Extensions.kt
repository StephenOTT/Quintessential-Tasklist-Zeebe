package com.github.stephenott.qtz.linter

import io.zeebe.model.bpmn.instance.BaseElement
import io.zeebe.model.bpmn.instance.zeebe.*
import org.camunda.bpm.model.xml.instance.ModelElementInstance

/**
 * Returns null if no task definition info
 */
fun BaseElement.getZeebeTaskDefinition(): ZeebeTaskDefinition? {
    return this.extensionElements.elementsQuery.filterByType(ZeebeTaskDefinition::class.java)
            .list().singleOrNull()
}

/**
 * Returns null if no headers
 */
fun BaseElement.getZeebeTaskHeaders(): ZeebeTaskHeaders? {
    return this.extensionElements.elementsQuery.filterByType(ZeebeTaskHeaders::class.java)
            .list().singleOrNull()
}

/**
 * Returns nul if no subscription data
 */
fun BaseElement.getZeebeSubscription(): ZeebeSubscription? {
    return this.extensionElements.elementsQuery.filterByType(ZeebeSubscription::class.java)
            .list().singleOrNull()
}

/**
 * returns null if no IO mappings
 */
fun BaseElement.getZeebeIoMapping(): ZeebeIoMapping? {
    return this.extensionElements.elementsQuery.filterByType(ZeebeIoMapping::class.java)
            .list().singleOrNull()
}

/**
 * Returns null if no loop characteristics
 */
fun BaseElement.getZeebeLoopCharacteristics(): ZeebeLoopCharacteristics? {
    return this.extensionElements.elementsQuery.filterByType(ZeebeLoopCharacteristics::class.java)
            .list().singleOrNull()
}

/**
 * Basic helper to provide the element type name. Just saves a step.
 */
fun ModelElementInstance.getElementTypeName(): String{
    return this.elementType.typeName
}

/**
 * Checks if model element instance is a BaseElement.  BaseElements are core of BPMN elements that the Model API of Zeebe uses from Camunda.
 */
fun ModelElementInstance.isBaseElement(): Boolean{
    return this is BaseElement
}

/**
 * Ensures that the headers have all required keys
 */
fun ZeebeTaskHeaders.hasRequiredKeys(keys: List<String>): Boolean{
    return this.headers.map { it.key }.containsAll(keys)
}

/**
 * Ensures that the keys list contains all of the items that are in the headers.
 * Should be used in together with .hasRequiredKeys()
 */
fun ZeebeTaskHeaders.hasOptionalAndRequiredKeys(keys: List<String>): Boolean{
    return keys.containsAll(this.headers.map { it.key })
}

fun ZeebeTaskHeaders.noDuplicateKeys(restrictedKeys: List<String>? = null): Boolean{
    return if (restrictedKeys == null){
        // No duplicate headers
        !this.headers.groupingBy { it.key }.eachCount().any { it.value > 1 }
    } else {
        // no duplicate headers only if the header is in the restricted Keys list
        !this.headers.filter { it.key in restrictedKeys }.groupingBy { it.key }.eachCount().any { it.value > 1 }
    }
}