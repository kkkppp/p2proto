# Platform 2 Prototype

## Overview
This repository contains a sample **Spring MVC** application that integrates with **Keycloak** using a special federation plugin.  
For more details, see: [Keycloak Repository](https://github.com/kkkppp/keycloak).

## Setup Instructions

### 1. Clone and Build the Keycloak Plugin
Before running the application, checkout and build the **Keycloak plugin**.

### 2. Build the Docker Image
Run the following command to build the Docker image:

```sh
./gradlew dockerBuildAll
```

### 3. Initialize the Database
Run the SQL scripts to set up the database structure:

```sh
psql -U <your_database_user> -d <your_database_name> -f src/main/resources/component.sql
psql -U <your_database_user> -d <your_database_name> -f src/main/resources/table.sql
```

**Note:**  
These scripts can be run **any time** to reset the database state.

### 4. Build and Start the Application
Run:

```sh
./gradlew dockerComposeUp
```

### 5. Deploy on a Remote Server (Optional)
If deploying on a server **other than localhost**, create a `.env` file inside the `docker` directory with the following content:

```ini
HOST_URL=<your_server_name_with_protocol>
```

This ensures the application recognizes the correct host.
