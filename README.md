# FLARE

**F**easibi**l**ity **A**nalysis **R**equest **E**xecutor

## Goal
the goal of this project is to provide a library that allows for execution of i2b2 queries on a FHIR-server


## Setting up the environment
```docker compose -f docker-compose.yml up```

```init-testdata.sh```

## Build
First install the dependencies to your local maven repository:

``
mvn install
``

Then build the cli
```
cd flare-cli
mvn package
```

## Execution
Execution from Flare project root goes like this:

```
java -jar flare-cli/target/flare-cli-1.0-SNAPSHOT-jar-with-dependencies.jar "example-queries\immunization-example.json" "http://localhost:8080/fhir/" -f CSQ
```