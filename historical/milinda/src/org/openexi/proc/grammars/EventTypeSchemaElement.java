package org.openexi.proc.grammars;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EXIEvent;

final class EventTypeSchemaElement extends EventTypeSchemaParticle implements EXIEvent {

  EventTypeSchemaElement(int particle, int elem, String uri, String name, 
    int index, int serial, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(particle, elem, uri, name, index, serial, ownerGrammar, eventTypeList, ITEM_SCHEMA_SE);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final boolean isContent() {
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
    return new EventTypeSchemaElement(
       particle, m_substance, 
       m_uri, m_name, index, serial, m_ownerGrammar, eventTypeList);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_SE;
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
