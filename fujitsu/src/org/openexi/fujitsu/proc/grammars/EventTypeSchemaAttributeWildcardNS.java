package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

final class EventTypeSchemaAttributeWildcardNS extends EventTypeSchemaAttributeWildcard {

  EventTypeSchemaAttributeWildcardNS(int wc, String uri, int serial, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(wc, uri, serial, EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, ITEM_SCHEMA_AT_WC_NS);
  }
  
//  @Override
//  public byte getItemType() {
//    return ITEM_SCHEMA_AT_WC_NS;
//  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeSchema duplicate(EventTypeList eventTypeList) {
    return new EventTypeSchemaAttributeWildcardNS(
        m_substance, m_uri, serial, m_ownerGrammar, eventTypeList);
  }

  @Override
  boolean isAugmented() {
    return false;
  }

}
