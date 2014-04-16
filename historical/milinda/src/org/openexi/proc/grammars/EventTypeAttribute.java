package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventTypeList;

abstract class EventTypeAttribute extends EventTypeNonSchema {

  EventTypeAttribute(String uri, String name, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(uri, name, EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, EventCode.ITEM_AT);
    assert ownerGrammar.getGrammarType() == Grammar.BUILTIN_GRAMMAR_ELEMENT;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public final byte getItemType() {
//    return EventCode.ITEM_AT;
//  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of AbstractEventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final boolean isContent() {
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeNonSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    throw new UnsupportedOperationException();
  }

}
