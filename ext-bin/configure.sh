#!/usr/bin/env bash

. "$(dirname "${BASH_SOURCE[0]}")/_configure_variables.sh"

function setupWardenConfFile() {
  if ! [ -d ${MAPR_WARDEN_CONF_DIR} ]; then
    mkdir -p ${MAPR_WARDEN_CONF_DIR} >/dev/null 2>&1
  fi

  cp $WARDEN_CONF $MAPR_WARDEN_CONF_DIR
}

function updateWardenLocalConfFile() {
  #update warden service.ui.port
  if [ "$IS_SECURED" == "true" ]; then
  	port=$(cat $NIFI_CONF | grep 'nifi.web.https.port=' | sed 's/\(nifi.web.https.port=\)//')
  else
  	port=$(cat $NIFI_CONF | grep 'nifi.web.http.port=' | sed 's/\(nifi.web.http.port=\)//')
  fi
  sed -i "s~service.ui.port=.*~service.ui.port=$port~" $WARDEN_CONF

  #update warden service.port
  #collect ports from nifi.properties
  properties="
    nifi.remote.input.socket.port
    nifi.cluster.node.protocol.port
    nifi.cluster.load.balance.port
    nifi.web.http.port
    nifi.web.https.port
    nifi.cluster.node.protocol.port"

    service_ports=""
    for property in $properties
      do
        port=$(cat $NIFI_CONF | grep "$property=" | sed 's/\('$property'=\)//')
        if [[ $port != "" ]];
        then
           service_ports+="$port,"
        fi
    done
    #collect port from bootstrap.conf
    bootStrapPort=$(cat $BOOTSTRAP_CONF | grep 'nifi.bootstrap.listen.port=' | sed 's/\(nifi.bootstrap.listen.port=\)//')
    service_ports+="$bootStrapPort"
    sed -i "s~service.port=.*~service.port=$service_ports~" $WARDEN_CONF
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

function migratePreviousConfiguration() {
  if [ -f ${NIFI_HOME}"/conf/.not_configured_yet" ]; then
    nifi_folders="$MAPR_HOME/nifi/*"
    array_of_prev_versions=()
    for folder in $nifi_folders
    do
       if [ -d "$folder" ]; then
          if [[ $folder =~ [0-9]{12}$ ]]; then
            echo "$folder"
            array_of_prev_versions+=($folder)
          fi
      fi
    done

    if (( ${#array_of_prev_versions[*]} != 0 )); then
      prev_conf_folder=${array_of_prev_versions[-1]}"/conf"
      if [ -d "$prev_conf_folder" ]; then
         if ! [ -f ${prev_conf_folder}".not_configured_yet" ]; then
            echo "We are migrating from ${array_of_prev_versions[-1]}"
            cp -r $prev_conf_folder $NIFI_HOME
          fi
      fi
    fi
  fi
}

changePermission
configureUiSecurity
updateWardenLocalConfFile
setupWardenConfFile
migratePreviousConfiguration
enableFipsIfConfigured

rm -rf ${NIFI_HOME}/conf/.not_configured_yet
