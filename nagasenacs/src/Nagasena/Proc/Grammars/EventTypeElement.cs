using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using IGrammar = Nagasena.Proc.Common.IGrammar;

namespace Nagasena.Proc.Grammars {

  internal sealed class EventTypeElement : EventType {

    public readonly Grammar ensuingGrammar;

    internal EventTypeElement(int uriId, string uri, int localNameId, string name, EventTypeList eventTypeList, Grammar ensuingGrammar, IGrammar subsequentGrammar) :
      base(uri, name, uriId, localNameId, EVENT_CODE_DEPTH_ONE, eventTypeList, EventType.ITEM_SE, EventDescription_Fields.EVENT_SE, subsequentGrammar) {
      this.ensuingGrammar = ensuingGrammar;
    }

  }

}