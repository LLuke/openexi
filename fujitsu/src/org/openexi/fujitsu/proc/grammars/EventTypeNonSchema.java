package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventTypeList;

abstract class EventTypeNonSchema extends AbstractEventType {

  protected EventTypeNonSchema(String uri, String name, byte depth, Grammar ownerGrammar, EventTypeList eventTypeList, byte itemType) {
    super(uri, name, depth, ownerGrammar, eventTypeList, itemType);
  }
  
  @Override
  public final boolean isSchemaInformed() {
    return false;
  }

  abstract EventTypeNonSchema duplicate(Grammar ownerGrammar, EventTypeList eventTypeList);

}
