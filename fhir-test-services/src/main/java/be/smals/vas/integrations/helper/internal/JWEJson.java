package be.smals.vas.integrations.helper.internal;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static javax.crypto.Cipher.DECRYPT_MODE;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.impl.AESGCM;
import com.nimbusds.jose.crypto.impl.CipherHelper;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONObjectUtils;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class will be replaced by Nimbus when release for JWEJson will be available
 * It is an adaptation simplified of the Nimbus JWEJson.
 *
 * @see <a href="https://bitbucket.org/connect2id/nimbus-jose-jwt/pull-requests/105" > Nimbus </a>
 */
public record JWEJson(
    Base64URL protectedHeader,
    Base64URL iv,
    Base64URL ciphertext,
    Base64URL tag,
    List<Recipient> recipients
) {
  public record Recipient(
      Map<String, Object> header,
      Base64URL encryptedKey
  ) {
    public String kid() {
      if (header == null) {
        return "";
      }
      return (String) header.getOrDefault("kid", "");
    }
  }

  /**
   * Method comes from Nimbus JWEJson,a bit modified to simplify
   *
   * @param json the JWE as Json format
   * @return JWEJson
   * @throws ParseException when json input is invalid
   */
  public static JWEJson parse(final String json) throws ParseException {
    final var jsonObject = JSONObjectUtils.parse(json);
    final var protectedHeader = JSONObjectUtils.getBase64URL(jsonObject, "protected");
    final var cipherText = JSONObjectUtils.getBase64URL(jsonObject, "ciphertext");
    final var iv = JSONObjectUtils.getBase64URL(jsonObject, "iv");
    final var authTag = JSONObjectUtils.getBase64URL(jsonObject, "tag");
    final var recipientList = new ArrayList<Recipient>();
    for (final Map<String, Object> recipientJSONObject : JSONObjectUtils.getJSONObjectArray(jsonObject, "recipients")) {
      final var header = JSONObjectUtils.getJSONObject(recipientJSONObject, "header");
      final var encryptedKey = JSONObjectUtils.getBase64URL(recipientJSONObject, "encrypted_key");
      recipientList.add(new Recipient(header, encryptedKey));
    }
    return new JWEJson(protectedHeader, iv, cipherText, authTag, recipientList);
  }

  /**
   * Decrypt the JWEJson and return the result as bytes[]
   *
   * @param recipientKid the recipient kid to get the encryptedKey
   * @param privateKey   the privateKey to decrypt the encryptedKey
   * @return cipherText decrypted as bytes[]
   */
  public byte[] decryptCipher(final String recipientKid, final PrivateKey privateKey)
      throws IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException,
             JOSEException {
    final var encryptedKey =
        recipients.stream()
                  .filter(recipient1 -> recipient1.kid().equals(recipientKid))
                  .findFirst()
                  .orElseThrow() //todo: what raise if recipient not found ?
                  .encryptedKey();
    final var decryptedKey = decryptEncryptedKey(privateKey, encryptedKey);
    return AESGCM.decrypt(
        decryptedKey,
        iv().decode(),
        ciphertext().decode(),
        protectedHeader().toString().getBytes(US_ASCII),
        tag().decode(),
        null);
  }

  private SecretKeySpec decryptEncryptedKey(final PrivateKey privateKey, final Base64URL encryptedKey)
      throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
    final var cipher = CipherHelper.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", null);
    cipher.init(DECRYPT_MODE, privateKey);
    return new SecretKeySpec(cipher.doFinal(encryptedKey.decode()), "AES");
  }
}
