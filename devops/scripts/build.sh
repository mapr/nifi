#!/usr/bin/env bash

. "$(dirname "${BASH_SOURCE[0]}")/_initialize_package_variables.sh"

function prepare_nifi_install_packages() {
  echo "Cleaning '${DIST_DIR}' dir..."
  rm -rf ${DIST_DIR}

  echo "Build shaded BouncyCastle"
  mvn -f bouncycastle-shaded/ clean install
  if (($?)); then
      return 1
  fi

  echo "Building project..."
  mvn -pl nifi-assembly -am clean install -DskipTests
  if (($?)); then
    return 1
  fi

  rpmbuild > /dev/null 2>&1
  if [ $? -ne 127 ]; then
    echo "Building rpm..."
    create_rpm_nifi_package
  fi

  dpkg-deb > /dev/null 2>&1
  if [ $? -ne 127 ]; then
    echo "Building deb..."
    create_deb_nifi_package
  fi

  cd ${DIST_DIR} && find . -not \( -name '*.deb' -or -name '*.rpm' \) -delete
  echo "Resulting packages:"
  find ./ -type f -name '*rpm' -exec readlink -f {} \;
  find ./ -type f -name '*deb' -exec readlink -f {} \;
}

function create_rpm_nifi_package() {
  mkdir -p ${RPM_NIFI_DIR}/{SPECS,INSTALL,SOURCES/${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION},BUILD,RPMS/noarch}
  pack_files "${RPM_NIFI_DIR}/SOURCES/${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION}"
  create_role ${RPM_NIFI_DIR}/SOURCES/${INSTALLATION_PREFIX} \
    ${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION} \
    "${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION}/bin/configure.sh" \
    "nifi"
  cp -r devops/specs/rpm/*.spec ${RPM_NIFI_DIR}/SPECS
  build_rpm ${RPM_NIFI_DIR}
}

function create_deb_nifi_package() {
  mkdir -p ${DEB_NIFI_DIR}/{DEBIAN,${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION}}
  pack_files "${DEB_NIFI_DIR}/${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION}"
  create_role ${DEB_NIFI_DIR}/${INSTALLATION_PREFIX} \
    ${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION} \
    "${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION}/bin/configure.sh" "nifi"
  cp -r devops/specs/deb/* ${DEB_NIFI_DIR}/DEBIAN
  build_deb ${DEB_NIFI_DIR}
}

function create_role() {
  PATH_ROLE=$1
  HOME=$2
  COMMAND=$3
  ROLE_NAME=$4
  mkdir -p ${PATH_ROLE}/roles
  echo -e "PKG_HOME_DIR=${HOME}\nPKG_CONFIG_COMMAND=${COMMAND}" >${PATH_ROLE}/roles/${ROLE_NAME}
}

function pack_files() {
  HOME_PATH=$1
  cp -r nifi-assembly/target/nifi-*-bin/nifi-*/* ${HOME_PATH}/
  cp ext-bin/* ${HOME_PATH}/bin
  cp ext-conf/* ${HOME_PATH}/conf
  echo ${PKG_3DIGIT_VERSION} >${HOME_PATH}/../nifiversion
}

function build_rpm() {
  RMP_DIR=$1
  replace_build_variables ${RMP_DIR}/SPECS
  rpmbuild --bb --define "_topdir ${RMP_DIR}" --buildroot=${RMP_DIR}/SOURCES ${RMP_DIR}/SPECS/*
  mv ${RMP_DIR}/RPMS/*/*rpm ${DIST_DIR}
}

function build_deb() {
  DEB_DIR=$1
  replace_build_variables ${DEB_DIR}/DEBIAN
  find ${DEB_DIR} -type f -exec md5sum \{\} \; 2>/dev/null |
    sed -e "s|${DEB_DIR}||" -e "s| \/| |" |
    grep -v DEBIAN >${DEB_DIR}/DEBIAN/md5sums
  echo "" >>${DEB_DIR}/DEBIAN/control
  dpkg-deb --build ${DEB_DIR} ${DIST_DIR}
}

function replace_build_variables() {
  REPLACE_DIR=$1
  find ${REPLACE_DIR} -type f -exec \
    sed -i "s|__GIT_COMMIT__|${GIT_COMMIT}|g" {} \;
  find ${REPLACE_DIR} -type f -exec \
    sed -i "s|__PREFIX__|${INSTALLATION_PREFIX}|g" {} \;
  find ${REPLACE_DIR} -type f -exec \
    sed -i "s|__VERSION__|${PKG_VERSION}|g" {} \;
  find ${REPLACE_DIR} -type f -exec \
    sed -i "s|__VERSION_3DIGIT__|${PKG_3DIGIT_VERSION}|g" {} \;
  find ${REPLACE_DIR} -type f -exec \
    sed -i "s|__RELEASE_BRANCH__|${BRANCH_NAME}|g" {} \;
  find ${REPLACE_DIR} -type f -exec \
    sed -i "s|__RELEASE_VERSION__|${PKG_VERSION}.${TIMESTAMP}|g" {} \;
  find ${REPLACE_DIR} -type f -exec \
    sed -i "s|__INSTALL_3DIGIT__|${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION}|g" {} \;
}

prepare_nifi_install_packages
