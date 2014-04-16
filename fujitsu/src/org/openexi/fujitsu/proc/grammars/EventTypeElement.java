package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.EXIEvent;

abstract class EventTypeElement extends EventTypeNonSchema implements EXIEvent {
  
  private final Grammar m_ensuingGrammar;

  EventTypeElement(String uri, String name, Grammar ownerGrammar, EventTypeList eventTypeList, Grammar ensuingGrammar) {
    super(uri, name, EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, ITEM_SE);
    m_ensuingGrammar = ensuingGrammar;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Accessors
  ///////////////////////////////////////////////////////////////////////////
  
  public Grammar getEnsuingGrammar() {
    return m_ensuingGrammar;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public byte getItemType() {
//    return ITEM_SE;
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
  // Implementation of EventTypeNonSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    throw new UnsupportedOperationException();
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

  public final EventType getEventType() {
    return this;
  }
  
}
