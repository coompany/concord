package concord.identity

import java.security.{Key, KeyPair}
import java.util.Base64


object KeyEncoder {

    def encode(key: Key): String = Base64.getEncoder.encodeToString(key.getEncoded)

    def encode(keyPair: KeyPair): (String, String) = (encode(keyPair.getPublic), encode(keyPair.getPrivate))

}
