package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.EXIEvent;

class EventTypeEndDocument extends EventTypeNonSchema implements EXIEvent {

  EventTypeEndDocument(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, "#document", EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, ITEM_ED);
  }

  ///////////////////////////////////////////////////////////////////////////
  // EventCode methods
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public int getPosition() {
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
    return EXIEvent.EVENT_ED;
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
