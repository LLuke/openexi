package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

final class EventTypeComment extends EventTypeNonSchema {

  EventTypeComment(byte depth, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, (String)null, depth, ownerGrammar, eventTypeList, ITEM_CM);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public byte getItemType() {
//    return ITEM_CM;
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
    return new EventTypeComment(m_depth, ownerGrammar, eventTypeList);
  }
  
}
