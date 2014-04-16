package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

final class EventTypeSchemaElementWildcardNS extends EventTypeSchemaElementWildcard {

  EventTypeSchemaElementWildcardNS(int particle, int term, String uri, int index, int serial, 
    Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(particle, term, uri, index, serial, ownerGrammar, eventTypeList, ITEM_SCHEMA_WC_NS);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final boolean isContent() {
    return true;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeSchema duplicate(EventTypeList eventTypeList) {
    return new EventTypeSchemaElementWildcardNS(
        particle, m_substance, 
        m_uri, index, serial, m_ownerGrammar, eventTypeList);
  }

}
