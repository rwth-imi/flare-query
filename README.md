# FLARE

**F**easibi**l**ity **A**nalysis **R**equest **E**xecutor

## Goal
the goal of this project is to provide a library that allows for execution of feasibility queries on a FHIR-server, e.g. from i2b2.


## Setting up the test-environment
Set up FHIR test server

```docker-compose -f docker-compose.yml up```

Load example data into FHIR server

```init-testdata.sh```

## Build
``
mvn clean install -DskipTests
``

## Execution
A CLI example project can be found under flare-cli, execution from Flare project root goes like this:

```
java -jar flare-cli/target/flare-cli-1.0-SNAPSHOT-jar-with-dependencies.jar "example-queries/immunization-example.json" "http://localhost:8080/fhir/" -f CSQ
```