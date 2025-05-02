package com.camunda.academy.handler;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;

public class BookingCancelledHandler implements JobHandler{

    private static final Logger logger = LoggerFactory.getLogger(BookingCancelledHandler.class);
    
    // Zeebe Client Credentials
    private static final String ZEEBE_PROPERTIES_PATH = "src/main/resources/cluster.properties";
    private static String ZEEBE_CLIENT_ID;
    private static String ZEEBE_CLIENT_SECRET;
    private static String ZEEBE_TOKEN_AUDIENCE;
    private static String ZEEBE_REST_ADDRESS;
    private static String ZEEBE_GRPC_ADDRESS;
    private static String ZEEBE_AUTHORIZATION_SERVER_URL;

    //Process Definition Details
    private static final String MESSAGE_NAME = "msg_bookingCancelled";

    @Override
    public void handle(JobClient client, ActivatedJob job) throws Exception {
    
    //Obtain the Process Variables
    final Map<String, Object> inputVariables = job.getVariablesAsMap();
    final String travelRequestId = (String) inputVariables.get("travelRequestId");
    final String travelDestination = (String) inputVariables.get("travelDestination");
    final String travelDate = (String) inputVariables.get("travelDate");
    final String travelFlight =  (String) inputVariables.get("travelFlight");
    final String travelHotel =  (String) inputVariables.get("travelHotel");
    
    loadProperties();
    
    final OAuthCredentialsProvider credentialsProvider =
            new OAuthCredentialsProviderBuilder()
                .authorizationServerUrl(ZEEBE_AUTHORIZATION_SERVER_URL)
                .audience(ZEEBE_TOKEN_AUDIENCE)
                .clientId(ZEEBE_CLIENT_ID)
                .clientSecret(ZEEBE_CLIENT_SECRET)
                .build();
        
    try (final ZeebeClient consultantClient = ZeebeClient.newClientBuilder()
                .grpcAddress(URI.create(ZEEBE_GRPC_ADDRESS))
                .restAddress(URI.create(ZEEBE_REST_ADDRESS))
                .credentialsProvider(credentialsProvider)
                .build()) {
    
        //Build the Message Variables
        final Map<String, Object> messageVariables = new HashMap<String, Object>();
        
        messageVariables.put("travelRequestId", travelRequestId);
        messageVariables.put("travelDestination", travelDestination);
        messageVariables.put("travelDate", travelDate);
        messageVariables.put("travelFlight", travelFlight);
        messageVariables.put("travelHotel", travelHotel);
                    
        //Send the message
        consultantClient.newPublishMessageCommand()
            .messageName(MESSAGE_NAME)
            .correlationKey(travelRequestId)
            .variables(messageVariables)
            .send()
            .join();
        
         logger.info(travelRequestId + " Travel Request: Booking cancellation sent");	
        
        //Complete the Job
        client.newCompleteCommand(job.getKey()).variables(messageVariables).send().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(ZEEBE_PROPERTIES_PATH)) {
            properties.load(input); 
            ZEEBE_CLIENT_ID = properties.getProperty("ZEEBE_CLIENT_ID");
            ZEEBE_CLIENT_SECRET = properties.getProperty("ZEEBE_CLIENT_SECRET");
            ZEEBE_REST_ADDRESS = properties.getProperty("ZEEBE_REST_ADDRESS");
            ZEEBE_GRPC_ADDRESS = properties.getProperty("ZEEBE_GRPC_ADDRESS");
            ZEEBE_TOKEN_AUDIENCE = properties.getProperty("ZEEBE_TOKEN_AUDIENCE");
            ZEEBE_AUTHORIZATION_SERVER_URL = properties.getProperty("ZEEBE_AUTHORIZATION_SERVER_URL");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
