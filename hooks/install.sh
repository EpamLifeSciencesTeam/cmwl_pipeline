#!/usr/bin/env bash

script_dir=$(dirname "${0}")
git_dir=$(git rev-parse --git-dir)

for hook_dir in "${script_dir}"/*.d/; do
  hook_name=$(basename "${hook_dir}" ".d")
  cp -r "${hook_dir}" "${git_dir}/hooks/"
  cp "${script_dir}/template" "${git_dir}/hooks/${hook_name}"
  chmod -R a+x "${git_dir}/hooks/${hook_name}.d"
  chmod a+x "${git_dir}/hooks/${hook_name}"
done
