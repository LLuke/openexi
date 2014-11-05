package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.IGrammar;

final class EventTypeArray extends EventType {
  
  public final Grammar ensuingGrammar;

  EventTypeArray(int localNameId, String name, EventTypeList eventTypeList, Grammar ensuingGrammar, IGrammar subsequentGrammar) {
    super(name, localNameId, EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_START_ARRAY_NAMED, EventDescription.EVENT_START_ARRAY, subsequentGrammar);
    this.ensuingGrammar = ensuingGrammar;
  }

}
