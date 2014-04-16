package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.util.URIConst;

final class EventTypeNamespaceDeclaration extends EventTypeNonSchema {

  EventTypeNamespaceDeclaration(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(URIConst.W3C_XMLNS_2000_URI, (String)null, EVENT_CODE_DEPTH_TWO, ownerGrammar, eventTypeList, ITEM_NS);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public byte getItemType() {
//    return ITEM_NS;
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
    return new EventTypeNamespaceDeclaration(ownerGrammar, eventTypeList);
  }
  
}
