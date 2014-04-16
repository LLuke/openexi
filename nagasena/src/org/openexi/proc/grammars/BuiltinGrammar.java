package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.IGrammar;

public abstract class BuiltinGrammar extends Grammar {

  public static final byte ELEMENT_STATE_IN_TAG     = 0;
  public static final byte ELEMENT_STATE_IN_CONTENT = 1;
  static final byte ELEMENT_STATE_DELEGATED  = 2;
  
  // N_NONSCHEMA_ITEMS must be ITEM_EE plus 1
  static final short N_NONSCHEMA_ITEMS = EventType.ITEM_EE + 1; 
  
  protected BuiltinGrammar(byte grammarType, GrammarCache grammarCache) {
    super(grammarType, grammarCache);
  }

  @Override
  public final boolean isSchemaInformed() {
    return false;
  }

  @Override
  final void nillify(int eventTypeIndex, GrammarState stateVariables) {
    throw new IllegalStateException();
  }

  @Override
  public final void chars(EventType eventType, GrammarState stateVariables) {
    throw new IllegalStateException("char() cannot be invoked on a built-in element grammar.");
  }
  
  public final EventType duplicate(EventType eventType, EventTypeList eventTypeList) {
    switch (eventType.itemType) {
      case EventType.ITEM_AT_WC_ANY_UNTYPED:
        return new EventType(eventType.depth, eventTypeList, EventType.ITEM_AT_WC_ANY_UNTYPED, (IGrammar)null);
      case EventType.ITEM_CH:
        return new EventType(eventType.depth, eventTypeList, EventType.ITEM_CH, eventType.subsequentGrammar);
      case EventType.ITEM_CM:
        return new EventType(eventType.depth, eventTypeList, EventType.ITEM_CM, eventType.subsequentGrammar);
      case EventType.ITEM_DTD:
        assert eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO;
        return new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_DTD, (IGrammar)null);
      case EventType.ITEM_EE:
        assert eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO;
        return EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList);
      case EventType.ITEM_ER:
        assert eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO;
        return new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_ER, eventType.subsequentGrammar);
      case EventType.ITEM_NS:
        assert eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO;
        return new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_NS, (IGrammar)null);
      case EventType.ITEM_PI:
        return new EventType(eventType.depth, eventTypeList, EventType.ITEM_PI, eventType.subsequentGrammar);
      case EventType.ITEM_SC:
        assert eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO;
        return new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SC, (IGrammar)null);
      case EventType.ITEM_SE_WC:
        return new EventType(eventType.depth, eventTypeList, EventType.ITEM_SE_WC, eventType.subsequentGrammar);
      default:
        throw new UnsupportedOperationException();
    }
  }
  

}
