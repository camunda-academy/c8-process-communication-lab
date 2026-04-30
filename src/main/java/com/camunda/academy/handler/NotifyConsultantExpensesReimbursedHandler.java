package com.camunda.academy.handler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobHandler;

public class NotifyConsultantExpensesReimbursedHandler implements JobHandler {

    private static final Logger logger = LoggerFactory.getLogger(NotifyConsultantExpensesReimbursedHandler.class);

    @Override
    public void handle(JobClient client, ActivatedJob job) throws Exception {

        final Map<String, Object> inputVariables = job.getVariablesAsMap();
        final String travelRequestId = (String) inputVariables.get("travelRequestId");

        logger.info("{} Reimbursement Request: Consultant notification sent", travelRequestId);	
        
        //Complete the Job
        client.newCompleteCommand(job.getKey()).send().join();
        
    }

}
