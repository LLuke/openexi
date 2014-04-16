package org.openexi.proc.grammars;

import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;

public final class EventTypeSchema extends EventType {
  
  public final int nd;

  EventTypeSchema(int nd, String uri, String name, int uriId, int nameId, byte depth, 
      EventTypeList eventTypeList, byte itemType, EXIGrammar subsequentGrammar) {
    super(uri, name, uriId, nameId, depth, eventTypeList, itemType, NOT_AN_EVENT, subsequentGrammar);
    this.nd = nd;
  }

}
