package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;

final class EventTypeSchemaAttributeInvalid extends EventTypeSchema {
  
  EventTypeSchemaAttributeInvalid(EventTypeSchemaAttribute eventTypeSchemaAttribute, 
    Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(eventTypeSchemaAttribute.m_substance, eventTypeSchemaAttribute.getURI(), eventTypeSchemaAttribute.getName(), 
        eventTypeSchemaAttribute.index, eventTypeSchemaAttribute.serial, EVENT_CODE_DEPTH_THREE, ownerGrammar, eventTypeList, ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  boolean isContent() {
    return false;
  }

  @Override
  EventTypeSchema duplicate(EventTypeList eventTypeList) {
    // will never be called because it is an augmentation.
    throw new UnsupportedOperationException();
  }
  
  @Override
  boolean isAugmented() {
    return true;
  }

}
