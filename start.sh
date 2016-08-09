#!/bin/bash

valid_opts=(start join)
def_port=1338

case $1 in
    start)
        CONCORD_PUBLIC_KEY="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCBzDY6sycgwD8SFIWB0OKqknoYbVh+xWSzq/fMoJ9PIEhP7xsXdmjk+lrKgfxQ5RtMO+Av7SoLGrq4D6KK5Nh6MiDGEtZ9Wzpl6Yan4tviVDLSoIawYtFI1BcGlsN9iPGN7lf/rbu/Jmx0bGXr8YPAeQ82Z3/b7iMlyRq8foJTCwIDAQAB" \
            CONCORD_SECRET_KEY="MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIHMNjqzJyDAPxIUhYHQ4qqSehhtWH7FZLOr98ygn08gSE/vGxd2aOT6WsqB/FDlG0w74C/tKgsaurgPoork2HoyIMYS1n1bOmXphqfi2+JUMtKghrBi0UjUFwaWw32I8Y3uV/+tu78mbHRsZevxg8B5DzZnf9vuIyXJGrx+glMLAgMBAAECgYB0PbKDN1ZfWkwhoQc/jxavs6ZsoeCTB6x7zBKLy31gm4SnMXeNt5mRFzSgD3Er8UJVL3pC8Z6pezKlDDuBMBk9b4EO4OYsRGO09sKGglpYqiu3ZeNd9BEUeE5J6+nq4dYSw11U1rL1h0PNknJXZdH/EI+JghqCbPJIXWJ1bMWwQQJBALmvzTZAf34vJkb0U8ks9k049cPN9Fu+g7a8Zu3BtSYHLSfJzFDxGFNAPMtNVVM5Pr2pN3x4b5f2gm0IotD1pzMCQQCy8pxHfyGICs52hSaGX9s9sC1bD5GmvDIQsWDBDLW4Gum6dtsLqfR4ypj9wevGx4uu4KgcruCWo1MNcTET5cTJAkB3MFxF7aKoiXVFaEF7yYuTx/MhK0sltKVxH3/mL0eq0EJw3rxyXD9j+MDNMqeJUx5tuXevQtNGtOnFORzhij03AkEAo6jjaThKUIOhpB/OxiKw/tA8CwZILXf9SesQFD8tiz2B+fluCFLdtgOEvMA4hMpHZB8vYVxHJz4kXSzit9HykQJALFatbGCLzbf34r1Kz7VrcoAWWiPFJLFFqzc0tRK3d+d59hLPOGuNh7r2mxzloCJ6SOQSVI522P3yWtvVuvivQg==" \
            CONCORD_X_NONCE="537447733960778879600970818141904316893166563717" \
            ./target/universal/stage/bin/concord
        exit 0
        ;;
    join)
        if [ -z "$2" ] ; then
            port=$def_port
        else
            port=$2
        fi
        echo "Starting join on port ${port}"
        CONCORD_BOOT_ID="1100001001000111000001101111100000011000001111000101011101110111010101010011010101100010110101001100101010010000010111100100001000110000111101111100001011101101" \
            CONCORD_BOOT_NONCE="537447733960778879600970818141904316893166563717" \
            ./target/universal/stage/bin/concord -Dconcord.bootstrap.joining=true -Dconcord.port=${port}
        exit 0
        ;;
    *)
        printf "Invalid option: $1\nValid options are: ${valid_opts[*]}\n" >&2
        exit 1
        ;;
esac
