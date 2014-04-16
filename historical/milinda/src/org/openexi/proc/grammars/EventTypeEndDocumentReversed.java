package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

final class EventTypeEndDocumentReversed extends EventTypeEndDocument {

  EventTypeEndDocumentReversed(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(ownerGrammar, eventTypeList);
  }

  @Override
  public final boolean isPositionReversed() {
    return true;
  }
  @Override
  final EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    // Never call duplicate method on this.
    throw new UnsupportedOperationException();
  }
  
}
