#!/bin/bash

set -e

INSTALL_DIR=/home/icecp/"<%= version %>"-"<%= iteration %>"
SOFT_LINK=/home/icecp/icecp-node

after_remove()
{
  echo "Performing post-removal steps for <%= name %> version=<%= version %> iteration=<%= iteration %>"
  
  # Remove soft link and icecp-node directory contents
  if [ -L $SOFT_LINK ] && [ "$(readlink $SOFT_LINK)" = $INSTALL_DIR ]; then
    rm $SOFT_LINK
    echo "Removed soft link $SOFT_LINK"
  fi

  if [ -d $INSTALL_DIR ]; then
    rm -rf $INSTALL_DIR
    echo "Removed $INSTALL_DIR"
  fi
}

after_remove

