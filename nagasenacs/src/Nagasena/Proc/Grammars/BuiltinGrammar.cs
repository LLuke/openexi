using System;
using System.Diagnostics;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using IGrammar = Nagasena.Proc.Common.IGrammar;

namespace Nagasena.Proc.Grammars {

  /// <exclude/>
  public abstract class BuiltinGrammar : Grammar {

    public const sbyte ELEMENT_STATE_IN_TAG = 0;
    public const sbyte ELEMENT_STATE_IN_CONTENT = 1;
    internal const sbyte ELEMENT_STATE_DELEGATED = 2;

    // N_NONSCHEMA_ITEMS must be ITEM_EE plus 1
    internal static readonly short N_NONSCHEMA_ITEMS = EventType.ITEM_EE + 1;

    protected internal BuiltinGrammar(sbyte grammarType, GrammarCache grammarCache) : base(grammarType, grammarCache) {
    }

    public override sealed bool SchemaInformed {
      get {
        return false;
      }
    }

    internal override sealed void nillify(int eventTypeIndex, GrammarState stateVariables) {
      throw new InvalidOperationException();
    }

    public override sealed void chars(EventType eventType, GrammarState stateVariables) {
      throw new InvalidOperationException("char() cannot be invoked on a built-in element grammar.");
    }

    internal EventType duplicate(EventType eventType, EventTypeList eventTypeList) {
      switch (eventType.itemType) {
        case EventType.ITEM_AT_WC_ANY_UNTYPED:
          return new EventType(eventType.depth, eventTypeList, EventType.ITEM_AT_WC_ANY_UNTYPED, (IGrammar)null);
        case EventType.ITEM_CH:
          return new EventType(eventType.depth, eventTypeList, EventType.ITEM_CH, eventType.subsequentGrammar);
        case EventType.ITEM_CM:
          return new EventType(eventType.depth, eventTypeList, EventType.ITEM_CM, eventType.subsequentGrammar);
        case EventType.ITEM_DTD:
          Debug.Assert(eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO);
          return new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_DTD, (IGrammar)null);
        case EventType.ITEM_EE:
          Debug.Assert(eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO);
          return EventTypeFactory.creatEndElement(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList);
        case EventType.ITEM_ER:
          Debug.Assert(eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO);
          return new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_ER, eventType.subsequentGrammar);
        case EventType.ITEM_NS:
          Debug.Assert(eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO);
          return new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_NS, (IGrammar)null);
        case EventType.ITEM_PI:
          return new EventType(eventType.depth, eventTypeList, EventType.ITEM_PI, eventType.subsequentGrammar);
        case EventType.ITEM_SC:
          Debug.Assert(eventType.depth == EventCode.EVENT_CODE_DEPTH_TWO);
          return new EventType(EventCode.EVENT_CODE_DEPTH_TWO, eventTypeList, EventType.ITEM_SC, (IGrammar)null);
        case EventType.ITEM_SE_WC:
          return new EventType(eventType.depth, eventTypeList, EventType.ITEM_SE_WC, eventType.subsequentGrammar);
        default:
          throw new System.NotSupportedException();
      }
    }


  }

}