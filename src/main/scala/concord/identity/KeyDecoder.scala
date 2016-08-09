package concord.identity

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, KeyPair, PrivateKey, PublicKey}
import java.util.Base64


private[concord] class KeyDecoder(algorithm: String) {

    def publicKey(pubStr: String): PublicKey = {
        val keyFactory = KeyFactory.getInstance(algorithm)
        val pubKeySpec = new X509EncodedKeySpec(Base64.getDecoder.decode(pubStr))
        keyFactory.generatePublic(pubKeySpec)
    }

    def privateKey(secret: String): PrivateKey = {
        val keyFactory = KeyFactory.getInstance(algorithm)
        val privKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder.decode(secret))
        keyFactory.generatePrivate(privKeySpec)
    }

}


object KeyDecoder {

    def keyPair(pubStr: String, secret: String, algorithm: String): KeyPair = {
        val decoder = new KeyDecoder(algorithm)
        new KeyPair(decoder.publicKey(pubStr), decoder.privateKey(secret))
    }

    def apply(algorithm: String): KeyDecoder = new KeyDecoder(algorithm)

}
