%undefine __check_files

summary:     Ezmeral
license:     Hewlett Packard Enterprise, CopyRight
Vendor:      Hewlett Packard Enterprise, <ezmeral_software_support@hpe.com>
name:        mapr-nifi
version:     __RELEASE_VERSION__
release:     1
prefix:      /
group:       MapR
buildarch:   x86_64
requires:    mapr-client >= 7.1.0, mapr-hadoop-util >= 3.3.3
conflicts:   mapr-core < 7.1.0
AutoReqProv: no


%description
MapR nifi package.
Commit: __GIT_COMMIT__
Branch: __RELEASE_BRANCH__

%clean
echo "NOOP"


%files
__PREFIX__/nifi
__PREFIX__/roles/nifi

%pre
if [ $1 -eq 2 ]; then
    MY_OLD_TIMESTAMP=$(rpm -qi mapr-nifi | awk -F': ' '/Version/ {print $2}')
    MY_OLD_CD_VERSION="$(echo $MY_OLD_TIMESTAMP | cut -d'.' -f1-3 )"
    MY_OLD_CD_HOME=__PREFIX__/nifi/nifi-$MY_OLD_CD_VERSION
    mkdir -p __PREFIX__/nifi/nifi-$MY_OLD_TIMESTAMP/conf
    cp -r $MY_OLD_CD_HOME/conf/* __PREFIX__/nifi/nifi-${MY_OLD_TIMESTAMP}/conf

    if [ -d "$MY_OLD_CD_HOME/logs" ]; then
        mkdir -p __PREFIX__/nifi/nifi-${MY_OLD_TIMESTAMP}/logs
        cp -r $MY_OLD_CD_HOME/logs/* __PREFIX__/nifi/nifi-${MY_OLD_TIMESTAMP}/logs
    fi
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

if [ -f __PREFIX__/conf/conf.d/warden.nifi.conf ]; then
    rm -Rf __PREFIX__/conf/conf.d/warden.nifi.conf
fi

if [ -d "__INSTALL_3DIGIT__/not-used-libs/" ]; then
  mv __INSTALL_3DIGIT__/not-used-libs/* __INSTALL_3DIGIT__/lib
fi

%postun
if [ "$1" = "0" ]; then
    rm -Rf __PREFIX__/nifi/
    rm -f __PREFIX__/roles/nifi
fi


%posttrans
