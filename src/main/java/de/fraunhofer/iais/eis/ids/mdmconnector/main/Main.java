package de.fraunhofer.iais.eis.ids.mdmconnector.main;

import de.fraunhofer.iais.eis.ids.component.client.RemoteComponentInteractor;
import de.fraunhofer.iais.eis.ids.component.client.RemoteComponentInteractorFactory;
import de.fraunhofer.iais.eis.ids.component.client.broker.BrokerException;
import de.fraunhofer.iais.eis.ids.mdmconnector.components.RemoteBrokerLogging;
import de.fraunhofer.iais.eis.ids.component.core.Component;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.component.interaction.multipart.MultipartComponentInteractor;
import de.fraunhofer.iais.eis.ids.component.protocol.http.server.ComponentInteractorProvider;
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.DirectoryWatcher;
import de.fraunhofer.iais.eis.ids.mdmconnector.infrastructure.DynamicConnectorSelfDescription;
import de.fraunhofer.iais.eis.ids.mdmconnector.logging.LoggingInteractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@EnableAutoConfiguration(exclude = SolrAutoConfiguration.class)
@ComponentScan(basePackages = {"de.fraunhofer.iais.eis.ids.component.protocol.http.server"})

@Controller
public class Main implements ComponentInteractorProvider {
    final private Logger logger = LoggerFactory.getLogger(Main.class);

    @Value("${component.url}")
    private String componentUrl = "";

    @Value("${component.truststore}")
    private String truststore = "";

    @Value("${component.participant}")
    private String participantString = "";

    @Value("${broker.url}")
    private String brokerUrl;

    @Value("${component.modelversion}")
    private String componentModelVersion = "";

    @Value("${component.maintainer}")
    private String componentMaintainer = "";

    @Value("${artifact.directory}")
    private String artifactDir;

    @Value("${artifact.directory.main}")
    private String artifactMainDir;

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

    @Value("${daps.verify}")
    private boolean dapsVerify;

    @Value("${daps.trustedHosts}")
    private Collection<String> dapsTrustedHosts;

    @Value("${negotiation.url}")
    private String negotiationServiceURL;

    @Value("${logging.ingoing}")
    private String loggingIn;

    @Value("${logging.outgoing}")
    private String loggingOut;

    @Value("${broker.ignore}")
    private boolean brokerIgnore = false;

    @Value("${logging.ignore}")
    private boolean loggerIgnore = false;


    private MultipartComponentInteractor multipartComponentInteractor;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @PostConstruct
    private void setUp() throws Exception {
        AppConfig appConfig = new AppConfig();
        try {

            DapsInteractionConfig dapsInteractionConfig = new DapsInteractionConfig(
                    dapsUrl, keyStoreFile, keyStorePwd, keyStoreAlias, dapsUUID, dapsTrustedHosts, dapsVerify);

            Properties systemProps = System.getProperties();
            systemProps.put("javax.net.ssl.trustStore", truststore);
            System.setProperties(systemProps);
            systemProps = System.getProperties();

            DapsSecurityTokenProvider daps = null;
            try {
                //InputStream keystore = getClass().getClassLoader().getResourceAsStream(dapsInteractionConfig.keyStoreFile);
                InputStream keystore = Files.newInputStream((new File("")).toPath().resolve(dapsInteractionConfig.keyStoreFile));
                String dapsUrl = dapsInteractionConfig.dapsUrl + "/token";
                daps = new DapsSecurityTokenProvider(keystore,
                        dapsInteractionConfig.keyStorePwd,
                        dapsInteractionConfig.keyStoreAlias,
                        dapsInteractionConfig.UUID,
                        dapsUrl,
                        true, //TODO: These two should be replaced by variables
                        true);
                daps.getSecurityToken(); //fetching securityToken at start up to reduce latency to first message (token can be re-used, if it is not expired by then)
            } catch (InvalidPathException | IOException | TokenRetrievalException e) {
                e.printStackTrace();
                logger.warn("Could not create security token!", e);
            }
            // (currently) MDM-specific parts
            DirectoryWatcher watcher = new DirectoryWatcher(artifactDir);
            DynamicConnectorSelfDescription selfDescriptionProvider = new DynamicConnectorSelfDescription(new URI(componentUrl), new URI(componentMaintainer), componentModelVersion);
            LoggingInteractor loggingInteractor = new LoggingInteractor(new URI(loggingIn), new URI(loggingOut), daps, loggerIgnore);

            RemoteComponentInteractor remoteBrokerComponent = RemoteComponentInteractorFactory.getInstance().create(new URL(brokerUrl));
            RemoteBrokerLogging remoteBrokerLogging = new RemoteBrokerLogging(remoteBrokerComponent, loggingInteractor);

            //Comment

            if (!brokerIgnore) {
                try {
                    remoteBrokerLogging.update(selfDescriptionProvider.getSelfDescription(), daps.getSecurityTokenAsDAT());
                } catch (BrokerException ex) {
                    logger.error("Could not talk to the broker!", ex);
                }
            }


            Component component = appConfig.createConnectorWiring(
                    selfDescriptionProvider,
                    new URI(participantString),
                    dapsInteractionConfig,
                    remoteBrokerLogging, watcher, negotiationServiceURL, loggingInteractor, brokerIgnore);

            multipartComponentInteractor = new MultipartComponentInteractor(component, daps, selfDescriptionProvider.getSelfDescription().getId(), false);

            // start up local directory watcher and perform broker registration
            runDirectoryWatcherasThread(watcher);

        } catch (URISyntaxException | IOException e) {
            logger.error("Exception in main: ", e);
            //e.printStackTrace();
            //System.out.println(e.toString());
            throw e;
        }

    }

    private void runDirectoryWatcherasThread(DirectoryWatcher watcher) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(watcher);
    }

    public MultipartComponentInteractor getMultipartComponentInteractor() {
        return multipartComponentInteractor;
    }

    @Override
    public MultipartComponentInteractor getComponentInteractor() {
        return multipartComponentInteractor;
    }
}
