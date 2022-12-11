#!/usr/bin/env bash

GIT_COMMIT=$(git log -1 --pretty=format:"%H")
INSTALLATION_PREFIX=${INSTALLATION_PREFIX:="/opt/mapr"}
PKG_NAME=${PKG_NAME:="nifi"}
PKG_VERSION=${PKG_VERSION:=1.19.1.0}
PKG_3DIGIT_VERSION=${PKG_3DIGIT_VERSION:=1.19.1}
TIMESTAMP=${TIMESTAMP:=$(sh -c 'date "+%Y%m%d%H%M"')}
NIFI_ROOT=${NIFI_ROOT:="${INSTALLATION_PREFIX}/${PKG_NAME}/${PKG_NAME}-${PKG_3DIGIT_VERSION}"}

START_DIR=$(pwd)
DIST_DIR=${DIST_DIR:="dist"}
DEB_NIFI_DIR=${DEB_NIFI_DIR:="${DIST_DIR}/mapr-${PKG_NAME}/deb"}
RPM_NIFI_DIR=${RPM_NIFI_DIR:="$START_DIR/${DIST_DIR}/mapr-${PKG_NAME}/rpm"}
