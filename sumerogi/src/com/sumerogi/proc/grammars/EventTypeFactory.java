package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.IGrammar;

class EventTypeFactory {
  
  private EventTypeFactory() {
  }
  
  static EventType creatEndObject(EventTypeList eventTypeList) {
    return new EventType((String)null, -1, 
        EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_END_OBJECT, EventDescription.EVENT_END_OBJECT, (IGrammar)null);
  }
  
  static EventType creatEndArray(EventTypeList eventTypeList) {
    return new EventType((String)null, -1, 
        EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_END_ARRAY, EventDescription.EVENT_END_ARRAY, (IGrammar)null);
  }
  
  static EventType createEndDocument(EventTypeList eventTypeList) {
    return new EventType((String)null, -1, 
        EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_END_DOCUMENT, EventDescription.EVENT_END_DOCUMENT, (IGrammar)null);
  }
  
  static EventType createStartDocument(EventTypeList eventTypeList) {
    return new EventType((String)null, -1, 
        EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_START_DOCUMENT, EventDescription.EVENT_START_DOCUMENT, (IGrammar)null);
  }

//  static EventTypeElement createStartElement(int localNameId, String localName,
//      EventTypeList eventTypeList, EXIGrammarUse ensuingGrammar) {
//      return new EventTypeElement(localNameId, localName, eventTypeList, ensuingGrammar, null);
//    }

}
