#!/bin/bash

set -e

before_remove() 
{
  echo "Performing pre-removal steps for <%= name %> version=<%= version %> iteration=<%= iteration %>"

  if [ $(cat /proc/1/comm) = "systemd" ]; then
    # Stop icecp service
    if systemctl list-units --type=service | grep 'icecp' >/dev/null; then
    systemctl stop icecp
    fi

    # Disable icecp service and remove sym links created by enable command
    systemctl disable icecp
  fi
}

before_remove
