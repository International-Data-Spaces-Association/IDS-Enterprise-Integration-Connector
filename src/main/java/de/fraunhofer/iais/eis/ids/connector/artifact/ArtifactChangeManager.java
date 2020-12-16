package de.fraunhofer.iais.eis.ids.connector.artifact;


import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.client.broker.BrokerException;
import de.fraunhofer.iais.eis.ids.component.client.broker.RemoteBroker;
import de.fraunhofer.iais.eis.ids.component.core.InfomodelFormalException;
import de.fraunhofer.iais.eis.ids.component.core.SecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.ids.connector.infrastructure.DynamicArtifactSelfDescriptionProvider;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.*;

public class ArtifactChangeManager implements ArtifactListener {

    final private Logger logger = LoggerFactory.getLogger(ArtifactChangeManager.class);

    private ArtifactIndex artifactIndex;
    private ArtifactFactory artifactFactory;
    private DynamicArtifactSelfDescriptionProvider selfDescription;
    private RemoteBroker remoteBroker;
    private boolean brokerIgnore;
    private Map<String, Contract> contractStack;
    private Map<String, Resource> descriptionStack;
    private Map<String, JSONObject> negotiationStack;
    private SecurityTokenProvider securityTokenProvider;

    public ArtifactChangeManager(
            RemoteBroker remoteBroker,
            ArtifactIndex artifactIndex,
            DynamicArtifactSelfDescriptionProvider selfDescription,
            SecurityTokenProvider securityTokenProvider,
            boolean brokerIgnore
    ) throws InfomodelFormalException, URISyntaxException {
        this.artifactIndex = artifactIndex;
        this.selfDescription = selfDescription;
        this.remoteBroker = remoteBroker;
        this.securityTokenProvider = securityTokenProvider;
        artifactFactory = new ArtifactFactory(selfDescription.getSelfDescription().getId());
        this.contractStack = new HashMap<>();
        this.descriptionStack = new HashMap<>();
        this.negotiationStack = new HashMap<>();
        this.brokerIgnore = brokerIgnore;
    }

    @Override
    public void notifyAdd(File file) throws InfomodelFormalException, IOException {

     //   if(file.getName().equals("PING"))
     //   {
     //       try {DemoClient.main(null);}
     //       catch (Exception e){
     //          logger.info("Could not start with PING");
     //          e.printStackTrace();
     //       };
     //   }

     if (file.getName().endsWith("-contract.jsonld")) {
            // contract added in observed directory


            try {
                Serializer serializer = new Serializer();
                Contract contract = serializer.deserialize(FileUtils.readFileToString(file, Charset.defaultCharset()), ContractOffer.class);
                Artifact artifact = null;
                artifact = findRelatedArtifact(file.getName());

                if (artifact != null) {

                    contract = addArtifactIdToContract(contract, artifact);
                    artifactIndex.addContract(artifact, contract);
                    selfDescription.setArtifacts(artifactIndex);
                    updateBroker();

                } else {

                    // respective artifact file does NOT exist
                    logger.info("New contract '" + file.getName() + "' available but no respective resource found. Self-description NOT updated.");
                    contractStack.put(file.getName().split("-contract")[0], contract);
                }


            } catch (IOException e) {
                logger.warn("Could not parse contract " + file.getName() + " . Self-description NOT updated.", e);
            }


        } else if (file.getName().endsWith("-desc.jsonld")) {
         // description added in observed directory
         try {
             Serializer serializer = new Serializer();
             Resource description = serializer.deserialize(FileUtils.readFileToString(file, Charset.defaultCharset()), Resource.class);
             Artifact artifact = findRelatedArtifact(file.getName());


             if (artifact != null) {
                 description = addArtifactIdToDescription((ResourceImpl) description, artifact);
                 artifactIndex.addDescription(artifact, description);
                 descriptionStack.put(file.getName().split("-desc")[0], description);
                 logger.info("New description '" + file.getName() + "' available.");
                 updateBroker();
             } else {
                 // respective artifact file does NOT exist
                 logger.info("New description '" + file.getName() + "' available but no respective resource found. Self-description NOT updated.");
                 descriptionStack.put(file.getName().split("-desc")[0], description);
             }
         } catch (IOException e) {
             logger.warn("Could not parse description " + file.getName() + " . Self-description NOT updated.", e);
         }
     }
     else if (file.getName().endsWith("-negotiation.jsonld")){
         try {
             JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
             Object obj = parser.parse(new FileReader(file));
             JSONObject negotiation = (JSONObject) obj;
             Artifact artifact = findRelatedArtifact(file.getName());

             if (artifact != null) {
                 artifactIndex.addNegotiation(artifact, negotiation);
                 logger.info("New Negotiaion '" + file.getName() + "' added for file");

             } else {
                 negotiationStack.put(file.getName().split("-negotiation")[0], negotiation);
                 // respective artifact file does NOT exist
                 logger.info("New Negotiaion '" + file.getName() + "' available but no respective resource found.");
             }
         } catch (IOException | ParseException e) {
             logger.warn("Could not parse negotiation " + file.getName() + "", e);
         }
     }
     else {
            // artifact added in observed directory
            File artifactFile = file;

            if (!artifactIndex.exists(artifactFile)) {
                Artifact artifact = artifactFactory.createArtifact(artifactFile);
                artifactIndex.addArtifact(artifactFile, artifact);


                if (artifactFile.getName().contains(".")) {
                    if (contractStack.containsKey(artifactFile.getName().split("\\.")[0])) {
                        Contract contract = addArtifactIdToContract(contractStack.get(artifactFile.getName().split("\\.")[0]), artifact);
                        artifactIndex.addContract(artifact, contract);
                    }
                    if (descriptionStack.containsKey(artifactFile.getName().split("\\.")[0])) {
                        addArtifactIdToDescription((ResourceImpl) descriptionStack.get(artifactFile.getName().split("\\.")[0]), artifact);
                        artifactIndex.addDescription(artifact, descriptionStack.get(artifactFile.getName().split("\\.")[0]));
                    }
                    if (negotiationStack.containsKey(artifactFile.getName().split("\\.")[0])) {
                        JSONObject negotiation = negotiationStack.get(artifactFile.getName().split("\\.")[0]);
                        artifactIndex.addNegotiation(artifact, negotiation);
                    }
                } else {
                    if (contractStack.containsKey(artifactFile.getName())) {
                        Contract contract = addArtifactIdToContract(contractStack.get(artifactFile.getName()), artifact);
                        artifactIndex.addContract(artifact, contract);
                    }
                    if (descriptionStack.containsKey(artifactFile.getName())) {
                        addArtifactIdToDescription((ResourceImpl) descriptionStack.get(artifactFile.getName()), artifact);
                        artifactIndex.addDescription(artifact, descriptionStack.get(artifactFile.getName()));
                    }
                }
                if (negotiationStack.containsKey(artifactFile.getName())) {
                    JSONObject negotiation = negotiationStack.get(artifactFile.getName().split("\\.")[0]);
                    artifactIndex.addNegotiation(artifact, negotiation);
                }

                selfDescription.setArtifacts(artifactIndex);
                updateBroker();

                logger.info("New artifact '" + artifactFile.getName() + "' available (URL: '" + artifact.getId() + "'). Self-description updated.");
            }
        }
    }

    /**
     * @param desc the description file, must always be *.ttl or *.jsonld
     * @return
     */
    private boolean validateDescriptionSyntax(File desc) {
        /*RDFFormat rdfFormat = desc.getName().endsWith("ttl") ? RDFFormat.TURTLE : RDFFormat.JSONLD;
        String rioBaseURI = rdfFormat == RDFFormat.JSONLD ? null : "";
        try {
            Rio.parse(new FileInputStream(desc), rioBaseURI, rdfFormat);
        } catch (IOException e) {
            logger.error("error when reading from description file: " + desc.getPath());
            e.printStackTrace();
            return false;
        } catch (RDFParseException e) {
            logger.error("bad rdf syntax in description file: " + desc.getPath());
            e.printStackTrace();
            return false;
        } */
        return true;
    }


    /**
     * checks whether an accordingly named file (Artifact) is present. Returns null if not.
     *
     * @param contractOrDescFileName
     * @return
     */
    private Artifact findRelatedArtifact(String contractOrDescFileName) {

        // check for matching artifact
        Iterator<Artifact> iter = artifactIndex.getAllArtifacts().iterator();

        while (iter.hasNext()) {
            Artifact artifact = iter.next();
            try {

                if (contractOrDescFileName.split("-contract")[0].equals(artifact.getFileName().split("\\.")[0]) ||
                        contractOrDescFileName.split("-desc")[0].equals(artifact.getFileName().split("\\.")[0]) ||
                        contractOrDescFileName.split("-negotiation")[0].equals(artifact.getFileName().split("\\.")[0])) {
                    // respective artifact file exists
                    return artifact;
                }

            } catch (Exception e) {
                // continue
            }

        }

        return null;
    }


    /**
     * adds or overwrites the given representation in order to put the proper artifact URI in
     *
     * @param description
     * @param artifact
     * @return
     */
    private Resource addArtifactIdToDescription(ResourceImpl description, Artifact artifact) {
        try {


            Representation representation = new RepresentationBuilder()._instance_(new ArrayList<RepresentationInstance>() {{
                add(artifact);
            }}).build();

            if (description.getDefaultRepresentation() != null) {


                //ArrayList<Representation> orig_representations = new ArrayList<Representation>(description.getDefaultRepresentation());
                ArrayList<Representation> orig_representations = new ArrayList<Representation>();
                orig_representations.add(representation);
                description.setDefaultRepresentation(orig_representations);


            } else if (description.getRepresentation() != null) {


                ArrayList<Representation> orig_representations = new ArrayList<Representation>(description.getRepresentation());
                orig_representations.add(representation);
                description.setRepresentation(orig_representations);


            } else {


                ArrayList<Representation> orig_representations = new ArrayList<Representation>();
                orig_representations.add(representation);
                description.setRepresentation(orig_representations);

            }


        } catch (NoSuchElementException e) {
            logger.info("Tried to add a file to the description but coudln't find its artifact.", e);
        }
        return description;
    }


    /**
     * adds or overwrites the given representation in order to put the proper artifact URI in
     *
     * @param description
     * @param file
     * @return
     */
    private Contract addArtifactIdToContract(Contract contract, Artifact artifact) {
        try {


            ArrayList<Duty> obligations = null;
          /*  if (contract.get != null) {

                obligations = new ArrayList<Duty>(contract.getObligation());
                Iterator<Duty> iter = obligations.iterator();
                while (iter.hasNext()) {
                    DutyImpl duty = (DutyImpl) iter.next();
                    duty.setTargetArtifact(artifact);
                }
            }
          */

            ArrayList<Permission> permissions = null;
            if (contract.getPermission() != null) {

                permissions = new ArrayList<Permission>(contract.getPermission());
                Iterator<Permission> iter_perm = permissions.iterator();
                while (iter_perm.hasNext()) {
                    PermissionImpl permission = (PermissionImpl) iter_perm.next();
                    permission.setTarget(artifact.getId());
                }

            }

            ArrayList<Prohibition> prohibitions = null;
            if (contract.getProhibition() != null) {

                prohibitions = new ArrayList<Prohibition>(contract.getProhibition());
                Iterator<Prohibition> iter_proh = prohibitions.iterator();
                while (iter_proh.hasNext()) {
                    ProhibitionImpl prohibition = (ProhibitionImpl) iter_proh.next();
                    prohibition.setTarget(artifact.getId());
                }

            }


            if (contract instanceof ContractOfferImpl) {
               // if (obligations != null) ((ContractOfferImpl) contract).setObligation(obligations);
                if (permissions != null) ((ContractOfferImpl) contract).setPermission(permissions);
                if (prohibitions != null) ((ContractOfferImpl) contract).setProhibition(prohibitions);

            } else if (contract instanceof ContractRequestImpl) {

              //  if (obligations != null) ((ContractRequestImpl) contract).setObligation(obligations);
                if (permissions != null) ((ContractRequestImpl) contract).setPermission(permissions);
                if (prohibitions != null) ((ContractRequestImpl) contract).setProhibition(prohibitions);

            } else if (contract instanceof ContractAgreementImpl) {

               // if (obligations != null) ((ContractAgreementImpl) contract).setObligation(obligations);
                if (permissions != null) ((ContractAgreementImpl) contract).setPermission(permissions);
                if (prohibitions != null) ((ContractAgreementImpl) contract).setProhibition(prohibitions);

            }


        } catch (NoSuchElementException e) {
            logger.info("Tried to add a file to the description but coudln't find its artifact.", e);
        }
        return contract;
    }


    @Override
    public void notifyRemove(File file) throws InfomodelFormalException, IOException {

        if (file.getName().endsWith("-contract.jsonld")) {

            Artifact artifact = findRelatedArtifact(file.getName());

            if (artifact != null) {

                if (artifactIndex.hasContract(artifact)) {

                    artifactIndex.removeContract(artifact);
                    selfDescription.setArtifacts(artifactIndex);
                    updateBroker();

                    logger.info("Recognized the deletion of contract '" + file.getName() + "' for artifact '" + artifact.getId() + "'. Default contract initialized. Self-description updated.");
                }

            } else {

                logger.info("Could not find an according artifact for contract '" + file.getName() + "' . Self-description NOT updated.");

            }
        } else if (file.getName().endsWith("-desc.jsonld")) {
            Artifact artifact = findRelatedArtifact(file.getName());

            if (artifact != null) {
                if (artifactIndex.hasDescription(artifact)) {
                    artifactIndex.removeDescription(artifact);
                    selfDescription.setArtifacts(artifactIndex);
                    updateBroker();
                    logger.info("Recognized the deletion of description '" + file.getName() + "' for artifact '" + artifact.getId() + "'. Default description initialized. Self-description updated.");
                }
            } else {
                logger.info("Could not find an according artifact for description '" + file.getName() + "' . Self-description NOT updated.");
            }
        }
        else if (file.getName().endsWith("-negotiation.jsonld")) {
                Artifact artifact = findRelatedArtifact(file.getName());
                if (artifact != null) {
                    if (artifactIndex.hasNegotiation(artifact)) {
                        artifactIndex.removeNegotiation(artifact);
                        logger.info("Recognized the deletion of negotiation '" + file.getName() + "' for artifact '" + artifact.getId() + "'. Default description initialized. Self-description updated.");
                    }
                } else {
                    logger.info("Could not find an according artifact for negotiation '" + file.getName() + "' .Negotiation NOT updated.");
                }
        } else {

            Artifact artifact = findRelatedArtifact(file.getName());
            if (artifact != null) artifactIndex.removeContract(artifact);
            if (artifact != null) artifactIndex.removeNegotiation(artifact);
            if (artifact != null) artifactIndex.removeDescription(artifact);

            artifactIndex.removeArtifact(file);
            selfDescription.setArtifacts(artifactIndex);
            updateBroker();
            logger.info("Artifact '" + file.getName() + "' has been removed. Self-description updated.");
        }
    }



    private void updateBroker() throws InfomodelFormalException, IOException {
        {
            if (!brokerIgnore) {
                try {

                   remoteBroker.update(selfDescription.getSelfDescription(), securityTokenProvider.getSecurityTokenAsDAT());

                } catch (BrokerException e) {
                    try {
                        logger.warn("Unable to update broker: " + ((RejectionMessage) e.getResponse().getMessage()).getRejectionReason());

                        e.printStackTrace();
                    } catch (Exception e2) {
                        logger.warn("Unable to update broker.", e);
                    }
                } catch (IOException | TokenRetrievalException e) {
                    logger.warn("Unable to update broker.", e);
                    e.printStackTrace();
                }
            }
        }
    }
}
