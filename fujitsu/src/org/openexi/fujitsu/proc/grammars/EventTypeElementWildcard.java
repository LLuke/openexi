package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

class EventTypeElementWildcard extends EventTypeNonSchema {
  
  EventTypeElementWildcard(byte depth, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, (String)null, depth, ownerGrammar, eventTypeList, ITEM_SE_WC);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public byte getItemType() {
//    return ITEM_SE_WC;
//  }

//  @Override
//  public int getPosition() {
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
  EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    return new EventTypeElementWildcard(m_depth, ownerGrammar, eventTypeList);
  }
  
}
