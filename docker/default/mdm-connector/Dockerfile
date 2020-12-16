#FROM frolvlad/alpine-oraclejdk8:slim
FROM adoptopenjdk/openjdk11:jdk11u-alpine-nightly

RUN java --version
ADD mdm-connector-*.jar mdm-connector.jar
ADD run.sh run.sh

RUN mkdir -p /var/ids/artifacts

# Add certificates and truststore
RUN mkdir -p /resources
ADD iais3-keystore.jks /resources/iais3-keystore.jks
ADD cacerts /resources/cacerts
ADD cacerts $JAVA_HOME/lib/security
#ADD daps.crt /resources/daps.crt
#RUN keytool -keystore /resources/cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias aisec_daps -file /resources/daps.crt
#RUN keytool -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias aisec_daps -file $JAVA_HOME/lib/security/daps.crt


# Add demo artifacts and contracts
#ADD resources/* /var/ids/artifacts/
#ADD resources/bab_bayern_sample.xml /var/ids/artifacts/bab_bayern_sample.xml
#ADD resources/bab_bayern_sample-contract.jsonld /var/ids/artifacts/bab_bayern_sample-contract.jsonld
#ADD resources/bab_sachsen_sample.xml /var/ids/artifacts/bab_sachsen_sample.xml
#ADD resources/bab_sachsen_sample-contract.jsonld /var/ids/artifacts/bab_sachsen_sample-contract.jsonld
#ADD resources/bab_sax_anhalt_sample.xml /var/ids/artifacts/bab_sax_anhalt_sample.xml
#ADD resources/bab_sax_anhalt_sample-contract.jsonld /var/ids/artifacts/bab_sax_anhalt_sample-contract.jsonld
#ADD resources/bab_thuringa_sample.xml /var/ids/artifacts/bab_thuringa_sample.xml
#ADD resources/bab_thuringa_sample-contract.jsonld /var/ids/artifacts/bab_thuringa_sample-contract.jsonld

#ADD resources/boost-wp3-demo5-speedlimit20.xml /var/ids/artifacts/boost-wp3-demo5-speedlimit20.xml
#ADD resources/boost-wp3-demo5-speedlimit25.xml /var/ids/artifacts/boost-wp3-demo5-speedlimit25.xml
#ADD resources/boost-wp3-demo5-speedlimit30.xml /var/ids/artifacts/boost-wp3-demo5-speedlimit30.xml

#ADD resources/bab_thuringa_sample.xml /var/ids/artifacts/sol1_mr_v2.ttl
#ADD resources/bab_thuringa_sample-contract.jsonld /var/ids/artifacts/sol1_mr_v2-contract.jsonld

#ADD resources/Raspberry_Pi_3b_plus-contract.jsonld /var/ids/artifacts/Raspberry_Pi_3b_plus-contract.jsonld
#ADD resources/Raspberry_Pi_3b_plus-desc.jsonld /var/ids/artifacts/Raspberry_Pi_3b_plus-desc.jsonld
#ADD resources/Raspberry_Pi_3b_plus.aasx /var/ids/artifacts/Raspberry_Pi_3b_plus.aasx

RUN sh -c 'touch /mdm-connector.jar'
RUN sh -c 'chmod +x /run.sh'

ENTRYPOINT ["/run.sh"]
