# FLARE

**F**easibi**l**ity **A**nalysis **R**equest **E**xecutor

## Goal
the goal of this project is to provide a library that allows for execution of feasibility queries on a FHIR-server, e.g. from i2b2.


## Setting up the test-environment
Set up FHIR test server

```docker compose -f docker-compose.yml up```

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

## Web-API

### Query Format
The query example for a structured query:

````bash
curl --location --request POST 'localhost:1080/executeQuery' \
--header 'Accept-Encoding: CSQ' \
--header 'Content-Type: application/json' \
--data-raw '{
  "version": "http://to_be_decided.com/draft-1/schema#",
  "inclusionCriteria": [
    [
      {
        "termCode": {
          "code": "30525-0",
          "system": "http://loinc.org",
          "display": "Age"
        },
        "valueFilter": {
          "type": "quantity-range",
          "unit": {
            "code": "1",
            "display": ""
          },
          "minValue": 18.0,
          "maxValue": 55.0
        }
      },
      {
        "termCode": {
          "code": "763264000",
          "system": "http://snomed.info/sct",
          "display": "Canadian Study of Health and Aging Clinical Frailty Scale score (observable entity)"
        },
        "valueFilter": {
          "type": "concept",
          "selectedConcepts": [
            {
              "code": "1",
              "system": "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/frailty-score",
              "display": "Very Fit"
            },
            {
              "code": "2",
              "system": "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/frailty-score",
              "display": "Well"
            }
          ]
        }
      }
    ]
  ],
  "exclusionCriteria": [
    [
      {
        "termCode": {
          "code": "J44",
          "system": "http://fhir.de/CodeSystem/dimdi/icd-10-gm",
          "display": "Sonstige chronische obstruktive Lungenkrankheit"
        }
      }
    ],
    [
      {
        "termCode": {
          "code": "72166-2",
          "system": "http://loinc.org",
          "display": "Tobacco smoking status"
        },
        "valueFilter": {
          "type": "concept",
          "selectedConcepts": [
            {
              "code": "LA15920-4",
              "system": "http://loinc.org",
              "display": "Former smoker"
            },
            {
              "code": "LA18976-3",
              "system": "http://loinc.org",
              "display": "Current every day smoker"
            }
          ]
        }
      }
    ]
  ],
  "display": ""
}'
````

### Query Executer
The flare query executer can be reached at the endpoint "query/execute".
It returns the patient count for the specified structured query result.

### Query Translate
The flare query translater can be reached at the endpoint "query/translate".
It returns the FHIR Search Query Strings formatted according to the structured query format.

