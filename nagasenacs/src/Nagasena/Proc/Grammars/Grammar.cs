using System.Diagnostics;

using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using StringTable = Nagasena.Proc.Common.StringTable;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.Grammars {

  /// <summary>
  /// CommonState is common base of those classes that represent
  /// grammars such as documents, elements, groups and so on.
  /// </summary>
  /// <exclude/>
  public abstract class Grammar {

    public const sbyte BUILTIN_GRAMMAR_ELEMENT = 0;
    public const sbyte BUILTIN_GRAMMAR_FRAGMENT = 1;
    public const sbyte SCHEMA_GRAMMAR_DOCUMENT = 2;
    public const sbyte SCHEMA_GRAMMAR_FRAGMENT = 3;
    public const sbyte SCHEMA_GRAMMAR_ELEMENT_FRAGMENT = 4;
    public const sbyte SCHEMA_GRAMMAR_ELEMENT_AND_TYPE = 5;
    public const sbyte SCHEMA_GRAMMAR_ELEMENT_AND_TYPE_USE = 6;

    private const sbyte DOCUMENT_STATE_BASE = 0;
    protected internal const sbyte DOCUMENT_STATE_CREATED = DOCUMENT_STATE_BASE;
    protected internal const sbyte DOCUMENT_STATE_DEPLETE = DOCUMENT_STATE_CREATED + 1;
    public const sbyte DOCUMENT_STATE_COMPLETED = DOCUMENT_STATE_DEPLETE + 1;
    public const sbyte DOCUMENT_STATE_END = DOCUMENT_STATE_COMPLETED + 1;

    ///////////////////////////////////////////////////////////////////////////
    /// immutables (Do not reset immutables!)
    ///////////////////////////////////////////////////////////////////////////

    public readonly sbyte grammarType;

    protected internal readonly GrammarCache m_grammarCache;
    protected internal readonly EXISchema schema;

    ///////////////////////////////////////////////////////////////////////////
    /// Constructors, initializers
    ///////////////////////////////////////////////////////////////////////////

    protected internal Grammar(sbyte grammarType, GrammarCache grammarCache) {
      // immutables
      this.grammarType = grammarType;
      m_grammarCache = grammarCache;
      schema = m_grammarCache.EXISchema;
    }

    /// <summary>
    /// Implementation of init at least needs to do this:
    /// stateVariables.targetGrammar = this;
    /// </summary>
    public abstract void init(GrammarState stateVariables);

    public abstract bool SchemaInformed { get; }

    ///////////////////////////////////////////////////////////////////////////
    /// Method declarations for event processing
    ///////////////////////////////////////////////////////////////////////////

    internal abstract EventTypeList getNextEventTypes(GrammarState stateVariables);

    internal abstract EventCodeTuple getNextEventCodes(GrammarState stateVariables);

    internal virtual void attribute(EventType eventType, GrammarState stateVariables) {
    }

    public virtual void wildcardAttribute(int eventTypeIndex, int uri, int name, GrammarState stateVariables) {
    }

    public abstract void element(EventType eventType, GrammarState stateVariables);

    internal virtual Grammar wildcardElement(int eventTypeIndex, int uri, int name, GrammarState stateVariables) {
      GrammarState kid = stateVariables.apparatus.pushState();
      StringTable stringTable = stateVariables.apparatus.stringTable;
      StringTable.LocalNamePartition localNamePartition;
      localNamePartition = stringTable.getLocalNamePartition(uri);
      Grammar elementGrammar;
      if ((elementGrammar = (Grammar)localNamePartition.localNameEntries[name].grammar) != null) {
        elementGrammar.init(kid);
        return elementGrammar;
      }
      else {
        BuiltinElementGrammar builtinElementGrammar;
        builtinElementGrammar = m_grammarCache.createBuiltinElementGrammar(stringTable.getURI(uri), stateVariables.apparatus.eventTypesWorkSpace);
        builtinElementGrammar.localNamePartition = localNamePartition;
        localNamePartition.setGrammar(name, builtinElementGrammar);
        builtinElementGrammar.init(kid);
        return builtinElementGrammar;
      }
    }

    /// <summary>
    /// Signals xsi:type event.
    /// </summary>
    internal abstract void xsitp(int tp, GrammarState stateVariables);

    /// <summary>
    /// Signals xsi:nil event.
    /// </summary>
    internal abstract void nillify(int eventTypeIndex, GrammarState stateVariables);

    public abstract void chars(EventType eventType, GrammarState stateVariables);

    public abstract void undeclaredChars(int eventTypeIndex, GrammarState stateVariables);

    /// <summary>
    /// Signals CM, PI or ER event. 
    /// </summary>
    public abstract void miscContent(int eventTypeIndex, GrammarState stateVariables);

    /// <summary>
    /// Signals the end of an element.
    /// </summary>
    public abstract void end(GrammarState stateVariables);

    public virtual void startDocument(GrammarState stateVariables) {
      Debug.Assert(false);
    }

    public virtual void endDocument(GrammarState stateVariables) {
      Debug.Assert(false);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Utility implementations
    ///////////////////////////////////////////////////////////////////////////

    internal EXIGrammar retrieveEXIGrammar(int gram) {
      if (gram != EXISchema.NIL_GRAM) {
        int gramSerial = schema.getSerialOfGrammar(gram);
        EXIGrammar grammar;
        if ((grammar = m_grammarCache.exiGrammars[gramSerial]) != null) {
          return grammar;
        }
        else {
          grammar = new EXIGrammar(m_grammarCache);
          grammar.substantiate(gram, false);
          return grammar;
        }
      }
      return null;
    }

  }

}