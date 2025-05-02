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

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;

public class ReimbursementClaim {

    private static final Logger logger = LoggerFactory.getLogger(ReimbursementClaim.class);

    // Zeebe Client Credentials
    private static final String ZEEBE_PROPERTIES_PATH = "src/main/resources/cluster.properties";
    private static String ZEEBE_CLIENT_ID;
    private static String ZEEBE_CLIENT_SECRET;
    private static String ZEEBE_TOKEN_AUDIENCE;
    private static String ZEEBE_REST_ADDRESS;
    private static String ZEEBE_GRPC_ADDRESS;
    private static String ZEEBE_AUTHORIZATION_SERVER_URL;
    
    //Application Details
    private static final int WORKER_TIMEOUT = 10;

    //Process Definition Details	
    private static final String REIMBURSE_EXPENSES_JOB_TYPE = "reimburseExpenses";
    private static final String NOTIFY_CONSULTANT_EXPENSES_REIMBURSED_JOB_TYPE = "notifyConsultantExpensesReimbursed";

    public static void main(String[] args) throws IOException{

        loadProperties();

        final OAuthCredentialsProvider credentialsProvider =
                new OAuthCredentialsProviderBuilder()
                    .authorizationServerUrl(ZEEBE_AUTHORIZATION_SERVER_URL)
                    .audience(ZEEBE_TOKEN_AUDIENCE)
                    .clientId(ZEEBE_CLIENT_ID)
                    .clientSecret(ZEEBE_CLIENT_SECRET)
                    .build();
            
        try (final ZeebeClient client = ZeebeClient.newClientBuilder()
                    .grpcAddress(URI.create(ZEEBE_GRPC_ADDRESS))
                    .restAddress(URI.create(ZEEBE_REST_ADDRESS))
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
