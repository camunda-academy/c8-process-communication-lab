package com.camunda.academy.handler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;

public class NotifyEmployeeTravelPolicyChangedHandler implements JobHandler{

private static final Logger logger = LoggerFactory.getLogger(NotifyEmployeeTravelPolicyChangedHandler.class);

    @Override
    public void handle(JobClient client, ActivatedJob job) {
        final Map<String, Object> inputVariables = job.getVariablesAsMap();
        final String travelRequestId = (String) inputVariables.get("travelRequestId");

         logger.info(travelRequestId + " Travel Policy changed");

        //Complete the Job
        client.newCompleteCommand(job.getKey()).send().join();
    }
}