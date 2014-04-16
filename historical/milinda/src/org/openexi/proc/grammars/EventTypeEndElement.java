package org.openexi.proc.grammars;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EXIEvent;

abstract class EventTypeEndElement extends EventTypeNonSchema implements EXIEvent {

  EventTypeEndElement(byte depth, Grammar ownerGrammar, EventTypeList eventTypeList) {
    super((String)null, (String)null, depth, ownerGrammar, eventTypeList, ITEM_EE);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public final byte getItemType() {
//    return ITEM_EE;
//  }

  @Override
  public final boolean isContent() {
    return true;
  }
  
  @Override
  public final EXIEvent asEXIEvent() {
    return this;
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

  public final EventType getEventType() {
    return this;
  }

}
