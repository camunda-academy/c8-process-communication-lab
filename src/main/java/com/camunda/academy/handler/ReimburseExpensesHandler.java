package com.camunda.academy.handler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;

public class ReimburseExpensesHandler implements JobHandler{

private static final Logger logger = LoggerFactory.getLogger(ReimburseExpensesHandler.class);
    
    @Override
    public void handle(JobClient client, ActivatedJob job) throws Exception {
        
        final Map<String, Object> inputVariables = job.getVariablesAsMap();
        final String travelRequestId = (String) inputVariables.get("travelRequestId");	    	
        
         logger.info(travelRequestId + " Reimbursement Request: Expenses reimbursed");	
        
        //Complete the Job
        client.newCompleteCommand(job.getKey()).send().join();
        
    }

}
