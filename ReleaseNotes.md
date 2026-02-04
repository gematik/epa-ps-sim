<img align="right" width="200" height="37" src="Gematik_Logo_Flag_With_Background.png" alt="Gematik Logo"/> <br/>

# Release notes ePA-PS-Sim

## Release 2.2.0 (contains ePA 3.0.5 and dgMP parts of upcoming ePA 3.1.3)

### added
- PoPP (Proof of Patient Presence) Token Generator and test scenario support
- Medication Service (eML/eMP):
    - `$add-eml-entry`: add new eML entries
    - `$update-emp-entry`: update eMP entries
    - `$cancel-eml-entry`: cancel eML entries
    - `$link-emp`: add eML-eMP linking
    - `$medication-plan-log`: retrieve eMP logs (provenance)
    - Get eMP as PDF via new render API endpoint
- Document Management: support AppendDocument
- MHD Service: support Find Document References (MHD search)
- VAU: New endpoint `/destroy` to destroy VAU session
- Format query parameter (`_format`) to control format of eML as FHIR (JSON or XML)
    - Test driver endpoint `/medication/render/eml/fhir` extended with `format` parameter
    - Default format remains JSON when no parameter is provided

### changed
- Upgraded epa-medication-render API client to support new operations
- Auto-unlock all SMBs on application start
- Enhanced logging (card info, null author handling)
- Upgraded version of lib-ihe-xds to 3.0.3 (https://github.com/gematik/lib-ihe-xds/blob/main/ReleaseNotes.md#release-303)
- Upgraded spring-boot version to 4.0.2
- Upgraded spring-web version to 7.0.3


## Release 2.1.0
### added
- Added copyright header to all files

### changed
- Use only ECC instead of RSA in all relevant places
- Removed deprecated phrService und phrMgmtService
- Select author institution related to the logged telematikId
- Select SMCB on the basis of OID
- Upgraded version of lib-ihe-xds to 3.0.0 (https://github.com/gematik/lib-ihe-xds/blob/main/ReleaseNotes.md#release-300)
- Upgraded spring-web version to 6.2.8
- Upgraded IDP-Client to version 29.2.4

### fixes
- Serialisation problem in consentDecision response

## Release 2.0.3

### required
- needs updated version of vau-proxy-client 1.0.19 (https://hub.docker.com/r/gematik1/epa-vau-proxy-client/tags?name=1.0.19)

### added
- new endpoint to remove active VAU Session (VAU-CID) on client side 
  - recreate new VAU Session to apply entitlement changes
- check VAU status before starting OIDC Flow to 
  - prevent an attempt to create a new user session when a session is still active
- entitlement client can handle HTTP status code 423
- new endpoint to get a medication list (eML) rendered as FHIR search bundle
- changed logic to localize health record
  - continuation of the search for the health record in the next health record system if HTTP 204 status code is not received (esp. HTTP status code 409)

### fixes
- HCV for defining a claim (entitlement) must be base64-encoded and uses charset ISO-8859-15

### changed
- Upgrade IDP-Client to version 29.1.7 (https://github.com/gematik/ref-idp-server/blob/master/ReleaseNotes.md)
- Upgraded spring-boot version to 3.4.4
- Upgraded HAPI FHIR client version to 8.0.0

## Release 2.0.2

### added
- Added support for Pruefziffer with HCV (C_12143)
- Card-API implementation
- Added filtering for eML FHIR retrieval

### changed
- Upgraded spring-boot version to 3.4.2
- Upgraded version of lib-ihe-xds to 2.0.2 (https://github.com/gematik/lib-ihe-xds/blob/main/ReleaseNotes.md#release-202)
- Enabled signDocument() operation in Signature-API

## Release 2.0.1

### added
- Retrieve eML as FHIR
- Added date based filtering for eML retrieval

### changed
- Updated PS-Testdriver API
- Extracting nonce for login (PS) from redirect response

## Release 2.0.0

### added

- Published epa-ps-sim docker image on Docker Hub
- Added client and API for:
    - InformationService,
    - MedicationService,
    - EntitlementService,
    - EmailManagementService,
    - ConsentDecisionService,
    - AuthenticationService
    - AuditService
- Integration and implementation of KOB PS Testdriver
- Added x-insurantId header parameter to XDS operation
- Support for RestrictedUpdateDocumentSet operation
- Support for FindDocumentByReferenceId query
- Support for FindDocumentsByComment query

### changed

- Updated version of lib-ihe-xds to 2.0.1 in pom.xml
- Updated APIs and XDSDocumentService.wsdl to version 3.0.3
- Optimized error handling and added request/response logging
- Updated existing XDS operations on new WSDL
- Updated to Spring Boot 3.0.13

## Release 1.2.4

### changed

- changed algorithm of authentication JWT to ES256
- Updated PS-Testdriver API to 0.1.0
- updated API specs of information and entitlement

## Release 1.2.3

### fixed

- Wrong version information for lib-ihe-xds in pom

## Release 1.2.2

### fixed

- Updated releaseChain.jenkinsfile - use variable TAGNAME for publishing

## Release 1.2.1

### fixed

- Missing copyright header

## Release 1.2.0

### added

- Additional operation configureKonnektor
    - Change the configuration relevant for the communication with a Konnektor at runtime
- Additional operation readVSD
- Feature unlock SMC-B:
    - Automatically triggered SMC-B unlock during the start of the simulation
- Add folder code DIGA

### changed

- Moved the Apache CXF generation of the client-side
  Konnektor Webservices implementation from the epa-ps-sim-app module to the epa-ps-sim-lib module
- Updated PHRManagementService on 2.5 Version

## Release 1.1.1

### fixed

- Wrong version information for lib-ihe-xds in pom

## Release 1.1.0

### added

- Additional operations:
    - replaceDocuments
    - permissionHcpo (a.k.a. requestFacilityAuthorization)
    - getAuthorizationState

## Release 1.0.0

- Initial version (internal only)
- Available operations:
    - putDocuments
    - getDocuments
    - find
    - deleteObjects

