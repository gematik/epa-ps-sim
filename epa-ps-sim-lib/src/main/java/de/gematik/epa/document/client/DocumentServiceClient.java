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
package de.gematik.epa.document.client;

import de.gematik.epa.config.AuthorInstitutionProvider;
import de.gematik.epa.config.DefaultdataProvider;
import de.gematik.epa.document.DocumentInterfaceAssembly;
import de.gematik.epa.konnektor.SmbInformationProvider;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.RemoveObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.lcm._3.SubmitObjectsRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;
import xds.document.wsdl.IDocumentManagementPortType;

@Accessors(fluent = true)
public class DocumentServiceClient extends AktensystemServiceClient {

  @Getter private IDocumentManagementPortType documentService;

  private final DefaultdataProvider defaultdataProvider;

  @Getter private AuthorInstitutionProvider authorInstitutionProvider;

  private final SmbInformationProvider smbInformationProvider;

  public DocumentServiceClient(
      @NonNull DocumentInterfaceAssembly documentInterfaceAssembly,
      @NonNull DefaultdataProvider defaultdataProvider,
      @NonNull SmbInformationProvider smbInformationProvider) {
    super(documentInterfaceAssembly);
    this.defaultdataProvider = defaultdataProvider;
    this.smbInformationProvider = smbInformationProvider;
    runInitializationSynchronized();
  }

  @Override
  protected void initialize() {
    documentService = documentInterfaceAssembly.documentService();
    if (defaultdataProvider.useAuthorInstitutionFromConfigForSubmissionSet()) {
      authorInstitutionProvider = defaultdataProvider.authorInstitutionProviderFromConfig();
    } else {
      authorInstitutionProvider = smbInformationProvider;
    }
  }

  public AdhocQueryResponse documentRegistryRegistryStoredQuery(@NonNull AdhocQueryRequest body) {
    return documentService.documentRegistryRegistryStoredQuery(body);
  }

  public RegistryResponseType documentRepositoryProvideAndRegisterDocumentSetB(
      @NonNull ProvideAndRegisterDocumentSetRequestType body) {
    return documentService.documentRepositoryProvideAndRegisterDocumentSetB(body);
  }

  public RetrieveDocumentSetResponseType documentRepositoryRetrieveDocumentSet(
      @NonNull RetrieveDocumentSetRequestType body) {
    return documentService.documentRepositoryRetrieveDocumentSet(body);
  }

  public RegistryResponseType documentRegistryDeleteDocumentSet(
      @NonNull RemoveObjectsRequest body) {
    return documentService.documentRegistryDeleteDocumentSet(body);
  }

  public RegistryResponseType updateResponderRestrictedUpdateDocumentSet(
      @NonNull SubmitObjectsRequest body) {
    return documentService.updateResponderRestrictedUpdateDocumentSet(body);
  }
}
