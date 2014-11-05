package com.sumerogi.proc.grammars;

//import com.sumerogi.proc.common.EventType;

public abstract class BuiltinGrammar extends Grammar {

  // N_NONSCHEMA_ITEMS must be ITEM_EO plus 1
//  static final short N_NONSCHEMA_ITEMS = EventType.ITEM_EO + 1; 
  
  protected BuiltinGrammar(byte grammarType, GrammarCache grammarCache) {
    super(grammarType, grammarCache);
  }

/*  
  public final EventType duplicate(EventType eventType, EventTypeList eventTypeList) {
    switch (eventType.itemType) {
//      case EventType.ITEM_AT_WC_ANY_UNTYPED:
//        return new EventType(eventType.depth, eventTypeList, EventType.ITEM_AT_WC_ANY_UNTYPED, (IGrammar)null);
//      case EventType.ITEM_CH:
//        return new EventType(eventType.depth, eventTypeList, EventType.ITEM_CH, eventType.subsequentGrammar);
      case EventType.ITEM_EO:
        //assert eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO;
        //return EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList);
        throw new UnsupportedOperationException();
      case EventType.ITEM_SO_WC:
        return new EventType(eventType.depth, eventTypeList, EventType.ITEM_SO_WC, eventType.subsequentGrammar);
      default:
        throw new UnsupportedOperationException();
    }
  }
*/  

}
