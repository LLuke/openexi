package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EXIEvent;

final class EventTypeEndElementSecond extends EventTypeEndElement implements EXIEvent {
 
  EventTypeEndElementSecond(Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(EventCode.EVENT_CODE_DEPTH_TWO, ownerGrammar, eventTypeList);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////

//  @Override
//  public final int getPosition() {
//    return m_position;
//  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeNonSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList) {
    return new EventTypeEndElementSecond(ownerGrammar, eventTypeList);
  }

}
