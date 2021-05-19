package de.fraunhofer.iais.eis.ids.connector.acceptancetests;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.ids.connector.main.Main;
import de.fraunhofer.iais.eis.ids.connector.shared.DapsSecurityTokenProviderGenerator;
import de.fraunhofer.iais.eis.util.ConstraintViolationException;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.validation.constraints.NotNull;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * 
 * @author cmader, dkubitza
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Main.class)
@WebAppConfiguration
@TestPropertySource(locations="classpath:application-test.properties")

public class RetrieveArtifactTest {


    @Autowired
    private WebApplicationContext webApplicationContext;

	@Value("${component.url}")
	private String component = "";

	@Value("${component.maintainer}")
	private String maintainer = "";

	@Value("${component.modelversion}")
	private String modelversion = "";

	@Value("${server.port}")
	private String port = "";
	
    private final static String ARTIFACT_FILENAME = "demoArtifact.xml";
    private final static String ARTIFACT_DESCRIPTION_FILENAME = "demoArtifact-desc.jsonld";
    private final static String ARTIFACT_OFFER_FILENAME = "demoArtifact-contract.jsonld";


    @Value("${artifact.directory}")
    private String artifactDir;
    
    @Value("${artifact.directory.up}")
    private String artifactDirUpFolder;

    @Value("${daps.url}")
    private String dapsUrl;

    @Value("${daps.keystore}")
    private String keyStoreFile;

    @Value("${daps.keystorePwd}")
    private String keyStorePwd;

    @Value("${daps.keystoreAlias}")
    private String keyStoreAlias;

    @Value("${daps.UUID}")
    private String dapsUUID;

    @BeforeClass
    public static void init() {
        Main.main(new String[] {"main"});
    }


    @After
    public void cleanUp() {
        Path artifactFile = Paths.get(artifactDir, ARTIFACT_FILENAME);
        artifactFile.toFile().delete();
        Path artifactFileDesc = Paths.get(artifactDir, ARTIFACT_DESCRIPTION_FILENAME);
        artifactFileDesc.toFile().delete();
       // Path artifactFileOff = Paths.get(artifactDir, ARTIFACT_OFFER_FILENAME);
        // artifactFileOff.toFile().delete();
    }

    @Test
    public void retrieveArtifact() throws Exception {
        prepareArtifact();
        Optional<@NotNull URI> artifactUri = Optional.empty();
        while (!artifactUri.isPresent()) {
            artifactUri = extractArtifactUri(retrieveLocalSelfDescription());
        }
        Message message = createArtifactRequest(artifactUri.get());
        String response = sendArtifactRequest(message);
        Path file = Paths.get(artifactDir, ARTIFACT_FILENAME);
        Assert.assertTrue(responseContains(response, Files.readAllBytes(file)));
        removeArtifact();

    }
    
   
    //Creating multiple artifiacts DONOT DLETE the file but not considered as artifact, and so it has to be deleted from artifact directory
    private void prepareArtifact() throws IOException {
        try {
            InputStream artifact = this.getClass().getClassLoader().getResourceAsStream(ARTIFACT_FILENAME);
            InputStream artifactdesc = this.getClass().getClassLoader().getResourceAsStream(ARTIFACT_DESCRIPTION_FILENAME);
            InputStream artifactoff = this.getClass().getClassLoader().getResourceAsStream(ARTIFACT_OFFER_FILENAME);
            Files.copy(artifact, Paths.get(artifactDir, ARTIFACT_FILENAME).normalize());
            //Files.copy(artifactoff, Paths.get(artifactDir, ARTIFACT_OFFER_FILENAME).normalize());
            Files.copy(artifactdesc, Paths.get(artifactDir, ARTIFACT_DESCRIPTION_FILENAME).normalize());
        }
        catch (Exception e){
            throw(e);
        }
    }
    //3.Deleting the resource
    //Here we want to move the given artifact file which is considered as a resource to another directory so that it is no longer considered as a resource.

    public void removeArtifact() throws IOException {
        try {
           Files.delete(Paths.get(artifactDir, ARTIFACT_FILENAME));

        }
        catch (Exception e){
            throw(e);
        }
    }
    
    
  

    private String retrieveLocalSelfDescription() throws Exception {
        DapsSecurityTokenProvider daps	= DapsSecurityTokenProviderGenerator.generate(dapsUrl,keyStoreFile,keyStorePwd,keyStoreAlias,dapsUUID);

        URI requestMessageUri = new URI("http://example.org/test/request-message-1");
        Message descriptionRequestMessage = new DescriptionRequestMessageBuilder(requestMessageUri)
                ._issued_(CalendarUtil.now())
                ._issuerConnector_(new URI("http://example.org"))
                ._authorizationToken_(new TokenBuilder()
                        ._tokenValue_("dummy-token")
                        ._tokenFormat_(TokenFormat.JWT)
                        .build())
                ._modelVersion_(modelversion)
                ._senderAgent_(new URI("http://example.org"))
                ._securityToken_(daps.getSecurityTokenAsDAT())
                .build();


        MockMultipartFile header = new MockMultipartFile("header", null, "application/json", descriptionRequestMessage.toRdf().getBytes());


        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/data/")
                .file(header))
                //.file(payload)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String resultMultipartMessage = result.getResponse().getContentAsString();
        return resultMultipartMessage;
        //Old code
        //CloseableHttpClient httpclient = HttpClients.createDefault();
        //HttpGet httpGet = new HttpGet(port);
        //CloseableHttpResponse response = httpclient.execute(httpGet);

        //Writer stringWriter = new StringWriter();
        //IOUtils.copy(response.getEntity().getContent(), stringWriter, Charset.defaultCharset());
        //return stringWriter.toString();
    }

    private Optional<@NotNull URI> extractArtifactUri(String selfDescription) throws IOException {

        Serializer serializer = new Serializer();
        boolean delete = true;
        int settrue = 0;
        //System.out.println(selfDescription);
        String[] lines = selfDescription.split(System.getProperty("line.separator"));
        for(int i=1;i<lines.length;i++){
            if(lines[i].contains("ids:Artifact") && settrue == 0){delete = false; settrue=i+8;}
            if(i==settrue){delete = true;}
            if(delete){
                lines[i]="";
            }
        }
        lines[0]="";

        StringBuilder finalStringBuilder= new StringBuilder("");
        for(String s:lines){
            if(!s.equals("")){
                finalStringBuilder.append(s).append(System.getProperty("line.separator"));
            }
        }
        selfDescription = finalStringBuilder.toString();
        Artifact baseConnector = serializer.deserialize("{" + selfDescription + "}", Artifact.class);

        if (baseConnector.getId() != null) {
            System.out.println(baseConnector.getId());
            return Optional.of(baseConnector.getId());
        }
        return Optional.empty();
    }

    private Message createArtifactRequest(URI requestedArtifact) throws IOException, ConstraintViolationException, URISyntaxException, TokenRetrievalException {
        DapsSecurityTokenProvider daps	= DapsSecurityTokenProviderGenerator.generate(dapsUrl,keyStoreFile,keyStorePwd,keyStoreAlias,dapsUUID);
        Message msg = new ArtifactRequestMessageBuilder()
                ._requestedArtifact_(requestedArtifact)
                ._authorizationToken_(new TokenBuilder()._tokenValue_("sometokenvalue")._tokenFormat_(TokenFormat.JWT).build())
                ._issuerConnector_(new URI(component))
                ._issued_(CalendarUtil.now())
                ._modelVersion_(modelversion)
                ._senderAgent_(new URI("http://example.org"))
                ._securityToken_(daps.getSecurityTokenAsDAT())
                .build();
                return msg;
      //  String msgSerialized = new Serializer().serializePlainJson(msg);
      //  return MultipartEntityBuilder
       //         .create()
       //         .addTextBody("header", msgSerialized, org.apache.http.entity.ContentType.APPLICATION_JSON)
        // .build();
    }

    private String sendArtifactRequest(Message message) throws Exception {
        MockMultipartFile header = new MockMultipartFile("header", null, "application/json", message.toRdf().getBytes());

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/data/")
                .file(header))
                //.file(payload)
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String resultMultipartMessage = result.getResponse().getContentAsString();
        return resultMultipartMessage;
        //Old code

        //CloseableHttpClient httpclient = getClosableHttpClientWithoutSslVerification();
        //HttpPost httpPost = new HttpPost(new URI("http://localhost:" + port));
        //httpPost.setEntity(multipartRequestBody);
        //return httpclient.execute(httpPost);
    }

    private boolean responseContains(String response, byte[] content) throws IOException {
        return response.contains(new String(content));
    }

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
    //4.The Test asks again and checks wheter the Artificats have been deleted.
    //A check post cleanup to perform an external IO operation if the artifact is deleted successfully or not.
    @Test
    public void artifactRemoved() throws Exception {
        String SelfDescription = retrieveLocalSelfDescription();
    	System.out.println(SelfDescription);
        boolean exists = SelfDescription.contains("ARTIFACT_FILENAME");
    	if(exists==true) {
    		 Assert.assertTrue("File still exists",false );
    	}
    	else {
   		 Assert.assertTrue(ARTIFACT_FILENAME+"File deleted successfully", true);
   	}
    	
    	
  
    }
}
