#!/bin/sh
#
# @see shongo-infrastructure-cesnet.js
#

# Check Node.js installed
if ! command -v node >/dev/null 2>&1; then
    cat <<EOF
Node.js is required.

Installation:

    # Install Node.js
    git clone https://github.com/joyent/node.git
    cd node
    git checkout v0.10.12
    ./configure --openssl-libpath=/usr/lib/ssl
    make
    make install

    # Configure NODE_PATH
    echo "export NODE_PATH=/usr/local/lib/node_modules:\$NODE_PATH" >> ~/.bash_profile

    # Install required modules
    npm install exec-sync
EOF
    exit 1
fi

# Execute Node.js
node  $(dirname $0)/shongo-infrastructure-cesnet.js $@
