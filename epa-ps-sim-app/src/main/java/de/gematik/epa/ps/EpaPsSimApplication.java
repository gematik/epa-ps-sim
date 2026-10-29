/*-
 * #%L
 * epa-ps-sim-app
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.epa.ps;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.security.Security;
import java.util.Set;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

/** Main class of the epa-ps-sim-app. Starts the Spring Boot context. */
@SpringBootApplication(scanBasePackages = {"de.gematik.epa.ps", "de.gematik.epa.konnektor"})
public class EpaPsSimApplication {

  static {
    /** {@link sun.security.ssl.NamedGroup#SECP192_R1} */
    System.setProperty(
        "jdk.tls.namedGroups",
        "brainpoolP256r1, brainpoolP384r1, brainpoolP512r1, secp256r1, secp384r1");

    /*
    Set SNI for all outgoing requests - necessary for tiger proxy to decide
    which server certificate to present to the client (e.g. with KOB testsuite)
    https://downloads.bouncycastle.org/fips-java/docs/BC-FJA-(D)TLSUserGuide-1.0.13.pdf Section 3.5.1
    */
    System.setProperty("org.bouncycastle.jsse.client.assumeOriginalHostName", "true");

    Security.setProperty("ssl.KeyManagerFactory.algorithm", "PKIX");
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    Security.insertProviderAt(new BouncyCastleProvider(), 1);
    Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
    Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);
  }

  @Value("${cxf.openapi.url:/openapi.json}")
  private String url = "/openapi.json";

  @Value("${cxf.openapi.resource-packages:de.gematik.epa.api.testdriver}")
  private String resourcePackages = "de.gematik.epa.api.testdriver";

  public static void main(final String[] args) {
    SpringApplication.run(EpaPsSimApplication.class, args);
  }

  @Bean
  public OpenApiFeature createOpenApiFeature() {
    final OpenApiFeature openApiFeature = new OpenApiFeature();
    openApiFeature.setPrettyPrint(true);
    openApiFeature.setScan(true);
    openApiFeature.setResourcePackages(Set.of(resourcePackages));
    openApiFeature.setSwaggerUiConfig(new SwaggerUiConfig().url(url).queryConfigEnabled(false));
    return openApiFeature;
  }

  /**
   * Create the JsonProvider as Bean, which is then used to serialize and deserialize the data,
   * which are processed at the implemented API interfaces.
   *
   * @return {@link JacksonJsonProvider}
   */
  @Bean
  public JacksonJsonProvider jsonProvider() {
    final var jsonMapper =
        JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultPropertyInclusion(
                inclusion -> inclusion.withValueInclusion(Include.NON_ABSENT))
            .addModule(new JsonNullableJackson3Module()) // differentiates between set and not set
            .build();
    final JacksonJsonProvider provider = new JacksonJsonProvider();
    provider.setMapper(jsonMapper);
    return provider;
  }
}
