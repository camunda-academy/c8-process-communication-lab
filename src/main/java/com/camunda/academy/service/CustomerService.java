package com.camunda.academy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    public void notifyTravelConfirmed(String travelRequestId) {
        logger.info("{} Business Travel confirmed", travelRequestId);
    }

    public void notifyTravelCancelled(String travelRequestId) {
        logger.info("{} Business Travel cancelled", travelRequestId);
    }

}