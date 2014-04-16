package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.XmlUriConst;

final class EventTypeNamespaceDeclaration extends EventTypeNonSchema {

  EventTypeNamespaceDeclaration(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(XmlUriConst.W3C_XMLNS_2000_URI, (String)null, EVENT_CODE_DEPTH_TWO, ownerGrammar, eventTypeList, ITEM_NS);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////


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
