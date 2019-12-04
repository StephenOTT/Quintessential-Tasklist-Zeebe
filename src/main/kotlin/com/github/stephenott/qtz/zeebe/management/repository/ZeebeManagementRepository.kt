package com.github.stephenott.qtz.zeebe.management.repository

import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.zeebe.management.ZeebeManagementClientConfiguration
import io.micronaut.context.annotation.Secondary
import io.reactivex.Single
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.ZeebeFuture
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
                    .variables(startVariables)
                    .send().join(10, TimeUnit.SECONDS)

        }.doOnSubscribe {
            println("Starting workflow instance based on key: " + workflowKey)
        }.doOnSuccess {
            println("Workflow(${workflowKey}) instance started: ${it.workflowInstanceKey}")
        }.doOnError {
            println("Unable to start workflow instance: ${it.message}")
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
                    .send().join(10, TimeUnit.SECONDS) // move timeout to configuration

        }.onErrorResumeNext {
            it.printStackTrace()
            Single.error(it) //@TODO add better error handling
        }
    }

    companion object {
        fun createDefaultZeebeClient(config: ZeebeManagementClientConfiguration): ZeebeClient {
            return ZeebeClient.newClientBuilder()
                    .brokerContactPoint(config.broker) //@TODO add rest of default configurations
                    .usePlaintext() //@TODO remove and replace with cert  /-/SECURITY/-/
                    .build()
        }
    }
}