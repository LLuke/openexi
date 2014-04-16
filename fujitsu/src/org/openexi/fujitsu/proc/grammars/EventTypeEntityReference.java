package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

final class EventTypeEntityReference extends EventTypeNonSchema {

  EventTypeEntityReference(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, (String)null, EVENT_CODE_DEPTH_TWO, ownerGrammar, eventTypeList, ITEM_ER);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public byte getItemType() {
//    return ITEM_ER;
//  }

//  @Override
//  public final int getPosition() {
//    return m_position;
//  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final boolean isContent() {
    return true;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeNonSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    return new EventTypeEntityReference(ownerGrammar, eventTypeList);
  }

}
