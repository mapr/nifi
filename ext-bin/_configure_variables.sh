#!/usr/bin/env bash

MAPR_HOME=${MAPR_HOME:=/opt/mapr}
NIFI_VERSION="1.16.0"
NIFI_HOME="$MAPR_HOME/nifi/nifi-$NIFI_VERSION"
NIFI_LIBS=${NIFI_HOME}"/lib/"
NIFI_NOT_USED_LIBS=${NIFI_HOME}"/not-used-libs/"
MAPR_WARDEN_CONF_DIR="${MAPR_HOME}/conf/conf.d"
MAPR_WARDEN_CONF="${MAPR_WARDEN_CONF_DIR}/warden.nifi.conf"
WARDEN_CONF="$NIFI_HOME/conf/warden.nifi.conf"
DAEMON_CONF="${MAPR_HOME}/conf/daemon.conf"
MAPR_USER=${MAPR_USER:-$( [ -f "$DAEMON_CONF" ] && awk -F = '$1 == "mapr.daemon.group" { print $2 }' "$DAEMON_CONF" )}
MAPR_USER=${MAPR_USER:-"mapr"}
BOOTSTRAP_CONF="$NIFI_HOME/conf/bootstrap.conf"
NIFI_CONF="$NIFI_HOME/conf/nifi.properties"
FIPS_CONF="${MAPR_HOME}/conf/java.security.fips"
IS_SECURED=`cat ${MAPR_HOME}/conf/mapr-clusters.conf | sed 's/.*\(secure=\)\(true\|false\).*/\2/'`
PID_FILE="${MAPR_HOME}/pid/nifi.pid"
STATUS_FILE="${MAPR_HOME}/pid/nifi.status"