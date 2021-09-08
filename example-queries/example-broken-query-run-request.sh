curl --location --request POST 'http://localhost:5000/query-sync' \
--header 'Content-Type: codex/json' \
--header 'Accept: internal/json' \
--data-raw '{
    "exclusionCriteria": [],
    "inclusionCriteria": [
        {
            "termCode": {
                "code": "81839001",
                "display": "",
                "system": "http://snomed.info/sct"
            }
        },
        {
            "termCode": {
                "code": "B01AB13",
                "display": "",
                "system": "http://fhir.de/CodeSystem/dimdi/atc"
            }
        }
    ],
    "version": ""
}'