package com.camunda.academy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.academy.handler.BookingCancelledHandler;
import com.camunda.academy.handler.BookingConfirmedHandler;
import com.camunda.academy.handler.BookingFlightAndHotelHandler;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.worker.JobWorker;
import io.camunda.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.client.impl.oauth.OAuthCredentialsProviderBuilder;

public class TravelAgencyApplication {

    private static final Logger logger = LoggerFactory.getLogger(TravelAgencyApplication.class);

    // Zeebe Client Credentials
    private static final String CAMUNDA_PROPERTIES_PATH = "src/main/resources/travelagency.cluster.properties";
    private static String CAMUNDA_AUTHORIZATION_SERVER_URL;
    private static String CAMUNDA_CLIENT_ID;
    private static String CAMUNDA_CLIENT_SECRET;
    private static String CAMUNDA_TOKEN_AUDIENCE;
    private static String CAMUNDA_REST_ADDRESS;
    private static String CAMUNDA_GRPC_ADDRESS;
    
    //Application Details
    private static final int WORKER_TIMEOUT = 10;
    
    //Travel Agency 
    private static final String BOOK_FLIGHT_AND_HOTEL_JOB_TYPE = "bookFlightAndHotel";
    private static final String BOOKING_CONFIRMED_JOB_TYPE = "bookingConfirmed";
    private static final String BOOKING_CANCELLED_JOB_TYPE = "bookingCancelled";
    
    public static void main(String[] args) throws IOException{

        loadProperties();

        final OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder()
            .authorizationServerUrl(CAMUNDA_AUTHORIZATION_SERVER_URL)
            .audience(CAMUNDA_TOKEN_AUDIENCE)
            .clientId(CAMUNDA_CLIENT_ID)
            .clientSecret(CAMUNDA_CLIENT_SECRET)
            .build();

        try (final CamundaClient  client = CamundaClient.newClientBuilder()
                .grpcAddress(URI.create(CAMUNDA_GRPC_ADDRESS))
                .restAddress(URI.create(CAMUNDA_REST_ADDRESS))
                .credentialsProvider(credentialsProvider)
                 .build()) {
            
            //Request the Cluster Topology
             logger.info("Connected to Cluster 2: " + client.newTopologyRequest().send().join());

            //Book flight and hotel Service task
            final JobWorker bookingFlightAndHotelWorker =
                    client.newWorker()
                        .jobType(BOOK_FLIGHT_AND_HOTEL_JOB_TYPE)
                        .handler(new BookingFlightAndHotelHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();
            
            //Send message Booking confirmation
            final JobWorker bookingConfirmedWorker =
                    client.newWorker()
                        .jobType(BOOKING_CONFIRMED_JOB_TYPE)
                        .handler(new BookingConfirmedHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();
            
            //Send message Booking cancelled
            final JobWorker bookingCancelledWorker =
                    client.newWorker()
                        .jobType(BOOKING_CANCELLED_JOB_TYPE)
                        .handler(new BookingCancelledHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();
            
            //Wait for the Workers
            Scanner sc = new Scanner(System.in);
            sc.nextInt();
            sc.close();
            bookingFlightAndHotelWorker.close();
            bookingConfirmedWorker.close();
            bookingCancelledWorker.close();
            
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
