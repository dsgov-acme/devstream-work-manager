package io.nuvalence.workmanager.service.utils.camunda;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CamundaWorkflowInspectorCacheTest {
    private static final String PROCESS_DEFINITION_ID = "processDefinitionId";

    @Mock private ProcessEngine processEngine;

    @Mock private RuntimeService runtimeService;

    @Mock private ProcessInstanceQuery processInstanceQuery;

    @Mock private HistoryService historyService;

    @Mock private HistoricProcessInstanceQuery historicProcessInstanceQuery;

    @Mock private RepositoryService repositoryService;

    private CamundaWorkflowInspectorCache camundaWorkflowInspectorCache;

    @BeforeEach
    void setup() {
        camundaWorkflowInspectorCache = new CamundaWorkflowInspectorCache(processEngine);

        Mockito.lenient().when(processEngine.getRuntimeService()).thenReturn(runtimeService);
        Mockito.lenient()
                .when(runtimeService.createProcessInstanceQuery())
                .thenReturn(processInstanceQuery);
        Mockito.lenient()
                .when(processInstanceQuery.processInstanceId(any()))
                .thenReturn(processInstanceQuery);

        Mockito.lenient().when(processEngine.getHistoryService()).thenReturn(historyService);
        Mockito.lenient()
                .when(historyService.createHistoricProcessInstanceQuery())
                .thenReturn(historicProcessInstanceQuery);
        Mockito.lenient()
                .when(historicProcessInstanceQuery.processInstanceId(any()))
                .thenReturn(historicProcessInstanceQuery);

        Mockito.lenient().when(processEngine.getRepositoryService()).thenReturn(repositoryService);

        ProcessDefinition processDefinition = Mockito.mock(ProcessDefinition.class);
        Mockito.lenient()
                .when(repositoryService.getProcessDefinition(PROCESS_DEFINITION_ID))
                .thenReturn(processDefinition);
        Mockito.lenient().when(processDefinition.getDeploymentId()).thenReturn("deploymentId");
        Mockito.lenient().when(processDefinition.getResourceName()).thenReturn("resourceName");
        Mockito.lenient()
                .when(repositoryService.getResourceAsStream("deploymentId", "resourceName"))
                .thenReturn(getClass().getResourceAsStream("/TestWorkflow.bpmn"));
    }

    @Test
    void getByProcessDefinitionIdCorrectlyLoadsInspector() {
        final CamundaWorkflowInspector inspector =
                camundaWorkflowInspectorCache.getByProcessDefinitionId(PROCESS_DEFINITION_ID);

        assertNotNull(inspector);
    }

    @Test
    void getByProcessInstanceIdForActiveProcess() {
        ProcessInstance processInstance = Mockito.mock(ProcessInstance.class);
        Mockito.when(processInstance.getProcessDefinitionId()).thenReturn(PROCESS_DEFINITION_ID);
        Mockito.when(processInstanceQuery.singleResult()).thenReturn(processInstance);

        final CamundaWorkflowInspector inspector =
                camundaWorkflowInspectorCache.getByProcessInstanceId("processInstanceId");

        assertNotNull(inspector);
    }

    @Test
    void getByProcessInstanceIdForCompleteProcess() {
        HistoricProcessInstance processInstance = Mockito.mock(HistoricProcessInstance.class);
        Mockito.when(processInstance.getProcessDefinitionId()).thenReturn(PROCESS_DEFINITION_ID);
        Mockito.when(processInstanceQuery.singleResult()).thenReturn(null);
        Mockito.when(historicProcessInstanceQuery.singleResult()).thenReturn(processInstance);

        final CamundaWorkflowInspector inspector =
                camundaWorkflowInspectorCache.getByProcessInstanceId("processInstanceId");

        assertNotNull(inspector);
    }

    @Test
    void getByProcessInstanceIdThrowsExceptionIfProcessDoesntExist() {
        Mockito.when(processInstanceQuery.singleResult()).thenReturn(null);
        Mockito.when(historicProcessInstanceQuery.singleResult()).thenReturn(null);

        assertThrows(
                RuntimeException.class,
                () -> camundaWorkflowInspectorCache.getByProcessInstanceId("processInstanceId"));
    }
}
