package be.smals.vas.integrations.helper;

import be.smals.vas.integrations.helper.Pseudonym.PseudonymImpl;
import be.smals.vas.integrations.helper.Value.ValueImpl;
import java.util.Objects;
import org.bouncycastle.math.ec.ECPoint;

public interface Point {
  abstract sealed class PointImpl implements Point permits PseudonymImpl, ValueImpl {

    protected final ECPoint ecPoint;
    protected final Domain.DomainImpl domain;

    protected PointImpl(final ECPoint ecPoint, final Domain domain) {
      this.ecPoint = ecPoint;
      this.domain = (Domain.DomainImpl) domain;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {return true;}
      if (!(obj instanceof final PointImpl that)) {return false;}
      return Objects.equals(ecPoint, that.ecPoint) &&
             Objects.equals(domain.key(), that.domain.key());
    }

    @Override
    public int hashCode() {
      return Objects.hash(ecPoint, domain.key());
    }

    @Override
    public String toString() {
      return "{" +
             "\"x\": \"" + ecPoint.getXCoord().toBigInteger() + "\", " +
             "\"y\": \"" + ecPoint.getYCoord().toBigInteger() + "\"," +
             "\"domain\": \"" + domain.key() + "\"}";
    }
  }
}
