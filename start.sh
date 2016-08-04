#!/bin/bash

valid_opts=(start join)
def_port=1338

case $1 in
    start)
        CONCORD_NODE_ID=1010111010000010111000100101101111011100111100110100100000110110100010110101101001101001110110010101110110100100011000100110000010010111001110100110001000100101 ./target/universal/stage/bin/concord
        exit 0
        ;;
    join)
        if [ -z "$2" ] ; then
            port=$def_port
        else
            port=$2
        fi
        echo "Starting join on port ${port}"
        ./target/universal/stage/bin/concord -Dconcord.bootstrap.joining=true -Dconcord.port=${port}
        exit 0
        ;;
    *)
        printf "Invalid option: $1\nValid options are: ${valid_opts[*]}\n" >&2
        exit 1
        ;;
esac
