package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

abstract class EventTypeSchemaElementWildcard extends EventTypeSchemaParticle {

  EventTypeSchemaElementWildcard(int particle, int term, String uri, int index, int serial, 
    Grammar ownerGrammar, EventTypeList eventTypeList, byte itemType) {
    super(particle, term, uri, (String)null, index, serial, ownerGrammar, eventTypeList, itemType);
  }
  
}
