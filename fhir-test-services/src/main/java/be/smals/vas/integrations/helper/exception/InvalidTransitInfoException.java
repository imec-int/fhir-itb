package be.smals.vas.integrations.helper.exception;

public class InvalidTransitInfoException extends RuntimeException {

  public InvalidTransitInfoException(final String message) {
    super(message);
  }

  public InvalidTransitInfoException(final String message, final Exception e) {
    super(message, e);
  }
}
