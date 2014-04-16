package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.IGrammar;

class EventTypeFactory {
  
  private EventTypeFactory() {
  }
  
  static EventType creatEndElement(byte depth, EventTypeList eventTypeList) {
    return new EventType((String)null, (String)null, -1, -1, 
        depth, eventTypeList, EventType.ITEM_EE, EventDescription.EVENT_EE, (IGrammar)null);
  }
  
  static EventType createEndDocument(EventTypeList eventTypeList) {
    return new EventType((String)null, (String)null, -1, -1, 
        EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_ED, EventDescription.EVENT_ED, (IGrammar)null);
  }
  
  static EventType createStartDocument(EventTypeList eventTypeList) {
    return new EventType((String)null, (String)null, -1, -1, 
        EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SD, EventDescription.EVENT_SD, (IGrammar)null);
  }

  static EventTypeElement createStartElement(int uriId,int localNameId, String uri, String localName,
      EventTypeList eventTypeList, EXIGrammarUse ensuingGrammar, EXIGrammar subsequentGrammar) {
      return new EventTypeElement(uriId, uri, localNameId, localName, eventTypeList, ensuingGrammar, subsequentGrammar);
    }

}
