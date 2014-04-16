package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

final class EventTypeElementReversed extends EventTypeElement {

  EventTypeElementReversed(String uri, String name, Grammar ownerGrammar, EventTypeList eventTypeList, 
      Grammar ensuingGrammar) {
    super(uri, name, ownerGrammar, eventTypeList, ensuingGrammar);
  }

  @Override
  public final boolean isPositionReversed() {
    return true;
  }

}
