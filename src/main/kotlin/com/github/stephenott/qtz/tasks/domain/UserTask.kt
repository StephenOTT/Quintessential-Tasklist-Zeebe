package com.github.stephenott.qtz.tasks.domain

import io.micronaut.data.annotation.DateUpdated
import java.time.Instant
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@Entity
data class UserTaskEntity(
        @field:Id
        @field:GeneratedValue
        var taskId: UUID? = null,

        @field:Version
        var olVersion: Long? = null,

        @field:DateUpdated
        var updatedAt: Instant? = null,

        var taskOriginalCapture: Instant? = null,

        var state: UserTaskState? = null,

        var title: String? = null,

        var description: String? = null,

        @field:Min(-255) @field:Max(255)
        var priority: Int? = null,

        var assignee: String? = null,

        @field:ElementCollection(fetch = FetchType.EAGER)
        var candidateGroups: Set<String>? = null,

        @field:ElementCollection(fetch = FetchType.EAGER)
        var candidateUsers: Set<String>? = null,

        var dueDate: Instant? = null,

        var formKey: String? = null,

        var completedAt: Instant? = null,

        @field:Column(columnDefinition = "JSON")
        @field:Convert(converter = ZeebeVariablesAttributeConverter::class)
        var completeVariables: ZeebeVariables? = null,

        var zeebeSource: String? = null,

        var zeebeJobKey: Long? = null,

        var bpmnProcessId: String?  = null,

        var bpmnProcessVersion: Long?  = null,

        @field:Column(columnDefinition = "JSON")
        @field:Convert(converter = ZeebeVariablesAttributeConverter::class)
        var zeebeVariablesAtCapture: ZeebeVariables? = null,

        @field:Column(columnDefinition = "JSON")
        @field:Convert(converter = UserTaskMetadataAttributeConverter::class)
        var metadata: UserTaskMetadata? = null
)

//@TODO Create History table that tracks changes to tasks such as claim, unclaim, assign, priority changes, etc.

enum class UserTaskState {
    NEW, ASSIGNED, UNASSIGNED, COMPLETED
}

data class ZeebeVariables(val variables: Map<String, Any?>? = null)
data class UserTaskMetadata(val metadata: Map<String, Any?>? = null)