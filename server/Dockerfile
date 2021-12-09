#
# Build stage
#
FROM maven:3.8.3-openjdk-16 AS build
ARG mavenOptions
COPY / /home/app/
RUN mvn ${mavenOptions} -f /home/app/pom.xml clean install -DskipTests
RUN mvn ${mavenOptions} -f /home/app/server/pom.xml clean package -DskipTests

#
# Package stage
#
FROM tomcat:9.0.54-jdk16-openjdk AS run
COPY --from=build /home/app/server/target/server-*.war /usr/local/tomcat/webapps/flare.war
EXPOSE 8080
