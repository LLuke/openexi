package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

abstract class EventTypeSchemaParticle extends EventTypeSchema {

  final int particle;

  EventTypeSchemaParticle(int particle, int term, String uri, String name, 
    int index, int serial, Grammar ownerGrammar, EventTypeList eventTypeList, byte itemType) {
    super(term, uri, name, index, serial, EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList, itemType);
    this.particle = particle;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventTypeSchema interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  final boolean isAugmented() {
    return false;
  }

}
