%undefine __check_files

summary:     HPE DataFabric Ecosystem Pack: NiFi
license:     Hewlett Packard Enterprise, CopyRight
Vendor:      Hewlett Packard Enterprise
name:        mapr-nifi
version:     __RELEASE_VERSION__
release:     1
prefix:      /
group:       HPE
buildarch:   noarch
requires:    mapr-client >= 7.1.0, mapr-hadoop-client >= 3.3.4, zip
conflicts:   mapr-core < 7.1.0
AutoReqProv: no


%description
Apache NiFi distribution included in HPE DataFabric Software Ecosystem Pack
Commit: __GIT_COMMIT__
Tag: __RELEASE_BRANCH__

%clean
echo "NOOP"


%files
__PREFIX__/nifi
__PREFIX__/roles/nifi

%pre
if [ $1 -eq 2 ]; then
    MY_OLD_TIMESTAMP_VERSION=$(rpm -qi mapr-nifi | awk -F': ' '/Version/ {print $2}')
    MY_OLD_3DIGIT_VERSION="$(echo $MY_OLD_TIMESTAMP_VERSION | cut -d'.' -f1-3 )"
    MY_OLD_HOME_DIR=__PREFIX__/nifi/nifi-$MY_OLD_3DIGIT_VERSION

    rm -rf __PREFIX__/nifi/nifi-$MY_OLD_3DIGIT_VERSION/work

    mkdir -p __PREFIX__/nifi/nifi-${MY_OLD_TIMESTAMP_VERSION}/
    cp -r $MY_OLD_HOME_DIR/* __PREFIX__/nifi/nifi-${MY_OLD_TIMESTAMP_VERSION}/

    rm -rf __PREFIX__/nifi/nifi-$MY_OLD_3DIGIT_VERSION/lib/*
    rm -rf __PREFIX__/nifi/nifi-$MY_OLD_3DIGIT_VERSION/not-used-libs

    createDummyRpmFiles() {
      rpmFilePaths="$(rpm -ql mapr-nifi | grep "/lib/" | grep -e "nar$" -e "jar$")"
      while read filePath
      do
        parentDir="$(dirname "${filePath}")"
        if [ ! -d "${parentDir}" ]
        then
          mkdir -p "${parentDir}"
        fi
        touch "${filePath}"
      done <<< "${rpmFilePaths}"
    }
    createDummyRpmFiles
fi

%post

if [ $1 -eq 1 ] || [ $1 -eq 2 ]; then
  touch "__INSTALL_3DIGIT__/conf/.not_configured_yet"
fi


%preun
DAEMON_CONF="__PREFIX__/conf/daemon.conf"
MAPR_USER=${MAPR_USER:-$([ -f "$DAEMON_CONF" ] && grep "mapr.daemon.user" "$DAEMON_CONF" | cut -d '=' -f 2)}
MAPR_USER=${MAPR_USER:-"mapr"}

if sudo -u $MAPR_USER -E "__INSTALL_3DIGIT__/bin/nifi.sh" status &>/dev/null ; then
    RESULT=$(sudo -u $MAPR_USER -E "__INSTALL_3DIGIT__/bin/nifi.sh" stop 2>&1)
    STATUS=$?
    if [ $STATUS -ne 0 ] ; then
        echo "$RESULT"
    fi
fi

# $1 equals to 1 in %preun scriplet if it is an upgrade.
# We don't want to remove warden config in case of an upgrade,
# updating warden conf is handled by configure.sh
if [ ! $1 -eq 1 ]; then
  if [ -f __PREFIX__/conf/conf.d/warden.nifi.conf ]; then
    rm -Rf __PREFIX__/conf/conf.d/warden.nifi.conf
  fi
fi

createDummyRpmFiles() {
  rpmFilePaths="$(rpm -ql mapr-nifi | grep "/lib/" | grep -e "nar$" -e "jar$")"
  while read filePath
  do
    parentDir="$(dirname "${filePath}")"
    if [ ! -d "${parentDir}" ]
    then
      mkdir -p "${parentDir}"
    fi
    touch "${filePath}"
  done <<< "${rpmFilePaths}"
}
createDummyRpmFiles

%postun
if [ "$1" = "0" ]; then
    rm -Rf __PREFIX__/nifi/
    rm -f __PREFIX__/roles/nifi
fi


%posttrans
