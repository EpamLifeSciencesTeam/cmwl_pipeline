#!/usr/bin/env bash

rm -rf "${GIT_DIR}"/hooks/*

"${GIT_DIR}"/../hooks/install.sh "${@}"
