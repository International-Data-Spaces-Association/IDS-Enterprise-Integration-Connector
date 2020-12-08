package de.fraunhofer.iais.eis.ids.mdmconnector.components;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import de.fraunhofer.iais.eis.ids.mdmconnector.artifact.ArtifactIndex;
import org.apache.jena.sparql.pfunction.library.ListBase1;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Objects for comparing and Editing Contracts
 * @author dennis.oliver.kubitza@iais.fraunhofer.de
 * @since 2020-11-11
 * @version 2020-11-11
 */
public class ContractUtil {
    private ArtifactIndex artifactIndex;
    private InfrastructureComponent infrastructureComponent;

    /**
     * Constructor
     * @param artifactIndex The Artifact index to be used f.e. InMemoryArtifactIndex
     * @param infrastructureComponent The infrastructureComponent that is used for the description
     */
    public ContractUtil(ArtifactIndex artifactIndex, InfrastructureComponent infrastructureComponent){
        this.artifactIndex=artifactIndex;
        this.infrastructureComponent = infrastructureComponent;
    }

    /**
     * Compare Incoming ContractRequest with ContractOffer from Artifact Listener
     * @param requestedContract the Rquestested Contract
     * @param consumer the Consumers URI used for the Replacement
     */
    public boolean compareWithIndex(Contract requestedContract, URI consumer) throws IOException {

      URI targetUri = requestedContract.getPermission().get(0).getTarget();
      Artifact targetArtifact = artifactIndex.getArtifactAsObject(targetUri).get();

      //Preprocesing, make offer and Request more similar
      ContractOffer contractOffer = (ContractOffer) artifactIndex.getContract(targetArtifact);
      String offer = replaceStrategy(contractOffer.toRdf(), consumer);
      ContractOffer processedContractOffer = new Serializer().deserialize(offer, ContractOfferImpl.class);
      String request = requestedContract.toRdf().replace("Request", "Offer");
      request = request.replace("Agreement", "Offer");

        ContractOffer processedContractRequest = new Serializer().deserialize(request, ContractOfferImpl.class);

      List<String> differences = getTextBasedDifferences(cleanUp(processedContractOffer.toRdf()), cleanUp(processedContractRequest.toRdf()) );
      if (differences.isEmpty()){
        return true;
      }

      boolean result = true;
      //Blacklisting not allowed differences in idsc: (Changed IDs) and values(except for Date)
        for(int i = 0; i < differences.size(); ++i) {
            if ((differences.get(i).matches("(.*)idsc:(.*)"))){
                return false;
            }
            if ((differences.get(i).matches("(.*)@value(.*)"))&&!(differences.get(i).matches("(.*)-(.*)-(.*)T(.*)"))){
                return false;
            }
        }
      return result;
    }

    public boolean compareWithIndexNeg(Contract requestedContract, URI consumer) throws IOException {

        URI targetUri = requestedContract.getPermission().get(0).getTarget();
        Artifact targetArtifact = artifactIndex.getArtifactAsObject(targetUri).get();

        //Preprocesing, make offer and Request more similar
        ContractOffer contractOffer = (ContractOffer) artifactIndex.getContract(targetArtifact);
        String offer = replaceStrategy(contractOffer.toRdf(), consumer);
        ContractOffer processedContractOffer = new Serializer().deserialize(offer, ContractOfferImpl.class);
        String request = requestedContract.toRdf().replace("Request", "Offer");
        request = request.replace("Agreement", "Offer");

        ContractOffer processedContractRequest = new Serializer().deserialize(request, ContractOfferImpl.class);

        List<String> differences = getTextBasedDifferences(cleanUp(processedContractOffer.toRdf()), cleanUp(processedContractRequest.toRdf()) );
        if (differences.isEmpty()){
            return true;
        }


        boolean result = true;
        //Blacklisting not allowed differences in idsc: (Changed IDs) and values(except for Date, Duration)
        for(int i = 0; i < differences.size(); ++i) {
            if ((differences.get(i).matches("(.*)idsc:(.*)"))){
                return false;
            }
            if ((differences.get(i).matches("(.*)@value(.*)"))&&!
                    (differences.get(i).matches("(.*)-(.*)-(.*)T(.*)")
                            ||differences.get(i).matches("(.*)P(.*)D(.*)")
                            ||differences.get(i).matches("(.*)https://www.wikidata.org/wiki/Q4916(.*)") )){
                return false;
            }
        }


        return result;
    }

    public String cleanUp(String Input)
    {
        return Input.replace("{", "")
                .replace("[", "").replace("]", "")
                .replace("}", "").replace(",", "")
                .replace(";", "").replace(" ", "");
    }

    public List<String> getTextBasedDifferences(String Input1, String Input2){
        String lines1[] = Input1.split("\\r?\\n");
        String lines2[] = Input2.split("\\r?\\n");
        List<String> sortedList1 = Arrays.asList(lines1).stream().sorted().collect(Collectors.toList());
        List<String> sortedList2 = Arrays.asList(lines2).stream().sorted().collect(Collectors.toList());
        List<String> savingList1 = new ArrayList<>(sortedList1);
        sortedList1.removeAll(sortedList2);
        sortedList2.removeAll(savingList1);
        sortedList1.addAll(sortedList2);
         sortedList1.addAll(sortedList2);
        return (sortedList1);
    }

    public ContractAgreement createNewContractAgreement(Artifact targetArtifact, URI consumer) throws IOException {
        ContractOffer contractOffer = (ContractOffer) artifactIndex.getContract(targetArtifact);
        String offer = replaceStrategy(contractOffer.toRdf(), consumer).replace("Offer", "Agreement");
        ContractAgreement finalContractAgreement= new Serializer().deserialize(offer, ContractAgreementImpl.class);
        System.out.print(finalContractAgreement.toRdf());
        return finalContractAgreement;
    }

    public ContractOffer createNewContractOffer(Artifact targetArtifact, URI consumer) throws IOException {
        ContractOffer contractOffer = (ContractOffer) artifactIndex.getContract(targetArtifact);
        String offer = replaceStrategy(contractOffer.toRdf(), consumer);
        ContractOffer finalContractOffer = new Serializer().deserialize(offer, ContractOfferImpl.class);
        return finalContractOffer;
    }

    public String replaceStrategy(String input,URI consumer){
        String offer = input.replace("www.example.org/YourURI", consumer.toString());
        offer = offer.replace("autogen", consumer.toString());
        offer = offer.replace("http://exampleProvider.example.org", infrastructureComponent.getMaintainer().toString());
        return offer;
    }

}
