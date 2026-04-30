package com.camunda.academy.handler;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobHandler;
import io.camunda.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.client.impl.oauth.OAuthCredentialsProviderBuilder;

public class BookingRequestHandler implements JobHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingRequestHandler.class);

    // Zeebe Client Credentials
    private static final String CAMUNDA_PROPERTIES_PATH = "src/main/resources/travelagency.cluster.properties";
    private static String CAMUNDA_AUTHORIZATION_SERVER_URL;
    private static String CAMUNDA_CLIENT_ID;
    private static String CAMUNDA_CLIENT_SECRET;
    private static String CAMUNDA_TOKEN_AUDIENCE;
    private static String CAMUNDA_REST_ADDRESS;
    private static String CAMUNDA_GRPC_ADDRESS;

    //Process Definition Details
    private static final String MESSAGE_NAME = "msg_startBookingRequest";

    @Override
    public void handle(JobClient client, ActivatedJob job) throws Exception {
        
        //Obtain the Process Variables
        final Map<String, Object> inputVariables = job.getVariablesAsMap();
        final String travelRequestId = (String) inputVariables.get("travelRequestId");
        final String travelDestination = (String) inputVariables.get("travelDestination");
        final String travelDate = (String) inputVariables.get("travelDate");
        final String travelFlight = (String) inputVariables.get("travelFlight");
        final String travelHotel = (String) inputVariables.get("travelHotel");

        loadProperties();

        final OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder()
            .authorizationServerUrl(CAMUNDA_AUTHORIZATION_SERVER_URL)
            .audience(CAMUNDA_TOKEN_AUDIENCE)
            .clientId(CAMUNDA_CLIENT_ID)
            .clientSecret(CAMUNDA_CLIENT_SECRET)
            .build();

        try (final CamundaClient travelAgencyClient = CamundaClient.newClientBuilder()
                .grpcAddress(URI.create(CAMUNDA_GRPC_ADDRESS))
                .restAddress(URI.create(CAMUNDA_REST_ADDRESS))
                .credentialsProvider(credentialsProvider)
                .build()) {

            //Build the Message Variables
            final Map<String, Object> messageVariables = new HashMap<>();
            
            messageVariables.put("travelRequestId", travelRequestId);
            messageVariables.put("travelDestination", travelDestination);
            messageVariables.put("travelDate", travelDate);
            messageVariables.put("travelFlight", travelFlight);
            messageVariables.put("travelHotel", travelHotel);

            //Send the message
            travelAgencyClient.newPublishMessageCommand()
                .messageName(MESSAGE_NAME)
                .correlationKey(travelRequestId)
                .variables(messageVariables)
                .send()
                .join();

            logger.info("{} Travel Request started", travelRequestId);

            //Complete the Job
            travelAgencyClient.newCompleteCommand(job.getKey()).send().join();
        } catch (Exception e) {
            logger.error("Error sending booking request message", e);
        }
    }

    private static void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(CAMUNDA_PROPERTIES_PATH)) {
            properties.load(input);
            CAMUNDA_AUTHORIZATION_SERVER_URL = properties.getProperty("camunda.auth.server.url");
            CAMUNDA_CLIENT_ID = properties.getProperty("camunda.client.auth.client-id");
            CAMUNDA_CLIENT_SECRET = properties.getProperty("camunda.client.auth.client-secret");
            CAMUNDA_REST_ADDRESS = properties.getProperty("CAMUNDA_REST_ADDRESS");
            CAMUNDA_GRPC_ADDRESS = properties.getProperty("CAMUNDA_GRPC_ADDRESS");
            CAMUNDA_TOKEN_AUDIENCE = properties.getProperty("CAMUNDA_TOKEN_AUDIENCE");
        
        } catch (IOException e) {
            logger.error("Failed to load properties", e);
        }
    }
}
