package org.openexi.proc.grammars;

import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.StringTable;
import org.openexi.schema.EXISchema;

/**
 * CommonState is common base of those classes that represent
 * grammars such as documents, elements, groups and so on.
 */
public abstract class Grammar {

  public static final byte BUILTIN_GRAMMAR_ELEMENT                = 0;
  public static final byte BUILTIN_GRAMMAR_FRAGMENT               = 1;
  public static final byte SCHEMA_GRAMMAR_DOCUMENT                = 2;
  public static final byte SCHEMA_GRAMMAR_FRAGMENT                = 3;
  public static final byte SCHEMA_GRAMMAR_ELEMENT_FRAGMENT        = 4;
  public static final byte SCHEMA_GRAMMAR_ELEMENT_AND_TYPE        = 5;
  public static final byte SCHEMA_GRAMMAR_ELEMENT_AND_TYPE_USE    = 6;

  private static final byte DOCUMENT_STATE_BASE = 0;
  protected static final byte DOCUMENT_STATE_CREATED     = DOCUMENT_STATE_BASE;
  protected static final byte DOCUMENT_STATE_DEPLETE     = DOCUMENT_STATE_CREATED + 1;
  public static final byte DOCUMENT_STATE_COMPLETED      = DOCUMENT_STATE_DEPLETE + 1;
  public static final byte DOCUMENT_STATE_END            = DOCUMENT_STATE_COMPLETED + 1;

  ///////////////////////////////////////////////////////////////////////////
  /// immutables (Do not reset immutables!)
  ///////////////////////////////////////////////////////////////////////////

  public final byte grammarType;

  protected final GrammarCache m_grammarCache;
  protected final EXISchema schema;

  ///////////////////////////////////////////////////////////////////////////
  /// Constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  protected Grammar(byte grammarType, GrammarCache grammarCache) {
    // immutables
    this.grammarType = grammarType;
    m_grammarCache = grammarCache;
    schema = m_grammarCache.getEXISchema();
  }

  /**
   * Implementation of init at least needs to do this:
   * stateVariables.targetGrammar = this;
   */
  public abstract void init(GrammarState stateVariables);

  public abstract boolean isSchemaInformed();
  
  ///////////////////////////////////////////////////////////////////////////
  /// Method declarations for event processing
  ///////////////////////////////////////////////////////////////////////////

  abstract EventTypeList getNextEventTypes(GrammarState stateVariables);
  
  abstract EventCodeTuple getNextEventCodes(GrammarState stateVariables);

  void attribute(EventType eventType, GrammarState stateVariables) {
  }

  public void wildcardAttribute(int eventTypeIndex, int uri, int name, GrammarState stateVariables) {
  }

  public abstract void element(EventType eventType, GrammarState stateVariables);

  Grammar wildcardElement(int eventTypeIndex, int uri, int name, final GrammarState stateVariables) {
    final GrammarState kid = stateVariables.apparatus.pushState();
    final StringTable stringTable = stateVariables.apparatus.stringTable;
    final StringTable.LocalNamePartition localNamePartition;
    localNamePartition = stringTable.getLocalNamePartition(uri);
    final Grammar elementGrammar;
    if ((elementGrammar = (Grammar)localNamePartition.localNameEntries[name].grammar) != null) {
      elementGrammar.init(kid);
      return elementGrammar;
    }
    else {
      final BuiltinElementGrammar builtinElementGrammar;
      builtinElementGrammar = m_grammarCache.createBuiltinElementGrammar(stringTable.getURI(uri), stateVariables.apparatus.eventTypesWorkSpace);
      builtinElementGrammar.localNamePartition = localNamePartition;
      localNamePartition.setGrammar(name, builtinElementGrammar);
      builtinElementGrammar.init(kid);
      return builtinElementGrammar;
    }
  }

  /**
   * Signals xsi:type event.
   */
  abstract void xsitp(int tp, GrammarState stateVariables);

  /**
   * Signals xsi:nil event.
   */
  abstract void nillify(int eventTypeIndex, GrammarState stateVariables);
  
  public abstract void chars(EventType eventType, GrammarState stateVariables);

  public abstract void undeclaredChars(int eventTypeIndex, GrammarState stateVariables);

  /**
   * Signals CM, PI or ER event. 
   */
  public abstract void miscContent(int eventTypeIndex, GrammarState stateVariables);

  /**
   * Signals the end of an element.
   */
  public abstract void end(GrammarState stateVariables);

  public void startDocument(GrammarState stateVariables) {
    assert false;
  }

  public void endDocument(GrammarState stateVariables) {
    assert false;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Utility implementations
  ///////////////////////////////////////////////////////////////////////////

  protected final EXIGrammar retrieveEXIGrammar(int gram) {
    if (gram != EXISchema.NIL_GRAM) {
      final int gramSerial = schema.getSerialOfGrammar(gram);
      EXIGrammar grammar;
      if ((grammar = m_grammarCache.exiGrammars[gramSerial]) != null)
        return grammar;
      else { 
        grammar = new EXIGrammar(m_grammarCache);
        grammar.substantiate(gram, false);
        return grammar;
      }
    }
    return null;
  }

}
