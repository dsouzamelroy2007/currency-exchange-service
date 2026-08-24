#!/bin/bash

mvn clean
if [ "$?" -ne 0 ]; then
    echo "Maven Clean Unsuccessful!"
    exit 1
fi

mvn package
if [ "$?" -ne 0 ]; then
    echo "Maven packaging Unsuccessful!"
    exit 1
fi

echo "================================="
echo "Building docker image"
echo "================================="

docker image build -t currency-exchange-service .
if [ "$?" -ne 0 ]; then
	echo "================================="
    echo "docker image not created!"
    echo "================================="
    exit 1
else
	echo "================================="
	echo "Docker image created"
	echo "================================="
fi



docker container rm currencyexchangeservice
docker container run -d --name currencyexchangeservice -p 8080:8080 currency-exchange-service . 
if [ "$?" -ne 0 ]; then
	echo "================================="
    echo "Error starting docker container currencyexchangeservice!"
    echo "================================="
    exit 1
else
	echo "================================="
	echo "Currency exchange service started successfully"
	echo "================================="
fi
