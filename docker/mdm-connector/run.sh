#!/bin/sh

echo "Starting Spring boot app"

ARGS=" -Djavax.net.ssl.trustStore=/resources/cacerts -Dbroker.url=${BROKER_URL} -Ddaps.url=${DAPS_URL}"
#ARGS="-Djava.security.egd=file:/dev/./urandom -Djavax.net.ssl.trustStore=/resources/cacerts -Dbroker.url=https://broker.ids.isst.fraunhofer.de -Ddaps.url=https://daps.ids.isst.fraunhofer.de"

# Add proxy args
if [ ! -z "$PROXY_HOST" ]; then
    ARGS="${ARGS} -Dhttp.proxyHost=${PROXY_HOST} -Dhttp.proxyPort=${PROXY_PORT-3128}"
    if [ ! -z "$PROXY_USER" ]; then
        ARGS="${ARGS} -Dhttp.proxyUser=${PROXY_USER}"
    fi
    if [ ! -z "$PROXY_PASS" ]; then
        ARGS="${ARGS} -Dhttp.proxyPassword=${PROXY_PASS}"
    fi
fi

# Enable debugging
ARGS="${ARGS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

echo "ARGS=${ARGS}"

echo "debug point 1"

exec java ${ARGS} -jar /mdm-connector.jar
