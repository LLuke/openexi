package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

final class EventTypeCharactersReversed extends EventTypeCharacters {

  EventTypeCharactersReversed(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList);
  };

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
