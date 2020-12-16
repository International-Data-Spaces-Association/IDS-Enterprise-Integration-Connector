package de.fraunhofer.iais.eis.ids.connector.main;

import java.util.Collection;

public class DapsInteractionConfig {

    String dapsUrl = "", keyStoreFile = "", keyStorePwd = "", keyStoreAlias = "", UUID = "";
    Collection<String> trustedHosts;
    boolean verify = true;

    public DapsInteractionConfig(String dapsUrl, String keyStoreFile, String keyStorePwd, String keyStoreAlias, String UUID,Collection<String> trustedHosts, boolean verify) {
        this.dapsUrl = dapsUrl;
        this.keyStoreFile = keyStoreFile;
        this.keyStorePwd = keyStorePwd;
        this.keyStoreAlias = keyStoreAlias;
        this.UUID = UUID;
        this.verify = verify;
        this.trustedHosts = trustedHosts;

    }
}
