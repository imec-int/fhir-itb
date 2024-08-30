package be.smals.vas.integrations.helper.utils;

@SuppressWarnings("unchecked")
public final class SneakyThrowUtil {

  private SneakyThrowUtil() {
  }

  public static <T extends Exception, R> R sneakyThrow(final Exception t) throws T {
    throw (T) t;
  }
}
