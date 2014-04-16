package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.schema.EXISchema;

final class EventTypeSchemaEndElement extends EventTypeSchema implements EXIEvent {

  EventTypeSchemaEndElement(int serial, Grammar ownerGrammar, EventTypeList eventTypeList) {
    this(NIL_INDEX, serial, ownerGrammar, eventTypeList);
  }

  EventTypeSchemaEndElement(int index, int serial, Grammar ownerGrammar, 
    EventTypeList eventTypeList) {
    super(EXISchema.NIL_NODE, null, null, index, serial, 
        EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, ITEM_SCHEMA_EE);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  boolean isContent() {
    return true;
  }

  @Override
  public final EXIEvent asEXIEvent() {
    return this;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeSchema duplicate(EventTypeList eventTypeList) {
    return new EventTypeSchemaEndElement(index, m_ownerGrammar, eventTypeList);
  }

  @Override
  boolean isAugmented() {
    return false;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_EE;
  }
  
  public String getPrefix() {
    return null;
  }

  public CharacterSequence getCharacters() {
    return null;
  }

  public final EventTypeSchema getEventType() {
    return this;
  }

}
