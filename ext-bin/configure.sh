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

changePermission
setupWardenConfFile

rm -rf ${NIFI_HOME}/conf/.not_configured_yet