package com.github.stephenott.qtz.zeebe.management.controler

import com.github.stephenott.qtz.zeebe.management.repository.ZeebeManagementRepository
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.StreamingFileUpload
import io.reactivex.Single
import java.io.File
import javax.inject.Inject

@Controller("/zeebe/management")
open class ZeebeManagementController() : ZeebeManagementOperations {

    @Inject
    lateinit var zeebeManagementRepository: ZeebeManagementRepository

    @Post(value = "/deployment", consumes = [MediaType.MULTIPART_FORM_DATA])
    override fun deployWorkflow(file: StreamingFileUpload): Single<HttpResponse<WorkflowDeploymentResponse>> {
        val tempFile = File.createTempFile(file.filename, "temp")

        val uploadPublisher = Single.fromPublisher(file.transferTo(tempFile))

        return uploadPublisher.flatMap { success ->
            if (success) {
                zeebeManagementRepository.deployWorkflow(tempFile)
                        .onErrorResumeNext {
                            Single.error(ZeebeFailedDeploymentException(WorkflowDeploymentFailedResponse("Unable to deploy workflow: ${it.message}")))
                        }.map {de ->
                            HttpResponse.ok(WorkflowDeploymentResponse("Success: " + de.workflows.map { it.workflowKey }))
                        }
            } else {
                throw ZeebeFileUploadException(WorkflowDeploymentFailedResponse("Could not upload file"))
            }
        }
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

    fun deployWorkflow(file: StreamingFileUpload): Single<HttpResponse<WorkflowDeploymentResponse>>

}

data class WorkflowDeploymentRequest(val name: String)
data class WorkflowDeploymentResponse(val result: String)
data class WorkflowDeploymentFailedResponse(val reason: String)

class ZeebeFileUploadException(val responseBody: WorkflowDeploymentFailedResponse) : RuntimeException("File failed to upload.") {}
class ZeebeFailedDeploymentException(val responseBody: WorkflowDeploymentFailedResponse) : RuntimeException("Zeebe Workflow Deployment Failure.") {}