package de.fraunhofer.iais.eis.ids.mdmconnector.infrastructure;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.InfomodelFormalException;
import de.fraunhofer.iais.eis.ids.component.core.util.CalendarUtil;
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.ArtifactIndex;
import de.fraunhofer.iais.eis.util.PlainLiteral;
import de.fraunhofer.iais.eis.util.RdfResource;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import de.fraunhofer.iais.eis.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static de.fraunhofer.iais.eis.util.Util.asList;

//import de.fraunhofer.iais.eis.*;

public class DynamicConnectorSelfDescription implements DynamicArtifactSelfDescriptionProvider {

    final private Logger logger = LoggerFactory.getLogger(DynamicConnectorSelfDescription.class);

    @NotNull
    final URI componentId;

    private final URI maintainerId;
    private final String modelVersion;
    private ArtifactIndex artifacts;

    public DynamicConnectorSelfDescription(@NotNull URI component, URI maintainer, String modelVersion) {
        this.componentId = component;
        this.maintainerId = maintainer;
        this.modelVersion = modelVersion;
    }

    @Override
    public synchronized InfrastructureComponent getSelfDescription() throws InfomodelFormalException {

        ResourceCatalog catalog = new ResourceCatalogBuilder()
                ._offeredResource_(createResources()).build();

//        AuditGuarantee autitAuditLoggingGuarantee;
//        AuthenticationGuarantee authenticationGuarantee;
//        IntegrityGuarantee integrityGuarantee;
//        LocalDataConfidentialityGuarantee localDataConfidentiality;
//        ServiceIsolationGuarantee serviceIsolationGuarantee;
//		UsageControlGuarantee usageControlGuarantee;
//		try {
//			autitAuditLoggingGuarantee = new AuditGuaranteeBuilder(new URI("https://w3id.org/idsa/code/security_guarantee/AUDIT_NONE")).build();
//			authenticationGuarantee = new AuthenticationGuaranteeBuilder(new URI("https://w3id.org/idsa/code/security_guarantee/AUTHENTICATION_NONE")).build();
//			integrityGuarantee = new IntegrityGuaranteeBuilder(new URI("https://w3id.org/idsa/code/security_guarantee/INTEGRITY_PROTECTION_NONE")).build();
//			localDataConfidentiality = new LocalDataConfidentialityGuaranteeBuilder(new URI("https://w3id.org/idsa/code/security_guarantee/LOCAL_DATA_CONFIDENTIALITY_NONE")).build();
//			serviceIsolationGuarantee = new ServiceIsolationGuaranteeBuilder(new URI("https://w3id.org/idsa/code/security_guarantee/SERVICE_ISOLATION_NONE")).build();
//			usageControlGuarantee = new UsageControlGuaranteeBuilder(new URI("https://w3id.org/idsa/code/security_guarantee/USAGE_CONTROL_NONE")).build();
//		} catch (ConstraintViolationException | URISyntaxException e) {
//			logger.error("Error parsing the self-describing security profile.", e);
//			throw new InfomodelFormalException(e);
//		}

       return new BaseConnectorBuilder(componentId)
                ._title_(Util.asList(new TypedLiteral("Enterprise Integration Connector", "en")))
                ._description_(asList(new TypedLiteral("Connector that serves static Artifacts", "en")))
                ._maintainer_(maintainerId)
                ._curator_(maintainerId)
                ._inboundModelVersion_(Util.asList(modelVersion))
                ._outboundModelVersion_(modelVersion)
                ._resourceCatalog_(Util.asList(catalog))
                ._securityProfile_(SecurityProfile.BASE_SECURITY_PROFILE)
                .build();
    }

    private ArrayList<Resource> createResources() {
        ArrayList<Resource> resources = new ArrayList<>();

        if (artifacts != null)
            for (Artifact artifact : artifacts.getAllArtifacts()) {
                /*
                 * contract precedence: contract file, contract from description, default contract
                 *
                 * - only use generated resource description if none is provided via a -desc.jsonld file
                 * - only use default contract if a contract is present neither inside a/the -desc.jsonld file or the -contract.jsonld file
                 * - overwrite contract from -desc.jsonld file with the one from -contract.jsonld file, if present
                 */
                Resource description = artifacts.getDescription(artifact);
                Contract contract = artifacts.getContract(artifact);

                if (description == null) {
                    description = new ResourceBuilder()
                            ._representation_(Util.asList(createRepresentation(artifact)))
                            .build();
                }
                if (contract == null && description.getContractOffer() == null) {
                    contract = createDefaultContract(artifact.getId());
                }
                if (contract != null) {
                    try {
                        Method contractSetter = description.getClass().getMethod("setContractOffer", ArrayList.class);
                        contractSetter.invoke(description, Util.asList((ContractOffer) contract));
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        logger.warn("reflection error while adding contract to the artifact description of " + artifact.getFileName());
                        e.printStackTrace();
                    }
                }
                resources.add(description);
            }


        if (resources.isEmpty()) {

            try {
                ArtifactBuilder artifactBuilder = new ArtifactBuilder(new URI("https://w3id.org/idsa/autogen/artifact/7e7a14bb-bde0-4c17-8d01-09e0f39004fc"));

                DataResourceBuilder resourceBuilder = new DataResourceBuilder()
                        ._representation_(Util.asList(createRepresentation(artifactBuilder.build())));

                ArrayList<ContractOffer> offers = new ArrayList<ContractOffer>();
                //offers.add((ContractOffer) createDefaultContract(new URI("http://example.org/contracts/dummy/")));
                resourceBuilder._contractOffer_(offers);

                resources.add(resourceBuilder.build());
            } catch (URISyntaxException e) {
                logger.warn("Failed to create a dummy resource.", e);
                e.printStackTrace();
            }
        }


        return resources;
    }

    /**
     * sba:
     *
     * @param uri
     * @return
     */
    private Contract createDefaultContract(@NotNull @NotNull URI uri) {

        @NotNull URI nonCommercialLicense = null;
        URI consumer = null;

        try {
            nonCommercialLicense = new URI("https://creativecommons.org/licenses/by-nc/4.0/legalcode");
            consumer = new URI("http://example.org/you");
        } catch (URISyntaxException e) {
            logger.warn("Exception in DynamicConnectorSelfDescription", e);
        }

        ArrayList<Action> allowedActions = new ArrayList<Action>() {{
            add(Action.USE);
        }};
        ArrayList<Action> obligingActions = new ArrayList<Action>() {{
            add(Action.DELETE);
        }};

        XMLGregorianCalendar contractEndsInFiveMinutes = CalendarUtil.now();
        try {
            contractEndsInFiveMinutes.add(DatatypeFactory.newInstance().newDuration(true, 0, 0, 0, 0, 5, 0));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }


        // permission only valid for five minutes
        ArrayList<Constraint> permissionConstraints = new ArrayList<Constraint>() {{
            add(
                    new ConstraintBuilder()
                            ._leftOperand_(LeftOperand.POLICY_EVALUATION_TIME)
                            ._operator_(BinaryOperator.BEFORE)
                            ._rightOperand_(new RdfResource("\"" + contractEndsInFiveMinutes.toString() + "\"^^xsd:dateTime"))
                            .build()
            );
        }};

        // after five minutes, the obligation has to be executed
        ArrayList<Constraint> dutyConstraints = new ArrayList<Constraint>() {{
            add(
                    new ConstraintBuilder()
                            ._leftOperand_(LeftOperand.POLICY_EVALUATION_TIME)
                            ._operator_(BinaryOperator.EQUALS)
                            ._rightOperand_(new RdfResource("\"" + contractEndsInFiveMinutes.toString() + "\"^^xsd:dateTime"))
                            .build()
            );
        }};

        // the final contract offer
        URI conURI = null;
        try {
            conURI = new URI("default.contract");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        ContractOffer offer = new ContractOfferBuilder(conURI)
                ._contractDocument_(new TextResourceBuilder(nonCommercialLicense).build())
                ._provider_(maintainerId)
                ._consumer_(consumer)
                ._contractStart_(CalendarUtil.now())
                ._contractEnd_(contractEndsInFiveMinutes)
                ._permission_(new ArrayList<Permission>() {{
                    add(new PermissionBuilder()
                            ._constraint_(permissionConstraints)
                            ._action_(allowedActions).build());
                }})
                .build();

        return offer;
    }

    private Representation createRepresentation(Artifact artifact) {
        return new RepresentationBuilder()
                ._instance_(Util.asList(artifact))
                .build();
    }

    public synchronized void setArtifacts(ArtifactIndex artifacts) {
        this.artifacts = artifacts;
    }

}
