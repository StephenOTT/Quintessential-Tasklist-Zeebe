package com.github.stephenott.qtz.zeebe.management.repository

import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.zeebe.ZeebeManagementClientConfiguration
import io.micronaut.context.annotation.Secondary
import io.reactivex.Completable
import io.reactivex.Single
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.response.DeploymentEvent
import io.zeebe.client.api.response.WorkflowInstanceEvent
import io.zeebe.model.bpmn.Bpmn
import io.zeebe.model.bpmn.BpmnModelInstance
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

interface ZeebeManagementRepository {
    val config: ZeebeManagementClientConfiguration
    val zClient: ZeebeClient

    fun deployWorkflow(file: File, fileName: String): Single<DeploymentEvent>
    fun createWorkflowInstance(workflowKey: Long, startVariables: ZeebeVariables): Single<WorkflowInstanceEvent>
    fun completeJob(jobKey: Long, completionVariables: ZeebeVariables): Completable
    fun reportJobFailure(jobKey: Long, errorMessage: String, remainingRetries: Int = 0): Completable
}

//@TODO refactor to support multiple repositories? Or should just deploy multiple containers, each with their own config?
@Singleton
@Secondary //@TODO refactor to support the @Replaces annotation of new micronaut release
class ZeebeManagementRepositoryImpl(
        override val config: ZeebeManagementClientConfiguration
) : ZeebeManagementRepository {

    override val zClient: ZeebeClient = createDefaultZeebeClient(config)

    override fun createWorkflowInstance(workflowKey: Long, startVariables: ZeebeVariables): Single<WorkflowInstanceEvent> {
        return Single.fromCallable {
            zClient.newCreateInstanceCommand()
                    .workflowKey(workflowKey)
                    .variables(startVariables.variables ?: mapOf())
                    .send().join(config.commandTimeout.seconds + 1, TimeUnit.SECONDS)

        }.doOnSubscribe {
            println("${config.clusterName} Starting workflow instance based on key: " + workflowKey)
        }.doOnSuccess {
            println("${config.clusterName} Workflow(${workflowKey}) instance started: ${it.workflowInstanceKey}")
        }.doOnError {
            println("${config.clusterName} Unable to start workflow instance: ${it.message}")
        }
    }

    override fun deployWorkflow(file: File, fileName: String): Single<DeploymentEvent> {
        return fileToBpmnModelInstance(file).flatMap {
            createWorkflowDeployment(it, fileName)
        }
    }

    private fun fileToBpmnModelInstance(file: File): Single<BpmnModelInstance> {
        return Single.fromCallable {
            Bpmn.readModelFromFile(file);
        } //@TODO add error handling to return better errors to the client based on bpmn.
        //@TODO add bpmn model linter support
    }

    private fun createWorkflowDeployment(modelInstance: BpmnModelInstance, filename: String): Single<DeploymentEvent> {
        return Single.fromCallable {
            zClient.newDeployCommand()
                    .addWorkflowModel(modelInstance, filename)
                    .send().join(config.commandTimeout.seconds + 1, TimeUnit.SECONDS)// move timeout to configuration

        }.onErrorResumeNext {
            it.printStackTrace()
            Single.error(it) //@TODO add better error handling
        }
    }

    override fun reportJobFailure(jobKey: Long, errorMessage: String, remainingRetries: Int): Completable {
        return Completable.fromCallable {
            zClient.newFailCommand(jobKey)
                    .retries(remainingRetries)
                    .errorMessage(errorMessage)
                    .send().join(config.commandTimeout.seconds + 1, TimeUnit.SECONDS)
        }.onErrorResumeNext {
            it.printStackTrace()
            Completable.error(it)
        }
    }

    override fun completeJob(jobKey: Long, completionVariables: ZeebeVariables): Completable {
        return Completable.fromCallable {
            zClient.newCompleteCommand(jobKey)
                    .variables(completionVariables.variables)
                    .send().join(config.commandTimeout.seconds + 1, TimeUnit.SECONDS)
        }.onErrorResumeNext {
            it.printStackTrace()
            Completable.error(it)
        }
    }

    companion object {
        fun createDefaultZeebeClient(config: ZeebeManagementClientConfiguration): ZeebeClient {
            return ZeebeClient.newClientBuilder()
                    .brokerContactPoint(config.brokerContactPoint) //@TODO add rest of default configurations
                    .defaultRequestTimeout(config.commandTimeout)
                    .defaultMessageTimeToLive(config.messageTimeToLive)
                    .usePlaintext() //@TODO remove and replace with cert  /-/SECURITY/-/
                    .build()
        }
    }
}