#!/bin/bash

set -e

SYSCONFIGDIR=/etc
INSTALL_DIR=/home/icecp/"<%= version %>"-"<%= iteration %>"
SOFTLINK=/home/icecp/icecp-node

after_install()
{
  echo "Performing after install steps for <%= name %> VERSION <%= version %> ITERATION <%= iteration %>"
  
  ln -sfn $INSTALL_DIR $SOFTLINK
  echo "Created soft link $SOFTLINK to $INSTALL_DIR"

  chmod 644 $SYSCONFIGDIR/systemd/system/icecp.service

  if [ $(cat /proc/1/comm) = "systemd" ]; then
    echo "Found systemd; attempting to start icecp service"

    # Reload daemon to pick up new changes
    systemctl daemon-reload
    echo "Ran systemctl daemon-reload"

    # Enable the service
    systemctl enable icecp
    echo "Enabled icecp service"

    # Enable the service
    systemctl start icecp
    echo "Started icecp service"
  else
    echo "No systemd found; run icecp manually"
  fi
}

after_install
