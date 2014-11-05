package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.IGrammar;
import com.sumerogi.proc.common.StringTable;

final class EventTypeObject extends EventType {

  public final Grammar objectGrammar;

  EventTypeObject(int localNameId, String name, EventTypeList eventTypeList, Grammar ensuingGrammar, IGrammar subsequentGrammar) {
    super(name, localNameId, EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_START_OBJECT_NAMED, EventDescription.EVENT_START_OBJECT, subsequentGrammar);
    this.objectGrammar = ensuingGrammar;
  }

  EventTypeObject(EventTypeList eventTypeList, Grammar ensuingGrammar, IGrammar subsequentGrammar) {
    super((String)null, StringTable.NAME_NONE, EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_START_OBJECT_ANONYMOUS, EventDescription.EVENT_START_OBJECT, subsequentGrammar);
    this.objectGrammar = ensuingGrammar;
  }

}
