package com.github.stephenott.qtz.zeebe.management.controler

import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.zeebe.management.ZeebeManagementClientConfiguration
import com.github.stephenott.qtz.zeebe.management.repository.ZeebeManagementRepository
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.StreamingFileUpload
import io.reactivex.Single
import io.zeebe.client.api.response.WorkflowInstanceEvent
import java.io.File
import javax.inject.Inject

@Controller("/zeebe/management")
open class ZeebeManagementController(
        val config: ZeebeManagementClientConfiguration) : ZeebeManagementOperations {

    @Inject
    lateinit var zeebeManagementRepository: ZeebeManagementRepository

    @Post(value = "/workflow/deployment", consumes = [MediaType.MULTIPART_FORM_DATA])
    override fun deployWorkflow(workflow: StreamingFileUpload): Single<HttpResponse<WorkflowDeploymentResponse>> {
        val tempFile = File.createTempFile(workflow.filename, "_temp_bpmn") //@TODO make this configurable

        val uploadPublisher = Single.fromPublisher(workflow.transferTo(tempFile))

        return uploadPublisher.flatMap { success ->
            if (success) {
                zeebeManagementRepository.deployWorkflow(tempFile, workflow.filename)
                        .onErrorResumeNext {
                            Single.error(ZeebeFailedDeploymentException(WorkflowDeploymentFailedResponse("Unable to deploy workflow: ${it.message}")))
                        }.map { de ->
                            HttpResponse.ok(WorkflowDeploymentResponse("Success: " + de.workflows.map { it.workflowKey }))
                        }
            } else {
                throw ZeebeFileUploadException(WorkflowDeploymentFailedResponse("Could not upload file"))
            }
        }
    }

    @Post("/workflow/instance")
    override fun createWorkflowInstance(@Body instanceCreationRequest: WorkflowInstanceCreateRequest): Single<HttpResponse<WorkflowInstanceEvent>> {
        return zeebeManagementRepository.createWorkflowInstance(
                instanceCreationRequest.workflowKey,
                instanceCreationRequest.startVariables)
                .map {
                    HttpResponse.created(it)
                }
        //@TODO add better error handling for when workflow creation fails.
    }

    @Error
    fun fileUploadError(request: HttpRequest<*>, exception: ZeebeFileUploadException): HttpResponse<WorkflowDeploymentFailedResponse> {
        return HttpResponse.badRequest(exception.responseBody)
    }

    @Error
    fun zeebeWorkflowDeploymentError(request: HttpRequest<*>, exception: ZeebeFailedDeploymentException): HttpResponse<WorkflowDeploymentFailedResponse> {
        return HttpResponse.badRequest(exception.responseBody)
    }
}

interface ZeebeManagementOperations {

    fun deployWorkflow(workflow: StreamingFileUpload): Single<HttpResponse<WorkflowDeploymentResponse>>

    fun createWorkflowInstance(instanceCreationRequest: WorkflowInstanceCreateRequest): Single<HttpResponse<WorkflowInstanceEvent>>

}

data class WorkflowDeploymentRequest(val name: String) // @TODO review usage need
data class WorkflowDeploymentResponse(val result: String)
data class WorkflowDeploymentFailedResponse(val reason: String)

data class WorkflowInstanceCreateRequest(val workflowKey: Long, val startVariables: ZeebeVariables)

class ZeebeFileUploadException(val responseBody: WorkflowDeploymentFailedResponse) : RuntimeException("File failed to upload.") {}
class ZeebeFailedDeploymentException(val responseBody: WorkflowDeploymentFailedResponse) : RuntimeException("Zeebe Workflow Deployment Failure.") {}