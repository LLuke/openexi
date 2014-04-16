package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

final class EventTypeSelfContained extends EventTypeNonSchema {

  EventTypeSelfContained(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, (String)null, EVENT_CODE_DEPTH_TWO, ownerGrammar, eventTypeList, ITEM_SC);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public byte getItemType() {
//    return ITEM_SC;
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
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeNonSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    return new EventTypeSelfContained(ownerGrammar, eventTypeList);
  }
  
}
