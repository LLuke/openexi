package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

final class EventTypeCharactersSecond extends EventTypeCharacters {

  EventTypeCharactersSecond(byte depth, Grammar ownerGrammar, EventTypeList eventTypeList) {
//    super(EventCode.EVENT_CODE_DEPTH_TWO, ownerGrammar, eventTypeList);
    super(depth, ownerGrammar, eventTypeList);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeNonSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    return new EventTypeCharactersSecond(m_depth, ownerGrammar, eventTypeList);
  }

}
