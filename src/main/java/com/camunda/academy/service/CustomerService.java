package com.camunda.academy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerService {

private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    public void notifyTravelConfirmed(String travelRequestId) {

     logger.info(travelRequestId + " Business Travel confirmed");
    }
    
    public void notifyTravelCancelled(String travelRequestId) {

     logger.info(travelRequestId + " Bussiness Travel cancelled");
    }
    
    
}