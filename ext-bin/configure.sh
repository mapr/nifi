#!/usr/bin/env bash

. "$(dirname "${BASH_SOURCE[0]}")/_configure_variables.sh"

function setupWardenConfFile() {
  if ! [ -d ${MAPR_WARDEN_CONF_DIR} ]; then
    mkdir -p ${MAPR_WARDEN_CONF_DIR} >/dev/null 2>&1
  fi

  cp $WARDEN_CONF $MAPR_WARDEN_CONF_DIR
}

function changePermission() {
  chown -R ${MAPR_USER}:${MAPR_GROUP} ${NIFI_HOME}
  sed -i "s~run.as=.*~run.as=$MAPR_USER~" $BOOTSTRAP_CONF
}

function configureUiSecurity() {
  hostName=`hostname -f`
  sed -i "s~0.0.0.0*~$hostName~" $NIFI_CONF
  if [ "$IS_SECURED" == "true" ]; then
    sed -i "s~nifi.remote.input.secure=.*~nifi.remote.input.secure=true~" $NIFI_CONF
    sed -i "s~nifi.web.http.host=.*~nifi.web.http.host=~" $NIFI_CONF
    sed -i "s~nifi.web.http.port=.*~nifi.web.http.port=~" $NIFI_CONF
  else
    sed -i "s~nifi.remote.input.secure=.*~nifi.remote.input.secure=false~" $NIFI_CONF
    sed -i "s~nifi.web.https.host=.*~nifi.web.https.host=~" $NIFI_CONF
    sed -i "s~nifi.web.https.port=.*~nifi.web.https.port=~" $NIFI_CONF
  fi
}

function enableFipsIfConfigured() {
  get_fips_mode=$(sysctl crypto.fips_enabled 2>/dev/null)
  fips_enabled='crypto.fips_enabled = 1'
  if [ "$get_fips_mode" == "$fips_enabled" ]; then
    # FIPS-mode implies -secure
    if [ "$IS_SECURED" == "true" ]; then
      sed -i "s~#java.arg.19=.*~java.arg.19=-Djava.security.properties=$FIPS_CONF~" $BOOTSTRAP_CONF
    fi
  fi
}

changePermission
configureUiSecurity
setupWardenConfFile
enableFipsIfConfigured

rm -rf ${NIFI_HOME}/conf/.not_configured_yet
