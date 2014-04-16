package org.openexi.scomp;

abstract class EventSEWildcardNS extends EventWildcardNS {

  protected EventSEWildcardNS(String uri) {
    super(uri);
  }

  @Override
  public byte getEventType() {
    return ELEMENT_WILDCARD_NS;
  }

}
