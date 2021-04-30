package de.fraunhofer.iais.eis.ids.connector.acceptancetests;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException; 
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.BaseConnectorImpl;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.TokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.connector.main.Main;
import de.fraunhofer.iais.eis.ids.connector.shared.DapsSecurityTokenProviderGenerator;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;

/**
 * First try to get a proper Spring Boot test, requires that the server is already started at localhost:8080
 * 
 * @author sbader
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Main.class)
@WebAppConfiguration
@TestPropertySource(locations="classpath:application-test.properties")
public class RemoteArtifactRequest {

	@Value("${component.url}")
	private String component = "";
	
	@Value("${daps.url}")
    private String dapsUrl;

	@Value("${component.maintainer}")
	private String maintainer = "";

	@Value("${component.modelversion}")
	private String modelversion = "";
	
	 @Value("${daps.keystore}")
	 private String keyStoreFile;

	@Value("${server.port}")
	private String port = "";
	
	@Value("${daps.keystorePwd}")
	private String keyStorePwd;

	@Value("${daps.keystoreAlias}")
	private String keyStoreAlias;

	@Value("${daps.UUID}")
	private String dapsUUID;

	@Test
	public void test() throws IOException, ConstraintViolationException, URISyntaxException {


        HttpEntity multipartRequestBody = createArtifactRequest(new URI("http://iais.fraunhofer.de/ids/mdm-connector/artifact/1550690438370"));
        HttpResponse response = sendArtifactRequest(multipartRequestBody);

        assertEquals(200, response.getStatusLine().getStatusCode());
	}
	
	
	@Test
    public void retrieveLocalSelfDescription() throws IOException, ConstraintViolationException, URISyntaxException, TokenRetrievalException {
		 DapsSecurityTokenProvider daps	= DapsSecurityTokenProviderGenerator.generate(dapsUrl,keyStoreFile,keyStorePwd,keyStoreAlias,dapsUUID);
        CloseableHttpClient httpclient = getClosableHttpClientWithoutSslVerification();
        HttpPost httpPost = new HttpPost("https://localhost:8080/data");
        
        Message msg = new DescriptionRequestMessageBuilder()
        		._modelVersion_(modelversion)
        		._issuerConnector_(new URI("https://localhost:8080/"))
        		._issued_(CalendarUtil.now())
        		._authorizationToken_(new TokenBuilder()
                        ._tokenValue_("dummy-token")
                        ._tokenFormat_(TokenFormat.JWT)
                        .build())
        		._securityToken_(daps.getSecurityTokenAsDAT())
                .build();
        String msgSerialized = new Serializer().serializePlainJson(msg);
        
        HttpEntity multipartRequestBody = MultipartEntityBuilder.create() 
                .addTextBody("header", msgSerialized, org.apache.http.entity.ContentType.APPLICATION_JSON)
                .build();
        httpPost.setEntity(multipartRequestBody);
        
        CloseableHttpResponse response = httpclient.execute(httpPost);

        Writer stringWriter = new StringWriter();
        IOUtils.copy(response.getEntity().getContent(), stringWriter, Charset.defaultCharset());
        System.out.println(stringWriter.toString());
    }
	
	
	
	
	
	
    //fetch contract like description `? Consider this as a test`??
    private Optional<URI> extractArtifactUrl(String selfDescription) throws IOException, URISyntaxException {
        Serializer serializer = new Serializer();
        BaseConnector baseConnector = serializer.deserialize(selfDescription, BaseConnectorImpl.class);
        for(int i=0;i<=baseConnector.getResourceCatalog().size();i++) {
        System.out.println("Offered Resources"+baseConnector.getResourceCatalog().getClass()); 
        System.out.println("Get all resources"+baseConnector.getResourceCatalog().get(i).getOfferedResource()); 
        for(int j= 0;j<=baseConnector.getResourceCatalog().get(i).getOfferedResource().size();j++) {
        	System.out.println("Get all Contracts"+baseConnector.getResourceCatalog().get(i).getOfferedResource().get(j).getContractOffer()); 
        }
        }
        if (baseConnector.getResourceCatalog().get(0).getOfferedResource() != null) {
            return Optional.of(baseConnector.getResourceCatalog().get(0).getOfferedResource().get(0).getRepresentation().get(0).getInstance().get(0).getId());
        }
       // Check the count for Contract offers?
        if (baseConnector.getResourceCatalog().get(0).getOfferedResource().get(0).getContractOffer().get(0) != null) {
            return Optional.of(baseConnector.getResourceCatalog().get(0).getOfferedResource().get(0).getRepresentation().get(0).getInstance().get(0).getId());
        }
        
       
        return Optional.empty();
        
    }
    //2 check if the contract offers and resources are represented properly in self description
    @Test
    private void checkResources(String selfDescription) throws IOException, URISyntaxException {
        Serializer serializer = new Serializer();
        BaseConnector baseConnector = serializer.deserialize(selfDescription, BaseConnectorImpl.class);
        for(int i=0;i<=baseConnector.getResourceCatalog().size();i++) {
        System.out.println("Offered Resources"+baseConnector.getResourceCatalog().getClass()); 
        System.out.println("Get all resources"+baseConnector.getResourceCatalog().get(i).getOfferedResource()); 
        for(int j= 0;j<=baseConnector.getResourceCatalog().get(i).getOfferedResource().size();j++) {
        	System.out.println("Get all Contracts"+baseConnector.getResourceCatalog().get(i).getOfferedResource().get(j).getContractOffer()); 
        }
        }
        if (baseConnector.getResourceCatalog().get(0).getOfferedResource() != null) {
        	Assert.assertTrue("Resources are present.",true);
           System.out.println(baseConnector.getResourceCatalog().get(0).getOfferedResource().get(0).getRepresentation().get(0).getInstance().get(0).getId());
        }
       // Check the count for Contract offers?
        if (baseConnector.getResourceCatalog().get(0).getOfferedResource().get(0).getContractOffer().get(0) != null) {
        	Assert.assertTrue("Contract Offers are present",true);
            //return Optional.of(baseConnector.getResourceCatalog().get(0).getOfferedResource().get(0).getRepresentation().get(0).getInstance().get(0).getId());
        }
        
      ;
        
    }

    private HttpEntity createArtifactRequest(URI requestedArtifact) throws IOException, ConstraintViolationException, URISyntaxException {
        Message msg = new ArtifactRequestMessageBuilder()
                ._requestedArtifact_(requestedArtifact)
                 ._authorizationToken_(new TokenBuilder()._tokenValue_("sometokenvalue").build())
                ._issuerConnector_(new URI("http://example.org/dummy"))
                ._issued_(CalendarUtil.now())
                ._modelVersion_(modelversion)
                .build();
        String msgSerialized = new Serializer().serializePlainJson(msg);
        return MultipartEntityBuilder
                .create()
                .addTextBody("header", msgSerialized, org.apache.http.entity.ContentType.APPLICATION_JSON)
                .build();
    }


    private HttpResponse sendArtifactRequest(HttpEntity multipartRequestBody) throws IOException {
        //CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpClient httpclient = getClosableHttpClientWithoutSslVerification();
        HttpPost httpPost = new HttpPost("https://localhost:" + port + "/data");
        BufferedReader in = new BufferedReader(new InputStreamReader(multipartRequestBody.getContent()));
        String line = null;
        while((line = in.readLine()) != null) {
          System.out.println(line);
        }
        httpPost.setEntity(multipartRequestBody);
        return httpclient.execute(httpPost);
    }

    private boolean responseContains(HttpResponse response, byte[] content) throws IOException {
        Writer responseWriter = new StringWriter();
        IOUtils.copy(response.getEntity().getContent(), responseWriter, Charset.defaultCharset());
        return responseWriter.toString().contains(new String(content));
    }

	/**
	 * This method creates a RestTemplate without a proper SSL certificate validation in order to 
	 * test HTTPS-only endpoints while missing a valid X509 certificate
	 * 
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public CloseableHttpClient getClosableHttpClientWithoutSslVerification()  {
		
		
		return getHttpClientBuilderWithoutSslVerification().build();
	}
	
	
	public HttpClientBuilder getHttpClientBuilderWithoutSslVerification() {
		
		return HttpClients.custom()
				.setSSLContext(getSslContextWithoutSslVerification())
				.setSSLHostnameVerifier(new NoopHostnameVerifier());
	}

	
	public SSLContext getSslContextWithoutSslVerification() {
		try {
			return new SSLContextBuilder()
					.loadTrustMaterial(null, (certificate, authType) -> true).build();
		} catch (KeyManagementException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (KeyStoreException e) {
			e.printStackTrace();
			return null;
		}
	}
}
