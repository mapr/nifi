#!/usr/bin/env bash

. "$(dirname "${BASH_SOURCE[0]}")/_configure_variables.sh"

RESTART_NEED=false

function setupWardenConfFile() {
  if ! [ -d ${MAPR_WARDEN_CONF_DIR} ]; then
    mkdir -p ${MAPR_WARDEN_CONF_DIR} >/dev/null 2>&1
  fi
  if [ -f ${MAPR_WARDEN_CONF} ]; then
    diff=$(diff ${WARDEN_CONF} ${MAPR_WARDEN_CONF})
    if [ ! -z "$diff" ]; then
      RESTART_NEED=true
    fi
  fi

  user=$(cat $BOOTSTRAP_CONF | grep 'run.as=' | sed 's/\(run.as=\)//')
  if [ "$user" == "$MAPR_USER" ]; then
    cp $WARDEN_CONF $MAPR_WARDEN_CONF_DIR
  fi
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
    nifi.listener.bootstrap.port"

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
  user=$(cat $BOOTSTRAP_CONF | grep 'run.as=' | sed 's/\(run.as=\)//')
  group=$MAPR_GROUP
  if [ -z "$user" ]; then
    user="$MAPR_USER"
    sed -i "s~run.as=.*~run.as=$MAPR_USER~" $BOOTSTRAP_CONF
  fi
  oldUser=$(stat -c '%U' $NIFI_CONF)

  if [ "$user" != "$MAPR_USER" ]; then
    group=$user
  fi

  chown -R ${user}:${group} ${NIFI_HOME}
  chown ${MAPR_USER}:${MAPR_GROUP} ${WARDEN_CONF}

  if [ -f ${PID_FILE} ]; then
    chown -R ${user}:${MAPR_GROUP} ${PID_FILE}
  fi

  if [ -f ${STATUS_FILE} ]; then
    chown -R ${user}:${MAPR_GROUP} ${STATUS_FILE}
  fi

  if [ -f ${NIFI_HOME}"/conf/.not_configured_yet" ]; then
    chmod 700 ${NIFI_HOME}/conf/
    chmod 600 ${NIFI_HOME}/conf/*
    chmod 644 ${WARDEN_CONF}
  fi
}

function configureUiSecurity() {
  hostName=`hostname -f`
  if [ -f ${NIFI_HOME}"/conf/.not_configured_yet" ]; then
    sed -i "s~0.0.0.0*~$hostName~" $NIFI_CONF
  fi
  if [ "$IS_SECURED" == "true" ]; then
    sed -i "s~nifi.remote.input.secure=.*~nifi.remote.input.secure=true~" $NIFI_CONF
    sed -i "s~nifi.web.http.host=.*~nifi.web.http.host=~" $NIFI_CONF
    sed -i "s~nifi.web.http.port=.*~nifi.web.http.port=~" $NIFI_CONF

    port=$(cat $NIFI_CONF | grep 'nifi.web.https.port=' | sed 's/\(nifi.web.https.port=\)//')
    if [ -z "$port" ]; then
      sed -i "s~nifi.web.https.port=.*~nifi.web.https.port=12443~" $NIFI_CONF
    fi

    host=$(cat $NIFI_CONF | grep 'nifi.web.https.host=' | sed 's/\(nifi.web.https.host=\)//')
    if [ -z "$host" ]; then
      sed -i "s~nifi.web.https.host=.*~nifi.web.https.host=$hostName~" $NIFI_CONF
    fi
  else
    sed -i "s~nifi.remote.input.secure=.*~nifi.remote.input.secure=false~" $NIFI_CONF
    sed -i "s~nifi.web.https.host=.*~nifi.web.https.host=~" $NIFI_CONF
    sed -i "s~nifi.web.https.port=.*~nifi.web.https.port=~" $NIFI_CONF

    port=$(cat $NIFI_CONF | grep 'nifi.web.http.port=' | sed 's/\(nifi.web.http.port=\)//')
    if [ -z "$port" ]; then
      sed -i "s~nifi.web.http.port=.*~nifi.web.http.port=12080~" $NIFI_CONF
    fi

    host=$(cat $NIFI_CONF | grep 'nifi.web.http.host=' | sed 's/\(nifi.web.http.host=\)//')
    if [ -z "$host" ]; then
      sed -i "s~nifi.web.http.host=.*~nifi.web.http.host=$hostName~" $NIFI_CONF
    fi
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
            array_of_prev_versions+=($folder)
          fi
      fi
    done

    if (( ${#array_of_prev_versions[*]} != 0 )); then
      prev_conf_folder=${array_of_prev_versions[-1]}"/conf"
      if [ -d "$prev_conf_folder" ]; then
         if ! [ -f ${prev_conf_folder}".not_configured_yet" ]; then
            echo "Migrating from ${array_of_prev_versions[-1]}"
            if [ -f ${prev_conf_folder}/warden.nifi.conf ]; then
              rm $prev_conf_folder/warden.nifi.conf
            fi
            cp -r $prev_conf_folder $NIFI_HOME
            rm -rf ${NIFI_HOME}/conf/.not_configured_yet
          fi
      fi
    fi
  fi
}

createRestartFile(){
  if [ "$RESTART_NEED" = true ] ; then
    role="nifi"
    mkdir -p ${MAPR_CONF_DIR}/restart
    cat > "${MAPR_CONF_DIR}/restart/$role-${NIFI_VERSION}.restart" <<EOF
#!/bin/bash
if [ -z "${MAPR_TICKETFILE_LOCATION}" ] && [ -e "${MAPR_HOME}/conf/mapruserticket" ]; then
    export MAPR_TICKETFILE_LOCATION="${MAPR_HOME}/conf/mapruserticket"
fi
maprcli node services -action restart -name ${role} -nodes $(hostname)
EOF
    chmod +x "${MAPR_CONF_DIR}/restart/$role-${NIFI_VERSION}.restart"
    chown -R $MAPR_USER:$MAPR_GROUP "${MAPR_CONF_DIR}/restart/$role-${NIFI_VERSION}.restart"
  fi
}

#$1 - libName
function moveLibToNotUsedLibs() {
  if ! [ -d $NIFI_NOT_USED_LIBS ]; then
    mkdir -p $NIFI_NOT_USED_LIBS >/dev/null 2>&1
  fi
  if [ -f $NIFI_LIBS$1 ]; then
    mv $NIFI_LIBS$1 $NIFI_NOT_USED_LIBS
    if (($?)); then
      echo "Error: while moving $NIFI_LIBS$1 to $NIFI_NOT_USED_LIBS"
    else
      if [ ! -f ${NIFI_HOME}"/conf/.not_configured_yet" ]; then
        RESTART_NEED=true
      fi
    fi
  fi
}

#$1-lib_name
function restoreLibFromNotUsedLibs() {
  if [ -d $NIFI_NOT_USED_LIBS ]; then
    if [ -f $NIFI_NOT_USED_LIBS$1 ]; then
      mv $NIFI_NOT_USED_LIBS$1 $NIFI_LIBS
      if (($?)); then
         echo "Error: while moving $NIFI_NOT_USED_LIBS$1 from $NIFI_LIBS"
      else
        if [ ! -f ${NIFI_HOME}"/conf/.not_configured_yet" ]; then
          RESTART_NEED=true
        fi
      fi
      if ! [ "$(ls -A $NIFI_NOT_USED_LIBS)" ]; then
        rm -rf $NIFI_NOT_USED_LIBS
      fi
    fi
  fi
}

function verifyHbaseInstalled() {
 lib_name="nifi-hbase-mapr_1*.nar"
 if ! [ -d "$MAPR_HOME/hbase" ]; then
   moveLibToNotUsedLibs $lib_name
 else
   restoreLibFromNotUsedLibs $lib_name
 fi
}

function verifyHiveInstalled() {
 lib_name="nifi-eep-hive3-nar-*.nar"
 if ! [ -d "$MAPR_HOME/hive" ]; then
   moveLibToNotUsedLibs $lib_name
 else
   restoreLibFromNotUsedLibs $lib_name
 fi
}

verifyHbaseInstalled
verifyHiveInstalled
migratePreviousConfiguration
configureUiSecurity
changePermission
updateWardenLocalConfFile
setupWardenConfFile
enableFipsIfConfigured
createRestartFile

rm -rf ${NIFI_HOME}/conf/.not_configured_yet
