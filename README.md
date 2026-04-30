# Camunda 8 - Process Communication - Lab

Lab project for the **[Camunda 8 - Process Communication](https://academy.camunda.com/c8-process-communication/)** course on Camunda Academy.

This course provides hands-on experience with process communication patterns in Camunda 8. You will implement job workers that send and receive messages across process boundaries, enabling cross-process orchestration between a Business Travel system and a Travel Agency running on two separate Camunda clusters.

> **Difficulty:** Intermediate | **Time:** ~2h | **Platform:** Camunda 8.9.0+

## What you will learn

- Describe how processes communicate using message events in Camunda 8
- Implement job workers that publish messages to correlate with running process instances
- Configure multiple applications to connect to different Camunda clusters
- Deploy BPMN processes with message send and receive events
- Trace and test end-to-end process communication across process boundaries

## Overview

The lab uses three BPMN processes deployed across two clusters and driven by three Java applications:

**BusinessTravelApplication** — connects to **Cluster 1** (`Business Travel Process` and `Reimbursement Claim`):

| Job Type | Worker Class | Description |
|---|---|---|
| `notifyCustomerTravelConfirmed` | `NotifyCustomerTravelConfirmedHandler` | Notifies the customer that travel is confirmed |
| `notifyEmployeeTravelPolicyChanged` | `NotifyEmployeeTravelPolicyChangedHandler` | Notifies the employee of a travel policy change |

**TravelAgencyApplication** — connects to **Cluster 2** (`Travel Agency Process`):

| Job Type | Worker Class | Description |
|---|---|---|
| `bookFlightAndHotel` | `BookingFlightAndHotelHandler` | Books the flight and hotel for the travel request |
| `bookingConfirmed` | `BookingConfirmedHandler` | Sends a `msg_bookingConfirmed` message back to Cluster 1 |
| `bookingCancelled` | `BookingCancelledHandler` | Sends a `msg_bookingCancelled` message back to Cluster 1 |

**ReimbursementClaim** — connects to **Cluster 1** (`Reimbursement Claim`):

| Job Type | Worker Class | Description |
|---|---|---|
| `reimburseExpenses` | `ReimburseExpensesHandler` | Processes the expense reimbursement |
| `notifyConsultantExpensesReimbursed` | `NotifyConsultantExpensesReimbursedHandler` | Notifies the consultant that expenses have been reimbursed |

## Prerequisites

### Knowledge

- Awareness of Camunda and Job Workers
- Competent with BPMN and Java

Recommended preparatory courses:

- [Camunda - Technical Overview](https://academy.camunda.com/c8-technical-overview)
- [Camunda - Develop your first job worker (Java)](https://academy.camunda.com/c8-develop-first-worker-java)
- [Camunda - Develop Workers (Java)](https://academy.camunda.com/c8-develop-workers-java/)

### Tools & Access

- Java 21+
- Maven 3.x
- An IDE (Eclipse, IntelliJ, or VS Code)
- Two [Camunda 8 SaaS clusters](https://academy.camunda.com/c8-h2-create-cluster) with [client credentials](https://academy.camunda.com/c8-h2-create-client-credentials) — one for the Business Travel application, one for the Travel Agency

## Configuration

The Camunda client is configured via two properties files. Fill in your cluster credentials before running:

**`src/main/resources/cluster.properties`** (Cluster 1 — Business Travel & Reimbursement):

| Property | Description |
|---|---|
| `CAMUNDA_REST_ADDRESS` | REST endpoint (e.g. `https://[REGION].zeebe.camunda.io/[CLUSTER-ID]`) |
| `CAMUNDA_GRPC_ADDRESS` | gRPC endpoint (e.g. `https://[CLUSTER-ID].[REGION].zeebe.camunda.io:443`) |
| `CAMUNDA_TOKEN_AUDIENCE` | Token audience (`zeebe.camunda.io`) |
| `camunda.auth.server.url` | OAuth token URL (`https://login.cloud.camunda.io/oauth/token`) |
| `camunda.client.auth.client-id` | OAuth client ID |
| `camunda.client.auth.client-secret` | OAuth client secret |

**`src/main/resources/travelagency.cluster.properties`** (Cluster 2 — Travel Agency):

Same properties as above, filled with the credentials for the Travel Agency cluster.

For Camunda SaaS these values are available in the **API Credentials** section of the Console.

## Build

```bash
mvn clean package
```

## Run

> **Before running:** deploy the BPMN processes from `src/main/resources/app/` to the appropriate cluster using Camunda Modeler or the Web Modeler:
> - `Business Travel Process.bpmn` and `Reimbursement Claim.bpmn` → Cluster 1
> - `Travel Agency Process.bpmn` → Cluster 2

**Business Travel Application** (Cluster 1):

```bash
mvn exec:java -Dexec.mainClass="com.camunda.academy.BusinessTravelApplication"
```

**Travel Agency Application** (Cluster 2):

```bash
mvn exec:java -Dexec.mainClass="com.camunda.academy.TravelAgencyApplication"
```

**Reimbursement Claim Application** (Cluster 1):

```bash
mvn exec:java -Dexec.mainClass="com.camunda.academy.ReimbursementClaim"
```

Each application waits for user input — press any integer and **Enter** to shut down the workers gracefully.

## Project Structure

```
src/main/java/com/camunda/academy/
├── BusinessTravelApplication.java                     # Entry point — Business Travel workers (Cluster 1)
├── TravelAgencyApplication.java                       # Entry point — Travel Agency workers (Cluster 2)
├── ReimbursementClaim.java                            # Entry point — Reimbursement workers (Cluster 1)
├── handler/
│   ├── BookingCancelledHandler.java                   # Sends msg_bookingCancelled to Cluster 1
│   ├── BookingConfirmedHandler.java                   # Sends msg_bookingConfirmed to Cluster 1
│   ├── BookingFlightAndHotelHandler.java              # Handles bookFlightAndHotel job
│   ├── NotifyConsultantExpensesReimbursedHandler.java # Notifies consultant of reimbursement
│   ├── NotifyCustomerTravelConfirmedHandler.java      # Notifies customer of travel confirmation
│   ├── NotifyEmployeeTravelPolicyChangedHandler.java  # Notifies employee of policy change
│   └── ReimburseExpensesHandler.java                  # Handles expense reimbursement
└── service/
    └── CustomerService.java                           # Service for customer notifications
src/main/resources/
├── cluster.properties                                 # Connection settings for Cluster 1
├── travelagency.cluster.properties                    # Connection settings for Cluster 2
└── app/
    ├── Business Travel Process.bpmn                   # Business Travel BPMN process
    ├── Travel Agency Process.bpmn                     # Travel Agency BPMN process
    └── Reimbursement Claim.bpmn                       # Reimbursement Claim BPMN process
```

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| `io.camunda:camunda-client-java` | 8.1.0 | Camunda 8 Java client |
| `org.slf4j:slf4j-simple` | 2.0.17 | Logging |
