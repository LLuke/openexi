package org.openexi.proc.grammars;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EXIEvent;

final class EventTypeStartDocument extends EventTypeNonSchema implements EXIEvent {

  EventTypeStartDocument(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, "#document", EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, ITEM_SD);
  }

  ///////////////////////////////////////////////////////////////////////////
  // EventCode methods
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public final int getPosition() {
//    return m_position;
//  }

  ///////////////////////////////////////////////////////////////////////////
  // EventType methods
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  boolean isContent() {
    return false;
  }

  @Override
  public final EXIEvent asEXIEvent() {
    return this;
  }

  ///////////////////////////////////////////////////////////////////////////
  // EventTypeNonSchema methods
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    assert false;
    return null;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventVariety() {
    return EXIEvent.EVENT_SD;
  }
  
  public String getPrefix() {
    return null;
  }

  public CharacterSequence getCharacters() {
    return null;
  }
  
  public EventType getEventType() {
    return this;
  }

}
