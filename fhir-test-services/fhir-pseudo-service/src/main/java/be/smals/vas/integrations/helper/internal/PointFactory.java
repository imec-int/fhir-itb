package be.smals.vas.integrations.helper.internal;

import be.smals.vas.integrations.helper.Domain;
import be.smals.vas.integrations.helper.Domain.DomainImpl;

public abstract class PointFactory {

  protected final DomainImpl domain;

  protected PointFactory(final Domain domain) {
    this.domain = (DomainImpl) domain;
  }
}
