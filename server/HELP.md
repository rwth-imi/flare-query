# Example request:
````bash
curl --location --request POST 'localhost:80/executeQuery' \
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


# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.5.5/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.5.5/maven-plugin/reference/html/#build-image)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.5.5/reference/htmlsingle/#boot-features-developing-web-applications)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)
