%undefine __check_files

summary:     MapR
license:     MapR Technologies, Inc., CopyRight
Vendor:      MapR Technologies, Inc., <support@maprtech.com>
name:        mapr-nifi
version:     __RELEASE_VERSION__
release:     1
prefix:      /
group:       MapR
buildarch:   x86_64
requires:    bash
AutoReqProv: no


%description
MapR nifi package.
Commit: __GIT_COMMIT__
Branch: __RELEASE_BRANCH__

%clean
echo "NOOP"


%files
__PREFIX__/nifi

%pre


%post

if [ "$1" = "1" ]; then
  touch "__INSTALL_3DIGIT__/conf/.not_configured_yet"
fi


%preun

%postun
if [ "$1" == "0" ]; then
    rm -Rf __PREFIX__/nifi/
    rm -f __PREFIX__/roles/nifi
fi


%posttrans
