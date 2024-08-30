package be.smals.vas.integrations.helper.exception;

public class UnknownKidException extends InvalidTransitInfoException {

  private final String kid;

  public UnknownKidException(final String kid) {
    super("Unknown `kid` `" + kid + "` (or no private key found to decrypt it)");
    this.kid = kid;
  }

  public String getKid() {
    return kid;
  }
}
