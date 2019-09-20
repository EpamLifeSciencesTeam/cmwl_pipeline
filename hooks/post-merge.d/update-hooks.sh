#!/usr/bin/env bash

git_dir=$(git rev-parse --git-dir)

rm -rf "${git_dir}"/hooks/*

"${git_dir}"/../hooks/install.sh "${@}"
