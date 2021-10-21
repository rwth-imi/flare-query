# Example request:
````bash
curl --location --request POST 'http://localhost:8081/FLARE/query/execute/' \
--header 'Accept-Encoding: CSQ' \
--header 'Content-Type: application/json' \
--data-raw '{
  "version": "http://to_be_decided.com/draft-1/schema#",
  "inclusionCriteria": [
    [
      {
        "termCode": {
          "code": "76689-9",
          "system": "http://loinc.org",
          "display": "Sex assigned at birth"
        },
        "valueFilter": {
          "type": "concept",
          "selectedConcepts": [
            {
              "code": "female",
              "system": "http://hl7.org/fhir/administrative-gender",
              "display": "Female"
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
````bash
docker compose -f ../docker-compose.yml up
````

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
