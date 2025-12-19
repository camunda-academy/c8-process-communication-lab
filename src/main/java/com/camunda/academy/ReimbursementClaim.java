package com.camunda.academy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.academy.handler.NotifyConsultantExpensesReimbursedHandler;
import com.camunda.academy.handler.ReimburseExpensesHandler;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.worker.JobWorker;
import io.camunda.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.client.impl.oauth.OAuthCredentialsProviderBuilder;

public class ReimbursementClaim {

    private static final Logger logger = LoggerFactory.getLogger(ReimbursementClaim.class);

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
    private static final String REIMBURSE_EXPENSES_JOB_TYPE = "reimburseExpenses";
    private static final String NOTIFY_CONSULTANT_EXPENSES_REIMBURSED_JOB_TYPE = "notifyConsultantExpensesReimbursed";

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
             logger.info("Connected to Cluster 1: " + client.newTopologyRequest().send().join());
                                    
            //Reimbursed expenses
            final JobWorker reimburseExpensesWorker =
                    client.newWorker()
                        .jobType(REIMBURSE_EXPENSES_JOB_TYPE)
                        .handler(new ReimburseExpensesHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();
            
            //Notify consultant expenses reimbursed
            final JobWorker notifyEmployeeWorker =
                    client.newWorker()
                        .jobType(NOTIFY_CONSULTANT_EXPENSES_REIMBURSED_JOB_TYPE)
                        .handler(new NotifyConsultantExpensesReimbursedHandler())
                        .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                        .open();
            
            //Wait for the Workers
            Scanner sc = new Scanner(System.in);
            sc.nextInt();
            sc.close();
            reimburseExpensesWorker.close();
            notifyEmployeeWorker.close();
            
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
