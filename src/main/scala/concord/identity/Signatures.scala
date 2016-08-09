package concord.identity

import java.security.{KeyPair, PublicKey, Signature => JSignature}
import java.util.Base64

import play.api.libs.functional.syntax._
import play.api.libs.json._


trait MessageSigner {

    def signMessage(message: String): Signature

}


trait MessageVerifier {

    def verifySignature(signature: Signature, message: String): Boolean

}


class Signatures(keyPair: KeyPair, signAlgorithm: String, keyDecoder: KeyDecoder) extends MessageSigner with MessageVerifier {

    private val sign = JSignature.getInstance(signAlgorithm)

    private val encoder = Base64.getEncoder
    private val decoder = Base64.getDecoder

    override def signMessage(message: String): Signature = {
        sign.initSign(keyPair.getPrivate)
        sign.update(message.getBytes)
        Signature(encoder.encodeToString(sign.sign()), keyPair.getPublic)
    }

    override def verifySignature(signature: Signature, message: String): Boolean = {
        sign.initVerify(signature.publicKey)
        sign.update(message.getBytes)
        sign.verify(decoder.decode(signature.signature))
    }

    // Signature writes / reads
    implicit val signatureWrites = new Writes[Signature] {
        override def writes(o: Signature): JsValue = Json.obj(
            "signature" -> o.signature,
            "publicKey" -> KeyEncoder.encode(o.publicKey)
        )
    }
    implicit val signatureReads: Reads[Signature] = (
            (JsPath \ "signature").read[String] and
            (JsPath \ "publicKey").read[String]
        )((signature, key) => Signature(signature, keyDecoder.publicKey(key)))

}


object Signatures {

    def apply(keyPair: KeyPair, signAlgorithm: String, keyDecoder: KeyDecoder): Signatures =
        new Signatures(keyPair, signAlgorithm, keyDecoder)

}


case class Signature(signature: String, publicKey: PublicKey)
