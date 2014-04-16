package org.openexi.proc.grammars;

import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.IGrammar;

final class EventTypeElement extends EventType {

  public final Grammar ensuingGrammar;

  EventTypeElement(int uriId, String uri, int localNameId, String name, EventTypeList eventTypeList, Grammar ensuingGrammar, IGrammar subsequentGrammar) {
    super(uri, name, uriId, localNameId, EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SE, EventDescription.EVENT_SE, subsequentGrammar);
    this.ensuingGrammar = ensuingGrammar;
  }

}
