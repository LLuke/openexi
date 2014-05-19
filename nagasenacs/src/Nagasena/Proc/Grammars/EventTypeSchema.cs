using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;

namespace Nagasena.Proc.Grammars {

  /// <exclude/>
  public sealed class EventTypeSchema : EventType {

    public readonly int nd;

    internal EventTypeSchema(int nd, string uri, string name, int uriId, int nameId, sbyte depth, EventTypeList eventTypeList, sbyte itemType, EXIGrammar subsequentGrammar) :
      base(uri, name, uriId, nameId, depth, eventTypeList, itemType, EventDescription_Fields.NOT_AN_EVENT, subsequentGrammar) {
      this.nd = nd;
    }

  }

}