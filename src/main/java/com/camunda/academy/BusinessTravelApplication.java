package com.camunda.academy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.academy.handler.BookingRequestHandler;
import com.camunda.academy.handler.NotifyCustomerTravelCancelledHandler;
import com.camunda.academy.handler.NotifyCustomerTravelConfirmedHandler;
import com.camunda.academy.handler.NotifyEmployeeTravelPolicyChangedHandler;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.worker.JobWorker;
import io.camunda.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.client.impl.oauth.OAuthCredentialsProviderBuilder;

public class BusinessTravelApplication {

    private static final Logger logger = LoggerFactory.getLogger(BusinessTravelApplication.class);

    // Zeebe Client Credentials
    private static final String CAMUNDA_PROPERTIES_PATH = "src/main/resources/cluster.properties";
    private static String CAMUNDA_AUTHORIZATION_SERVER_URL;
    private static String CAMUNDA_CLIENT_ID;
    private static String CAMUNDA_CLIENT_SECRET;
    private static String CAMUNDA_TOKEN_AUDIENCE;
    private static String CAMUNDA_REST_ADDRESS;
    private static String CAMUNDA_GRPC_ADDRESS;

    //Application Details
    private static final int WORKER_TIMEOUT = 10;

    //Process Definition Details
    private static final String NOTIFY_CUSTOMER_TRAVEL_CONFIRMED_JOB_TYPE = "notifyCustomerTravelConfirmed";
    private static final String NOTIFY_EMPLOYEE_POLICY_CHANGED = "notifyEmployeeTravelPolicyChanged";

    private static final String BOOK_REQUEST_JOB_TYPE = "bookRequest";
    private static final String NOTIFY_CUSTOMER_TRAVEL_CANCELLED_JOB_TYPE = "notifyCustomerTravelCancelled";
    
    public static void main(String[] args) throws IOException {
        
        loadProperties();

        final OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder()
            .authorizationServerUrl(CAMUNDA_AUTHORIZATION_SERVER_URL)
            .audience(CAMUNDA_TOKEN_AUDIENCE)
            .clientId(CAMUNDA_CLIENT_ID)
            .clientSecret(CAMUNDA_CLIENT_SECRET)
            .build();

        try (final CamundaClient client = CamundaClient.newClientBuilder()
                .grpcAddress(URI.create(CAMUNDA_GRPC_ADDRESS))
                .restAddress(URI.create(CAMUNDA_REST_ADDRESS))
                .credentialsProvider(credentialsProvider)
                .build()) {

            //Request the Cluster Topology
            logger.info("Connected to Cluster 1: {}", client.newTopologyRequest().send().join());
            
            //Contact customer travel confirmed
            final JobWorker notifyCustomerTravelConfirmedWorker =
                    client.newWorker()
                        .jobType(NOTIFY_CUSTOMER_TRAVEL_CONFIRMED_JOB_TYPE)
                        .handler(new NotifyCustomerTravelConfirmedHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();

            //Notify Travel Policy changed
            final JobWorker notifyEmployeeTravelPolicyChangedWorker =
                    client.newWorker()
                        .jobType(NOTIFY_EMPLOYEE_POLICY_CHANGED)
                        .handler(new NotifyEmployeeTravelPolicyChangedHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();

            //Send message Start request	
            final JobWorker bookingRequestWorker =
                    client.newWorker()
                        .jobType(BOOK_REQUEST_JOB_TYPE)
                        .handler(new BookingRequestHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();
            
            final JobWorker notifyCustomerBookingCancelledWorker =
                    client.newWorker()
                        .jobType(NOTIFY_CUSTOMER_TRAVEL_CANCELLED_JOB_TYPE)
                        .handler(new NotifyCustomerTravelCancelledHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();

            //Wait for the Workers
            Scanner sc = new Scanner(System.in);
            sc.nextInt();
            sc.close();
            notifyCustomerTravelConfirmedWorker.close();
            notifyEmployeeTravelPolicyChangedWorker.close();
            bookingRequestWorker.close();
            notifyCustomerBookingCancelledWorker.close();
            
        } catch (Exception e) {
            logger.error("Application error", e);
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
