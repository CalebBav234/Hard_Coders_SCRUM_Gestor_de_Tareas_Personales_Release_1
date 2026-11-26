package com.hardcoders.taskmanager.bdd.context;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Estado aislado de un escenario de Cucumber.
 * Es un bean singleton compartido entre Hooks y los steps del mismo escenario.
 * El estado se reinicia antes de cada escenario mediante reset() en los hooks,
 * por lo que no hay filtros de informacion entre escenarios.
 */
@Component
public class ScenarioContext {

    private Integer projectId;
    private Integer stageId;
    private Integer activityId;
    private Integer taskId;
    private Integer subtaskId;
    private Integer workerUserId;
    private Integer createdUserId;
    private String authToken;
    private String chiefEmail;
    private ResponseEntity lastPutResponse;
    private ResponseEntity lastGetResponse;
    private ResponseEntity lastListResponse;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getStageId() {
        return stageId;
    }

    public void setStageId(Integer stageId) {
        this.stageId = stageId;
    }

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Integer getSubtaskId() {
        return subtaskId;
    }

    public void setSubtaskId(Integer subtaskId) {
        this.subtaskId = subtaskId;
    }

    public Integer getWorkerUserId() {
        return workerUserId;
    }

    public void setWorkerUserId(Integer workerUserId) {
        this.workerUserId = workerUserId;
    }

    public Integer getCreatedUserId() {
        return createdUserId;
    }

    public void setCreatedUserId(Integer createdUserId) {
        this.createdUserId = createdUserId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getChiefEmail() {
        return chiefEmail;
    }

    public void setChiefEmail(String chiefEmail) {
        this.chiefEmail = chiefEmail;
    }

    public ResponseEntity getLastPutResponse() {
        return lastPutResponse;
    }

    public void setLastPutResponse(ResponseEntity lastPutResponse) {
        this.lastPutResponse = lastPutResponse;
    }

    public ResponseEntity getLastGetResponse() {
        return lastGetResponse;
    }

    public void setLastGetResponse(ResponseEntity lastGetResponse) {
        this.lastGetResponse = lastGetResponse;
    }

    public ResponseEntity getLastListResponse() {
        return lastListResponse;
    }

    public void setLastListResponse(ResponseEntity lastListResponse) {
        this.lastListResponse = lastListResponse;
    }

    public void reset() {
        this.projectId = null;
        this.stageId = null;
        this.activityId = null;
        this.taskId = null;
        this.subtaskId = null;
        this.workerUserId = null;
        this.createdUserId = null;
        this.authToken = null;
        this.chiefEmail = null;
        this.lastPutResponse = null;
        this.lastGetResponse = null;
        this.lastListResponse = null;
    }

    public boolean isComplete() {
        return projectId != null && stageId != null && activityId != null
                && taskId != null && subtaskId != null;
    }
}
