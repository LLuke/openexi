package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventType;

public class EventTypeAccessor {

  public static Grammar getGrammar(EventType eventType) {
    return ((AbstractEventType)eventType).getGrammar();
  }
  
  public static String getContentGrammarRegime(EventType eventType) {
    return ((ContentGrammar)EventTypeAccessor.getGrammar(eventType)).getContentRegime();
  }
  
}
