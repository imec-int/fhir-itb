package be.smals.vas.integrations.helper.exception;

public class InvalidValueException extends RuntimeException {

  public InvalidValueException(final String message) {
    super(message);
  }

  public InvalidValueException(final String message, final Exception e) {
    super(message, e);
  }
}
