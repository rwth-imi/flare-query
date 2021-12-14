#!/bin/bash

FILES=./testdata/*
for fhirBundle in $FILES
do
  echo "Sending Testdate bundle $fhirBundle ..."
  curl -X POST -H "Content-Type: application/json" -d @$fhirBundle http://localhost:8082/fhir
done
