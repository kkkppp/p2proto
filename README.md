# Platform 2 prototype

## Overview
Repository contains sample Spring MVC application, which uses Keycloak with special federation plugin. For more information see https://github.com/kkkppp/keycloak

## Setup Instructions
Checkout Kecloak plugin first, build docker image.

Run gradlew dockerBuildAll
Then run component.sql and table.sql from src/main/resources.sql. Those scripts can be run any time to wipe database state
Then do gradlew jar dockerComposeUp

If you want to install not on localhost, create .env in docker directory, containing line HOST_URL=<your server name with protocol>
