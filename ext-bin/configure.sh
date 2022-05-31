#!/usr/bin/env bash

. "$(dirname "${BASH_SOURCE[0]}")/_configure_variables.sh"

function setupWardenConfFile() {
    if ! [ -d ${MAPR_WARDEN_CONF_DIR} ]; then
      mkdir -p ${MAPR_WARDEN_CONF_DIR} > /dev/null 2>&1
    fi

    cp $WARDEN_CONF $MAPR_WARDEN_CONF_DIR
}

function changePermission() {
    chown -R ${MAPR_USER}:${MAPR_GROUP} ${NIFI_HOME}
    sed -i "s~run.as=.*~run.as=$MAPR_USER~" $BOOTSTRAP_CONF
}

function enableFipsIfConfigured() {
    get_fips_mode=$(sysctl crypto.fips_enabled 2> /dev/null)
    fips_enabled='crypto.fips_enabled = 1'
     if [ "$get_fips_mode" == "$fips_enabled" ]; then
           # FIPS-mode implies -secure
           if [ "$IS_SECURED" == "true" ]; then
                 sed -i "s~#java.arg.19=.*~java.arg.19=-Djava.security.properties=$FIPS_CONF~" $BOOTSTRAP_CONF
           fi
     fi
}

changePermission
setupWardenConfFile
enableFipsIfConfigured

rm -rf ${NIFI_HOME}/conf/.not_configured_yet