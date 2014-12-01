#!/bin/bash

# This is a helper script for installing a given JAR over the
# JAR currently installed on the system (i.e. /home/icecp/...)
# It may be useful when testing new versions of icecp-node
# without creating an RPM every time. It should follow links
# and expand wildcards.
#
# Example:
#   sudo ./link-icecp-node.sh target/icecp-node*
#   sudo systemctl restart icecp

if [ -z "$1" ]; then
	echo "Usage: `basename "$0"` [path of icecp-node JAR to use]"
	exit 1
fi

FROM=$(readlink -m $1)
TO=$(readlink -m /home/icecp/icecp-node/lib/icecp-node*)

if [ `basename $FROM` != `basename $TO` ]; then
	echo "Names do not match: `basename $FROM` != `basename $TO`"
	exit 1
else 
	mv $TO $TO.replaced
	ln -s $FROM $TO
fi
