#!/usr/bin/env bash

script_dir=$(dirname "${0}")
hook_name=$(basename "${0}")

hook_dir="${script_dir}/${hook_name}.d"

if [[ -d ${hook_dir} ]]; then
  for hook in "${hook_dir}"/*; do
    "${hook}" "${@}"
    exit_code=${?}

    if [[ ${exit_code} != 0 ]]; then
      script_name=$(basename "${hook}")
      echo "Hook ${hook_name} : ${script_name} failed!"
      exit ${exit_code}
    fi
  done
fi

exit 0
