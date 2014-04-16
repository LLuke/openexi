package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

final class EventTypeAttributeWildcardAnyUntyped extends EventTypeNonSchema {

  EventTypeAttributeWildcardAnyUntyped(byte depth, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, (String)null, depth, ownerGrammar, eventTypeList, ITEM_AT_WC_ANY_UNTYPED);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public final byte getItemType() {
//    return ITEM_AT_WC_ANY_UNTYPED;
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
    return new EventTypeAttributeWildcardAnyUntyped(m_depth, ownerGrammar, eventTypeList);
  }
  
}
