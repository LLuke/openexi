using System.Diagnostics;

using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using IGrammar = Nagasena.Proc.Common.IGrammar;

namespace Nagasena.Proc.Grammars {

  internal sealed class EXIGrammarUse : Grammar, IGrammar {

    internal EXIGrammar exiGrammar;
    internal readonly int contentDatatype;

    internal EXIGrammarUse(int contentDatatype, GrammarCache grammarCache) : base(SCHEMA_GRAMMAR_ELEMENT_AND_TYPE_USE, grammarCache) {
      this.contentDatatype = contentDatatype;
    }

    public override void init(GrammarState stateVariables) {
      exiGrammar.init(stateVariables);
      stateVariables.contentDatatype = contentDatatype;
    }

    public override bool SchemaInformed {
      get {
        Debug.Assert(false);
        return true;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Implementation of IGrammar (used by StringTable)
    ///////////////////////////////////////////////////////////////////////////

    public void reset() {
      Debug.Assert(false);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Implementation of abstract methods
    ///////////////////////////////////////////////////////////////////////////

    internal override void attribute(EventType eventType, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    internal override EventTypeList getNextEventTypes(GrammarState stateVariables) {
      Debug.Assert(false);
      return null;
    }

    internal override EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
      Debug.Assert(false);
      return null;
    }

    public override void element(EventType eventType, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    internal override Grammar wildcardElement(int eventTypeIndex, int uriId, int localNameId, GrammarState stateVariables) {
      Debug.Assert(false);
      return null;
    }

    internal override void xsitp(int tp, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    internal override void nillify(int eventTypeIndex, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    public override void chars(EventType eventType, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    public override void undeclaredChars(int eventTypeIndex, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    public override void miscContent(int eventTypeIndex, GrammarState stateVariables) {
      Debug.Assert(false);
    }

    public override void end(GrammarState stateVariables) {
      Debug.Assert(false);
    }

  }

}