#!/bin/sh
echo "Starting DCEMS ... (make sure MySQL is running and src/main/resources/db.properties is correct)"
mvn -q javafx:run
