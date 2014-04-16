package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

final class EventTypeSchemaElementWildcardAny extends EventTypeSchemaElementWildcard {

  EventTypeSchemaElementWildcardAny(int particle, int term, int index, int serial, 
    Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(particle, term, null, index, serial, ownerGrammar, eventTypeList, ITEM_SCHEMA_WC_ANY);
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
    return new EventTypeSchemaElementWildcardAny(
        particle, m_substance, 
        index, serial, m_ownerGrammar, eventTypeList);
  }

}
