package be.smals.vas.integrations.helper;

import static java.math.BigInteger.ZERO;
import static java.util.UUID.randomUUID;

import be.smals.vas.integrations.helper.PseudonymAtRestFactory.PseudonymAtRestFactoryImpl;
import be.smals.vas.integrations.helper.PseudonymFactory.PseudonymFactoryImpl;
import be.smals.vas.integrations.helper.PseudonymInTransitFactory.PseudonymInTransitFactoryImpl;
import be.smals.vas.integrations.helper.TransitInfoFactory.TransitInfoFactoryImpl;
import be.smals.vas.integrations.helper.ValueFactory.ValueFactoryImpl;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonPrimitive;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import javax.crypto.SecretKey;
import org.bouncycastle.math.ec.ECCurve;

public interface Domain {

  ValueFactory valueFactory();

  PseudonymFactory pseudonymFactory();

  PseudonymInTransitFactory pseudonymInTransitFactory();

  PseudonymAtRestFactory pseudonymAtRestFactory();

  TransitInfoFactory transitInfoFactory();

  class DomainImpl implements Domain {

    private final String key;
    private final String crv;
    private final ECCurve curve;
    private final String audience;
    private final int bufferSize;
    private final Map<String, SecretKey> secretKeys;
    private final String activeKid;
    private final EncryptionMethod activeKeyEncryptionMethod;
    private final Duration inTransitTtl;
    private final PseudonymisationClient pseudonymisationClient;
    private final PseudonymisationHelper pseudonymisationHelper;
    private final ValueFactory valueFactory;
    private final PseudonymFactory pseudonymFactory;
    private final PseudonymInTransitFactory pseudonymInTransitFactory;
    private final PseudonymAtRestFactory pseudonymAtRestFactory;
    private final TransitInfoFactory transitInfoFactory;
    private final SecureRandom secureRandom;

    DomainImpl(final String key,
               final String crv,
               final ECCurve curve,
               final String audience,
               final int bufferSize,
               final Map<String, SecretKey> secretKeys,
               final String activeKid,
               final EncryptionMethod activeKeyEncryptionMethod,
               final Duration inTransitTtl,
               final PseudonymisationHelper pseudonymisationHelper,
               final PseudonymisationClient pseudonymisationClient,
               final SecureRandom secureRandom) {
      this.key = key;
      this.crv = crv;
      this.curve = curve;
      this.audience = audience;
      this.bufferSize = bufferSize;
      this.secretKeys = secretKeys;
      this.activeKid = activeKid;
      this.activeKeyEncryptionMethod = activeKeyEncryptionMethod;
      this.inTransitTtl = inTransitTtl;
      this.pseudonymisationHelper = pseudonymisationHelper;
      this.pseudonymisationClient = pseudonymisationClient;
      this.valueFactory = new ValueFactoryImpl(this);
      this.pseudonymFactory = new PseudonymFactoryImpl(this);
      this.pseudonymInTransitFactory = new PseudonymInTransitFactoryImpl(this);
      this.pseudonymAtRestFactory = new PseudonymAtRestFactoryImpl(this);
      this.transitInfoFactory = new TransitInfoFactoryImpl(this);
      this.secureRandom = secureRandom;
    }

    String key() {
      return key;
    }

    String crv() {
      return crv;
    }

    ECCurve curve() {
      return curve;
    }

    String audience() {
      return audience;
    }

    int bufferSize() {
      return bufferSize;
    }

    Map<String, SecretKey> secretKeys() {
      return secretKeys;
    }

    /**
     * Try to get the secretKey by its kid. If not found, returns {@code null} and reload the domains asynchronously.
     *
     * @param kid of the secretKey from Domain
     * @return the SecretKeySpec to decrypt/encrypt transitInfo or {@code null} if the key is not found.
     */
    SecretKey getSecretKey(final String kid) {
      final var secretKey = secretKeys.get(kid);
      if (secretKey == null) {
        pseudonymisationHelper.refreshDomainAsynchronously();
      }
      return secretKey;
    }

    /**
     * Try to get the active kid. If not found, returns {@code null} and reload the domains asynchronously.
     *
     * @return the kid of the secret key to use to encrypt transitInfo or {@code null} if there is no active kid (which should not happen).
     */
    String activeKid() {
      if (activeKid == null) {
        pseudonymisationHelper.refreshDomainAsynchronously();
      }
      return activeKid;
    }

    EncryptionMethod activeKeyEncryptionMethod() {
      return activeKeyEncryptionMethod;
    }

    Duration inTransitTtl() {
      return inTransitTtl;
    }

    PseudonymisationHelper pseudonymisationHelper() {
      return pseudonymisationHelper;
    }

    PseudonymisationClient pseudonymisationClient() {
      return pseudonymisationClient;
    }

    String createPayload(final Pseudonym pseudonym) {
      return createPayload(pseudonym, pseudonym instanceof PseudonymInTransit ? ((PseudonymInTransit) pseudonym).transitInfo().asString() : null);
    }

    String createPayload(final Pseudonym pseudonym, final String transitInfo) {
      final var payload = new JsonObject();
      payload.add("id", new JsonPrimitive(randomUUID().toString()));
      payload.add("crv", new JsonPrimitive(crv));
      payload.add("x", new JsonPrimitive(pseudonym.xAsBase64()));
      payload.add("y", new JsonPrimitive(pseudonym.yAsBase64()));
      if (transitInfo != null) {
        payload.add("transitInfo", new JsonPrimitive(transitInfo));
      }
      return payload.toString();
    }

    BigInteger createRandom() {
      BigInteger random;
      // 1 is excluded to prevent no-op blinding
      // P521.getOrder() is excluded to prevent `INF` (infinite) result
      // Not sure those checks are necessary because I guess BouncyCastle already does it
      final var curveOrder = curve.getOrder();
      do {
        random = curve.randomFieldElementMult(secureRandom).toBigInteger();
      } while (random.equals(ZERO) || random.equals(curveOrder));
      return random;
    }

    @Override
    public ValueFactory valueFactory() {
      return valueFactory;
    }

    @Override
    public PseudonymFactory pseudonymFactory() {
      return pseudonymFactory;
    }

    @Override
    public PseudonymInTransitFactory pseudonymInTransitFactory() {
      return pseudonymInTransitFactory;
    }

    @Override
    public PseudonymAtRestFactory pseudonymAtRestFactory() {
      return pseudonymAtRestFactory;
    }

    @Override
    public TransitInfoFactory transitInfoFactory() {
      return transitInfoFactory;
    }
  }
}
