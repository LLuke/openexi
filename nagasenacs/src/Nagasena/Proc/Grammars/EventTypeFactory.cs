using EventCode = Nagasena.Proc.Common.EventCode;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using IGrammar = Nagasena.Proc.Common.IGrammar;

namespace Nagasena.Proc.Grammars {

  internal class EventTypeFactory {

    private EventTypeFactory() {
    }

    internal static EventType creatEndElement(sbyte depth, EventTypeList eventTypeList) {
      return new EventType((string)null, (string)null, -1, -1, depth, eventTypeList, EventType.ITEM_EE, EventDescription_Fields.EVENT_EE, (IGrammar)null);
    }

    internal static EventType createEndDocument(EventTypeList eventTypeList) {
      return new EventType((string)null, (string)null, -1, -1, EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_ED, EventDescription_Fields.EVENT_ED, (IGrammar)null);
    }

    internal static EventType createStartDocument(EventTypeList eventTypeList) {
      return new EventType((string)null, (string)null, -1, -1, EventCode.EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SD, EventDescription_Fields.EVENT_SD, (IGrammar)null);
    }

    internal static EventTypeElement createStartElement(int uriId, int localNameId, string uri, string localName, 
      EventTypeList eventTypeList, EXIGrammarUse ensuingGrammar) {
        return new EventTypeElement(uriId, uri, localNameId, localName, eventTypeList, ensuingGrammar, (EXIGrammar)null);
    }

  }

}