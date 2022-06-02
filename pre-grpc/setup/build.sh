#!/bin/sh
cd ~
echo "Do docker login first to push images"
git clone https://github.com/kbhattac/CoolRetailer
cd CoolRetailer
mvn clean install -DskipTests
cd target
cp ../src/main/resources/Dockerfile .
docker build . -t kbhattac/coolretailer:v4
docker push kbhattac/coolretailer:v4
