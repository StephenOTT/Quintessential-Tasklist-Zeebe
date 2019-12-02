package com.github.stephenott.qtz.zeebe.management.repository

import com.github.stephenott.qtz.tasks.domain.ZeebeVariables
import com.github.stephenott.qtz.zeebe.management.RepositoryConfig
import io.micronaut.context.annotation.Secondary
import io.reactivex.Single
import io.zeebe.client.ZeebeClient
import io.zeebe.client.api.ZeebeFuture
import io.zeebe.client.api.response.DeploymentEvent
import io.zeebe.client.api.response.WorkflowInstanceEvent
import io.zeebe.model.bpmn.Bpmn
import io.zeebe.model.bpmn.BpmnModelInstance
import java.io.File
import javax.inject.Singleton

interface ZeebeManagementRepository{
    val config: RepositoryConfig
    val zClient: ZeebeClient

    fun deployWorkflow(file: File): Single<DeploymentEvent>
    fun createWorkflowInstance(workflowKey: Long, startVariables: ZeebeVariables):Single<WorkflowInstanceEvent>
}

//@TODO refactor to support multiple repositories? Or should just deploy multiple containers, each with their own config?
@Singleton
@Secondary
class SimpleZeebeManagementRepository(
        override val config: RepositoryConfig = RepositoryConfig(),
        override val zClient: ZeebeClient = createDefaultZeebeClient(config)
): ZeebeManagementRepository {

    override fun createWorkflowInstance(workflowKey: Long, startVariables: ZeebeVariables): Single<WorkflowInstanceEvent> {
        //@TODO refactor this into different methods and then use Single.fromCallable
        // currently untested:
        val workflowInstanceEventFuture: ZeebeFuture<WorkflowInstanceEvent> = zClient.newCreateInstanceCommand()
                .workflowKey(workflowKey)
                .variables(startVariables)
                .send()

        println ("Starting workflow instance based on key: " + workflowKey)
        try {
            val workflowInstanceEvent = workflowInstanceEventFuture.join()
            println("Workflow(${workflowKey}) instance started: ${workflowInstanceEvent.workflowInstanceKey}")
            return Single.just(workflowInstanceEvent)

        } catch (e: java.lang.Exception) {
            println("Unable to start workflow instance: ${e.message}")
            throw e
        }
    }

    override fun deployWorkflow(file: File): Single<DeploymentEvent> {
        return Single.fromCallable {
            val model: BpmnModelInstance = fileToBpmnModelInstance(file)
            createWorkflowDeployment(model)
        }
    }

    private fun fileToBpmnModelInstance(file: File): BpmnModelInstance {
        try {
            return Bpmn.readModelFromFile(file);
        } catch (e: Exception) {
            throw e //@TODO add better error handling
        }
    }

    private fun createWorkflowDeployment(modelInstance: BpmnModelInstance): DeploymentEvent {
        val deploymentEventFuture: ZeebeFuture<DeploymentEvent> = zClient.newDeployCommand()
                .addWorkflowModel(modelInstance, modelInstance.model.modelName)
                .send()

        println("Deploying Workflow...")

        try {
            val deploymentEvent: DeploymentEvent = deploymentEventFuture.join()

            //@TODO rebuild this log statement
            println("""
                Deployment Succeeded: 
                Deployment Key:${deploymentEvent.key} with workflows: 
                ${deploymentEvent.workflows.map { it.workflowKey }}
                """)

            return deploymentEvent

        } catch (e:Exception) {
            throw e //@TODO add exceptions customized for Zeebe
        }
    }

    companion object {
        fun createDefaultZeebeClient(config: RepositoryConfig): ZeebeClient {
            return ZeebeClient.newClientBuilder()
                    .brokerContactPoint(config.brokerContactPoint)
                    .usePlaintext() //@TODO remove and replace with cert  /-/SECURITY/-/
                    .build()
        }
    }
}