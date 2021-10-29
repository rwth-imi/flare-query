#
# Build stage
#
FROM maven:3.8.3-openjdk-16 AS build
COPY / /home/app/
RUN mvn -f /home/app/pom.xml clean install -DskipTests
RUN mvn -f /home/app/server/pom.xml clean package -DskipTests

#
# Package stage
#
FROM tomcat:9.0.54-jdk16-openjdk AS run
COPY --from=build /home/app/server/target/server-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/
EXPOSE 8080