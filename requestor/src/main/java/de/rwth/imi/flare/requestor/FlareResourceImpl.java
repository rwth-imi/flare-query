package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.FlareIdDateWrap;
import de.rwth.imi.flare.api.FlareResource;
import org.hl7.fhir.r4.model.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * FlareResource, represents a single FHIR Resource and it's associated patient
 */
public class FlareResourceImpl implements FlareResource {
    private String patientId;
    private FlareIdDateWrap idDateWrap = new FlareIdDateWrap();

    private final Resource underlyingFhirResource;

    /**
     * Constructs a FlareResource representing a given FHIR Resource
     * @param resource to base the FlareResource upon
     */
    public FlareResourceImpl(Resource resource){
        this.underlyingFhirResource = resource;
        this.extractId();
        this.extractSetDate();
    }

    /**
     * Extracts the patientId from the {@link #underlyingFhirResource} into {@link #patientId}
     */
    private void extractId() {
        ResourceType resourceType = this.underlyingFhirResource.getResourceType();

        switch (resourceType) {
            case Observation -> this.patientId = extractId((Observation) this.underlyingFhirResource);
            case Patient -> this.patientId = extractId((Patient) this.underlyingFhirResource);
            case Condition -> this.patientId = extractId((Condition) this.underlyingFhirResource);
            case Specimen -> this.patientId = extractId((Specimen) this.underlyingFhirResource);
            case Encounter -> this.patientId = extractId((Encounter) this.underlyingFhirResource);
            case Procedure -> this.patientId = extractId((Procedure) this.underlyingFhirResource);
            case MedicationAdministration -> this.patientId = extractId((MedicationAdministration) this.underlyingFhirResource);
            case MedicationStatement -> this.patientId = extractId((MedicationStatement) this.underlyingFhirResource);
            case Immunization -> this.patientId = extractId((Immunization) this.underlyingFhirResource);
            case Consent -> this.patientId = extractId((Consent) this.underlyingFhirResource);
            case DiagnosticReport -> this.patientId = extractId((DiagnosticReport) this.underlyingFhirResource);
        }

    }

    private void extractSetDate(){
        ResourceType resourceType = this.underlyingFhirResource.getResourceType();

        switch (resourceType) {
            case Observation -> extractSetDate((Observation) this.underlyingFhirResource);
            case Patient -> extractSetDate((Patient) this.underlyingFhirResource);
            case Condition -> extractSetDate((Condition) this.underlyingFhirResource);
            case Specimen -> extractSetDate((Specimen) this.underlyingFhirResource);
            case Encounter -> extractSetDate((Encounter) this.underlyingFhirResource);
            case Procedure -> extractSetDate((Procedure) this.underlyingFhirResource);
            case MedicationAdministration -> extractSetDate((MedicationAdministration) this.underlyingFhirResource);
            case MedicationStatement -> extractSetDate((MedicationStatement) this.underlyingFhirResource);
            case Immunization -> extractSetDate((Immunization) this.underlyingFhirResource);
            case Consent -> extractSetDate((Consent) this.underlyingFhirResource);
            case DiagnosticReport -> extractSetDate((DiagnosticReport) this.underlyingFhirResource);
        }
    }

    private String extractId(Specimen resource) {
        return resource.getSubject().getReferenceElement().getIdPart();
    }

    private String extractId(Encounter resource) {
        return resource.getSubject().getReferenceElement().getIdPart();
    }

    private String extractId(Condition resource) {
        return resource.getSubject().getReferenceElement().getIdPart();
    }

    private String extractId(Patient patient) {
        return patient.getIdElement().getIdPart();
    }

    private String extractId(Procedure resource) {
        return resource.getSubject().getReferenceElement().getIdPart();
    }

    private String extractId(Observation resource) {
        return resource.getSubject().getReferenceElement().getIdPart();
    }

    private String extractId(MedicationAdministration resource) {
        return resource.getSubject().getReferenceElement().getIdPart();
    }

    private String extractId(MedicationStatement resource) {
        return resource.getSubject().getReferenceElement().getIdPart();
    }

    private String extractId(Immunization resource) {
        return resource.getPatient().getReferenceElement().getIdPart();
    }

    private String extractId(Consent resource) {
        return resource.getPatient().getReferenceElement().getIdPart();
    }

    private String extractId(DiagnosticReport resource) {
        return resource.getSubject().getReferenceElement().getIdPart();
    }

    //TODO handle if date not found
    //TODO some dates might might have to be gotten from another part of the resource
    private void extractSetDate(Specimen resource) {
        this.idDateWrap.startDate = resource.getCollection().getCollectedPeriod().getStart();
        this.idDateWrap.endDate = resource.getCollection().getCollectedPeriod().getEnd();
    }

    private void extractSetDate(Encounter resource) {
        this.idDateWrap.startDate = resource.getPeriod().getStart();
        this.idDateWrap.endDate = resource.getPeriod().getEnd();
    }

    private void extractSetDate(Condition resource) {
        this.idDateWrap.startDate = resource.getOnsetPeriod().getStart();
        this.idDateWrap.endDate = resource.getOnsetPeriod().getEnd();
    }

    private void extractSetDate(Patient patient) {
        //TODO this is just a placeholder as i didn't know if and what date to set here
        this.idDateWrap.startDate = Date.from(LocalDate.parse("1980-01-01").atStartOfDay().toInstant(ZoneOffset.UTC));
        this.idDateWrap.endDate = Date.from(LocalDate.parse("2100-01-01").atStartOfDay().toInstant(ZoneOffset.UTC));

    }

    private void extractSetDate(Procedure resource) {
        this.idDateWrap.startDate = resource.getPerformedPeriod().getStart();
        this.idDateWrap.endDate = resource.getPerformedPeriod().getEnd();
    }

    private void extractSetDate(Observation resource) {
        this.idDateWrap.startDate = resource.getEffectivePeriod().getStart();
        this.idDateWrap.endDate = resource.getEffectivePeriod().getEnd();
    }

    private void extractSetDate(MedicationAdministration resource) {
        this.idDateWrap.startDate = resource.getEffectivePeriod().getStart();
        this.idDateWrap.endDate = resource.getEffectivePeriod().getEnd();
    }

    private void extractSetDate(MedicationStatement resource) {
        this.idDateWrap.startDate = resource.getEffectivePeriod().getStart();
        this.idDateWrap.endDate = resource.getEffectivePeriod().getEnd();
    }

    private void extractSetDate(Immunization resource) {
        this.idDateWrap.startDate = resource.getOccurrence().dateTimeValue().getValue();
        this.idDateWrap.endDate = resource.getOccurrence().dateTimeValue().getValue();
    }

    private void extractSetDate(Consent resource) {
        this.idDateWrap.startDate = resource.getProvision().getProvision().get(0).getPeriod().getStart();
        this.idDateWrap.endDate = resource.getProvision().getProvision().get(0).getPeriod().getEnd();
    }

    private void extractSetDate(DiagnosticReport resource) {
        this.idDateWrap.startDate = resource.getEffectivePeriod().getStart();
        this.idDateWrap.endDate = resource.getEffectivePeriod().getEnd();
    }

    @Override
    public String getPatientId() {
        return this.patientId;
    }

     @Override
    public FlareIdDateWrap getIdDateWrap(){
        this.idDateWrap.patId = this.patientId;
        return this.idDateWrap;
    }

}
