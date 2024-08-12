package be.smals.vas.integrations.helper.exception;

public class InvalidPseudonymException extends RuntimeException {

  public InvalidPseudonymException(final String message) {
    super(message);
  }

  public InvalidPseudonymException(final String message, final Exception e) {
    super(message, e);
  }
}
