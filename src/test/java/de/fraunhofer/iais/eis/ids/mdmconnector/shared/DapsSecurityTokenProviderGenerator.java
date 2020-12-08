package de.fraunhofer.iais.eis.ids.mdmconnector.shared;

import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;


 public class DapsSecurityTokenProviderGenerator {


    static public DapsSecurityTokenProvider generate(String dapsUrl, String keyStoreFile, String keyStorePwd, String keyStoreAlias, String dapsUUID)
    {	DapsSecurityTokenProvider daps = null;
        try {
            //InputStream keystore = getClass().getClassLoader().getResourceAsStream(dapsInteractionConfig.keyStoreFile);
            InputStream keystore = Files.newInputStream((new File("")).toPath().resolve(keyStoreFile));
            String dapsUrl2 = dapsUrl + "/token";
            daps = new DapsSecurityTokenProvider(keystore,
                    keyStorePwd,
                    keyStoreAlias,
                    dapsUUID,
                    dapsUrl2,
                    true,
                    true);
            String securityToken = daps.getSecurityToken();
        } catch (InvalidPathException | IOException | TokenRetrievalException e) {
            e.printStackTrace();
        }
        return daps;
    }
}
