#!/usr/bin/env bash

MAPR_HOME=${MAPR_HOME:=/opt/mapr}
NIFI_VERSION="1.16.0"
NIFI_HOME="$MAPR_HOME/nifi/nifi-$NIFI_VERSION"
NIFI_LIBS=${NIFI_HOME}"/lib/"
NIFI_NOT_USED_LIBS=${NIFI_HOME}"/not-used-libs/"
MAPR_WARDEN_CONF_DIR="${MAPR_HOME}/conf/conf.d"
WARDEN_CONF="$NIFI_HOME/conf/warden.nifi.conf"
MAPR_USER=`logname`
MAPR_GROUP="$MAPR_USER"
BOOTSTRAP_CONF="$NIFI_HOME/conf/bootstrap.conf"
NIFI_CONF="$NIFI_HOME/conf/nifi.properties"
FIPS_CONF="${MAPR_HOME}/conf/java.security.fips"
IS_SECURED=`cat ${MAPR_HOME}/conf/mapr-clusters.conf | sed 's/.*\(secure=\)\(true\|false\).*/\2/'`