/*-
 * #%L
 * epa-ps-sim-lib
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.epa.medication;

import static de.gematik.epa.utils.StringUtils.appendCauses;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import de.gematik.epa.api.testdriver.medication.dto.*;
import de.gematik.epa.fhir.client.FhirClient;
import de.gematik.epa.medication.client.EmlRenderClient;
import de.gematik.epa.utils.FhirUtils;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;

@Slf4j
public class MedicationService {

  protected static final String MEDICATION_REQUEST_MEDICATION = "MedicationRequest:medication";
  protected static final String MEDICATION_DISPENSE_MEDICATION = "MedicationDispense:medication";
  protected static final String PRACTITIONER_ROLE_PRACTITIONER = "PractitionerRole:practitioner";
  private final FhirClient fhirClient;
  private final EmlRenderClient emlRenderClient;

  public MedicationService(final FhirClient fhirClient, EmlRenderClient emlRenderClient) {
    this.fhirClient = fhirClient;
    this.emlRenderClient = emlRenderClient;

    FhirUtils.setJsonParser(fhirClient.getContext().newJsonParser());
  }

  private static GetMedicationResponseDTO fromThrowableToMedicationResponse(
      @NonNull Throwable throwable) {
    var statusMsgBuilder = new StringBuilder().append(throwable);

    return new GetMedicationResponseDTO()
        .success(false)
        .statusMessage(appendCauses(throwable, statusMsgBuilder).toString());
  }

  private static GetMedicationRequestListDTO fromThrowableToMedicationRequestResponse(
      @NonNull Throwable throwable) {
    var statusMsgBuilder = new StringBuilder().append(throwable);

    return new GetMedicationRequestListDTO()
        .success(false)
        .statusMessage(appendCauses(throwable, statusMsgBuilder).toString());
  }

  private static GetMedicationDispenseListDTO fromThrowableToMedicationDispenseResponse(
      @NonNull Throwable throwable) {
    var statusMsgBuilder = new StringBuilder().append(throwable);

    return new GetMedicationDispenseListDTO()
        .success(false)
        .statusMessage(appendCauses(throwable, statusMsgBuilder).toString());
  }

  private static GetMedicationListAsFhirResponseDTO fromThrowableToMedicationListAsFhirResponse(
      @NonNull Throwable throwable) {
    var statusMsgBuilder = new StringBuilder().append(throwable);

    return new GetMedicationListAsFhirResponseDTO()
        .success(false)
        .statusMessage(appendCauses(throwable, statusMsgBuilder).toString());
  }

  protected static IQuery<IBaseBundle> filterByWhenHandedOver(
      String whenhandedover, IQuery<IBaseBundle> query) {
    if (StringUtils.isNotEmpty(whenhandedover)) {
      var prefix = whenhandedover.substring(0, 2);
      whenhandedover = whenhandedover.substring(2);
      switch (prefix) {
        case "le" ->
            query =
                query.and(MedicationDispense.WHENHANDEDOVER.beforeOrEquals().day(whenhandedover));
        case "ge" ->
            query =
                query.and(MedicationDispense.WHENHANDEDOVER.afterOrEquals().day(whenhandedover));
        case "lt" ->
            query = query.and(MedicationDispense.WHENHANDEDOVER.before().day(whenhandedover));
        case "gt" ->
            query = query.and(MedicationDispense.WHENHANDEDOVER.after().day(whenhandedover));
        case "eq" ->
            query = query.and(MedicationDispense.WHENHANDEDOVER.exactly().day(whenhandedover));
        default -> log.warn("Not valid prefix found in 'whenhandedover': {}", whenhandedover);
      }
    }
    return query;
  }

  public GetMedicationResponseDTO executeGetById(String id) {
    final Medication medication;
    try {
      medication = fhirClient.getClient().read().resource(Medication.class).withId(id).execute();
      var medicationAsJson = FhirUtils.asJson(medication);
      return new GetMedicationResponseDTO()
          .medications(List.of(medicationAsJson))
          .success(Boolean.TRUE);
    } catch (ResourceNotFoundException e) {
      var statusMessage = "No medication found for ID: " + id;
      log.warn(statusMessage);
      return new GetMedicationResponseDTO().success(Boolean.TRUE).statusMessage(statusMessage);
    } catch (Exception e) {
      log.error("Error occurred during get medication by id", e);
      return fromThrowableToMedicationResponse(e);
    }
  }

  public GetMedicationResponseDTO searchMedications(final MedicationsSearch searchRequest) {

    try {
      final IGenericClient client = fhirClient.getClient();
      final IUntypedQuery<IBaseBundle> search = client.search();

      IQuery<IBaseBundle> baseQuery =
          search
              .forResource(Medication.class)
              .where(Medication.CODE.exactly().code(searchRequest.code()))
              .and(Medication.IDENTIFIER.exactly().identifier(searchRequest.identifier()))
              .and(Medication.STATUS.exactly().identifier(searchRequest.status()))
              .count(searchRequest.count())
              .offset(searchRequest.offset())
              .totalMode(FhirUtils.calculateTotalMode(searchRequest.total()));
      if (StringUtils.isNotEmpty(searchRequest.lastUpdated())) {
        baseQuery =
            baseQuery.lastUpdated(new DateRangeParam(new DateParam(searchRequest.lastUpdated())));
      }
      final Bundle result = baseQuery.returnBundle(Bundle.class).execute();

      var response = new GetMedicationResponseDTO();
      if (result.getEntry().isEmpty()) {
        var statusMessage = "No medication found for search params: " + searchRequest;
        log.warn(statusMessage);
        return response.success(Boolean.TRUE).statusMessage(statusMessage);
      }

      return response.success(Boolean.TRUE).medications(FhirUtils.extractData(result));
    } catch (Exception e) {
      log.error("Error occurred during search for medication", e);
      return fromThrowableToMedicationResponse(e);
    }
  }

  public GetMedicationListAsPdfResponseDTO getEmlAsPdf(String insurantId) {
    var response = new GetMedicationListAsPdfResponseDTO();
    var renderResponse = emlRenderClient.getEmlAsPdf(insurantId);
    response
        .success(renderResponse.httpStatusCode() == 200)
        .eml(renderResponse.pdf())
        .statusMessage(renderResponse.errorMessage());
    return response;
  }

  public GetMedicationListAsXhtmlResponseDTO getEmlAsXhtml(String insurantId) {
    var response = new GetMedicationListAsXhtmlResponseDTO();
    var renderResponse = emlRenderClient.getEmlAsXhtml(insurantId);
    response
        .success(renderResponse.httpStatusCode() == 200)
        .eml(renderResponse.xhtml())
        .statusMessage(renderResponse.errorMessage());
    return response;
  }

  public GetEmlAsFhirResponseDTO getEmlAsFhir(
      UUID requestId,
      final String insurantId,
      final String date,
      final Integer count,
      final Integer offset) {
    var response = new GetEmlAsFhirResponseDTO();
    var renderResponse =
        emlRenderClient.getMedicationList(
            insurantId,
            requestId != null ? requestId.toString() : UUID.randomUUID().toString(),
            date,
            count,
            offset);
    response
        .success(renderResponse.httpStatusCode() == 200)
        .eml(renderResponse.emlAsFhir())
        .statusMessage(renderResponse.errorMessage());
    return response;
  }

  public GetMedicationRequestListDTO searchMedicationRequests(
      MedicationsSearch medicationRequestsSearch) {

    try {

      final IGenericClient client = fhirClient.getClient();
      final IUntypedQuery<IBaseBundle> search = client.search();

      var response = new GetMedicationRequestListDTO();

      IQuery<IBaseBundle> query =
          search
              .forResource(MedicationRequest.class)
              .where(
                  MedicationRequest.STATUS.exactly().identifier(medicationRequestsSearch.status()))
              .count(medicationRequestsSearch.count())
              .offset(medicationRequestsSearch.offset());

      if (StringUtils.isNotEmpty(medicationRequestsSearch.lastUpdated())) {
        query =
            query.lastUpdated(
                new DateRangeParam(new DateParam(medicationRequestsSearch.lastUpdated())));
      }
      var result = query.totalMode(SearchTotalModeEnum.NONE).returnBundle(Bundle.class).execute();

      if (result.getEntry().isEmpty()) {
        var statusMessage =
            "No medication request found for search params: " + medicationRequestsSearch;
        log.warn(statusMessage);
        return response.success(Boolean.TRUE).statusMessage(statusMessage);
      }
      return response.success(Boolean.TRUE).medicationRequests(FhirUtils.extractData(result));
    } catch (Exception e) {
      log.error("Error occurred during search for medication request", e);
      return fromThrowableToMedicationRequestResponse(e);
    }
  }

  public GetMedicationRequestListDTO getMedicationRequestById(final String id) {
    final MedicationRequest medicationRequest;
    try {
      medicationRequest =
          fhirClient.getClient().read().resource(MedicationRequest.class).withId(id).execute();
      var medicationAsJson = FhirUtils.asJson(medicationRequest);
      return new GetMedicationRequestListDTO()
          .medicationRequests(List.of(medicationAsJson))
          .success(Boolean.TRUE);
    } catch (ResourceNotFoundException e) {
      var statusMessage = "No medication request found for ID: " + id;
      log.warn(statusMessage);
      return new GetMedicationRequestListDTO().success(Boolean.TRUE).statusMessage(statusMessage);
    } catch (Exception e) {
      log.error("Error occurred during get medication request by id", e);
      return fromThrowableToMedicationRequestResponse(e);
    }
  }

  public GetMedicationDispenseListDTO searchMedicationDispenses(
      final MedicationsSearch medicationDispensesSearch) {
    try {
      final IGenericClient client = fhirClient.getClient();
      final IUntypedQuery<IBaseBundle> search = client.search();

      var response = new GetMedicationDispenseListDTO();

      IQuery<IBaseBundle> query =
          search
              .forResource(MedicationDispense.class)
              .where(
                  MedicationDispense.STATUS
                      .exactly()
                      .identifier(medicationDispensesSearch.status()))
              .count(medicationDispensesSearch.count())
              .offset(medicationDispensesSearch.offset());

      var whenHandedOver = medicationDispensesSearch.whenhandedover();
      query = filterByWhenHandedOver(whenHandedOver, query);

      if (StringUtils.isNotEmpty(medicationDispensesSearch.lastUpdated())) {
        query =
            query.lastUpdated(
                new DateRangeParam(new DateParam(medicationDispensesSearch.lastUpdated())));
      }
      var result = query.totalMode(SearchTotalModeEnum.NONE).returnBundle(Bundle.class).execute();

      if (result.getEntry().isEmpty()) {
        var statusMessage =
            "No medication dispense found for search params: " + medicationDispensesSearch;
        log.warn(statusMessage);
        return response.success(Boolean.TRUE).statusMessage(statusMessage);
      }
      return response.success(Boolean.TRUE).medicationDispenses(FhirUtils.extractData(result));
    } catch (Exception e) {
      log.error("Error occurred during search for medication dispense", e);
      return fromThrowableToMedicationDispenseResponse(e);
    }
  }

  public GetMedicationDispenseListDTO getMedicationDispenseById(final String id) {
    final MedicationDispense medicationDispense;
    try {
      medicationDispense =
          fhirClient.getClient().read().resource(MedicationDispense.class).withId(id).execute();
      var medicationAsJson = FhirUtils.asJson(medicationDispense);
      return new GetMedicationDispenseListDTO()
          .medicationDispenses(List.of(medicationAsJson))
          .success(Boolean.TRUE);
    } catch (ResourceNotFoundException e) {
      var statusMessage = "No medication dispense found for ID: " + id;
      log.warn(statusMessage);
      return new GetMedicationDispenseListDTO().success(Boolean.TRUE).statusMessage(statusMessage);
    } catch (Exception e) {
      log.error("Error occurred during get medication dispense by id", e);
      return fromThrowableToMedicationDispenseResponse(e);
    }
  }

  public GetMedicationListAsFhirResponseDTO searchForEmlAsFhir(
      final MedicationsSearch searchRequest) {
    final GetMedicationListAsFhirResponseDTO response = new GetMedicationListAsFhirResponseDTO();
    try {
      var client = fhirClient.getClient();

      // 1. get Medication with _revinclude MedicationRequest and MedicationDispense
      final IUntypedQuery<IBaseBundle> searchMedication = client.search();
      IQuery<IBaseBundle> iBaseBundleIQuery = searchMedication.forResource(Medication.class);
      iBaseBundleIQuery =
          iBaseBundleIQuery
              .and(Medication.STATUS.exactly().identifier(searchRequest.status()))
              .count(searchRequest.count())
              .offset(searchRequest.offset())
              .revInclude(new Include(MEDICATION_REQUEST_MEDICATION))
              .revInclude(new Include(MEDICATION_DISPENSE_MEDICATION))
              .totalMode(FhirUtils.calculateTotalMode(searchRequest.total()));

      if (StringUtils.isNotEmpty(searchRequest.lastUpdated())) {
        iBaseBundleIQuery =
            iBaseBundleIQuery.lastUpdated(
                new DateRangeParam(new DateParam(searchRequest.lastUpdated())));
      }

      final Bundle medications = iBaseBundleIQuery.returnBundle(Bundle.class).execute();

      final Bundle result = new Bundle();
      result.setType(Bundle.BundleType.TRANSACTION);

      // only in case there are medications we should retrieve the other resources
      if (medications != null
          && medications.getEntry() != null
          && !medications.getEntry().isEmpty()) {
        medications.getEntry().forEach(result::addEntry);

        // 2. get PractitionerRole with _include Practitioner
        final IUntypedQuery<IBaseBundle> searchPractitioner = client.search();
        final Bundle practitioners =
            searchPractitioner
                .forResource(PractitionerRole.class)
                .include(new Include(PRACTITIONER_ROLE_PRACTITIONER))
                .returnBundle(Bundle.class)
                .execute();

        // 3. get Organization
        final IUntypedQuery<IBaseBundle> searchOrg = client.search();
        final Bundle organizations =
            searchOrg.forResource(Organization.class).returnBundle(Bundle.class).execute();

        if (practitioners != null && practitioners.getEntry() != null) {
          practitioners.getEntry().forEach(result::addEntry);
        }

        if (organizations != null && organizations.getEntry() != null) {
          organizations.getEntry().forEach(result::addEntry);
        }
      } else {
        response.statusMessage("No medication found for search params: " + searchRequest);
      }

      return response.success(Boolean.TRUE).eml(FhirUtils.asJson(result));
    } catch (Exception e) {
      log.error("Error occurred during get medication list as FHIR", e);
      return fromThrowableToMedicationListAsFhirResponse(e);
    }
  }
}
