package de.fraunhofer.iais.eis.ids.connector.main;

class LocalConnectorConfig {

    String componentUrl = "", componentModelVersion = "", componentMaintainer = "";

    LocalConnectorConfig(String componentUrl, String componentModelVersion, String componentMaintainer) {
        this.componentUrl = componentUrl;
        this.componentModelVersion = componentModelVersion;
        this.componentMaintainer = componentMaintainer;
    }
}
