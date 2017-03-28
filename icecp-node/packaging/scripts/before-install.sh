#!/bin/bash

set -e 

before_install() 
{
  echo "Performing pre-install steps for <%= name %> version=<%= version %> iteration=<%= iteration %>"

  if [ $(cat /proc/1/comm) = "systemd" ]; then
    echo "Found systemd; attempting to stop any prior icecp service"

    # Stop (if any) previous versions of running icecp service
    if systemctl list-units --type=service | grep 'icecp' >/dev/null; then
      systemctl stop icecp
    fi

    # Disable (if any) previous version of icecp service and remove sym links created by enable command
    if [ -f $SYSCONFIGDIR/systemd/system/icecp.service ]; then
      systemctl disable icecp
    fi
  fi
}

before_install
