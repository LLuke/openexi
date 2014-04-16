package org.openexi.scomp;

abstract class EventATWildcardNS extends EventWildcardNS {

  protected EventATWildcardNS(String uri) {
    super(uri);
  }

  @Override
  public byte getEventType() {
    return ATTRIBUTE_WILDCARD_NS;
  }
  
}
