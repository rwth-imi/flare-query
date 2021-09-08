curl --location --request POST 'http://localhost:5000/query-sync' \
--header 'Content-Type: codex/json' \
--header 'Accept: internal/json' \
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