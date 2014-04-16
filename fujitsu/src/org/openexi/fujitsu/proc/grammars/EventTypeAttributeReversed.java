package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

final class EventTypeAttributeReversed extends EventTypeAttribute {

  EventTypeAttributeReversed(String uri, String name, Grammar ownerGrammar, 
      EventTypeList eventTypeList) {
    super(uri, name, ownerGrammar, eventTypeList);
  }
  
  @Override
  public final boolean isPositionReversed() {
    return true;
  }
  
}
