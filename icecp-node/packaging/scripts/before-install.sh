#!/bin/bash

set -e 

before_install() 
{
  echo "Performing pre-install steps for <%= name %> version=<%= version %> iteration=<%= iteration %>"

  # Stop (if any) previous versions of running icecp service
  if systemctl list-units --type=service | grep 'icecp' >/dev/null; then
    systemctl stop icecp
  fi

  # Disable (if any) previous version of icecp service and remove sym links created by enable command
  systemctl disable icecp
}

before_install
