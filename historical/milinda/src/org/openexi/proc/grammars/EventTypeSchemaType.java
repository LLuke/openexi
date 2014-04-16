package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.schema.EXISchema;

final class EventTypeSchemaType extends EventTypeSchema {

  EventTypeSchemaType(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(EXISchema.NIL_NODE, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", 
        NIL_INDEX, -1, EVENT_CODE_DEPTH_TWO, ownerGrammar, eventTypeList, ITEM_SCHEMA_TYPE);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem
  ///////////////////////////////////////////////////////////////////////////


  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType 
  ///////////////////////////////////////////////////////////////////////////

  @Override
  boolean isContent() {
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeSchema
  ///////////////////////////////////////////////////////////////////////////

  @Override
  boolean isAugmented() {
    return true;
  }

  @Override
  EventTypeSchema duplicate(EventTypeList eventTypeList) {
    // will never be called because it is an augmentation.
    throw new UnsupportedOperationException();
  }

}
