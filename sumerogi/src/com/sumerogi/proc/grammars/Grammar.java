package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.common.StringTable.LocalNameEntry;

/**
 * CommonState is common base of those classes that represent
 * grammars such as documents, elements, groups and so on.
 */
public abstract class Grammar {

  public static final byte BUILTIN_GRAMMAR_OBJECT                 = 0;
  public static final byte BUILTIN_GRAMMAR_ARRAY                  = 1;
  public static final byte SCHEMA_GRAMMAR_DOCUMENT                = 2;

//  private static final byte DOCUMENT_STATE_BASE = 0;
//  protected static final byte DOCUMENT_STATE_CREATED     = DOCUMENT_STATE_BASE;
//  protected static final byte DOCUMENT_STATE_DEPLETE     = DOCUMENT_STATE_CREATED + 1;
//  public static final byte DOCUMENT_STATE_COMPLETED      = DOCUMENT_STATE_DEPLETE + 1;
//  public static final byte DOCUMENT_STATE_END            = DOCUMENT_STATE_COMPLETED + 1;

  ///////////////////////////////////////////////////////////////////////////
  /// immutables (Do not reset immutables!)
  ///////////////////////////////////////////////////////////////////////////

  public final byte grammarType;

  protected final GrammarCache m_grammarCache;

  ///////////////////////////////////////////////////////////////////////////
  /// Constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  protected Grammar(byte grammarType, GrammarCache grammarCache) {
    // immutables
    this.grammarType = grammarType;
    m_grammarCache = grammarCache;
  }

  /**
   * Implementation of init at least needs to do this:
   * stateVariables.targetGrammar = this;
   */
  public abstract void init(GrammarState stateVariables);

  ///////////////////////////////////////////////////////////////////////////
  /// Method declarations for event processing
  ///////////////////////////////////////////////////////////////////////////

  abstract EventTypeList getNextEventTypes(GrammarState stateVariables);
  
  abstract EventCodeTuple getNextEventCodes(GrammarState stateVariables);
  
  public abstract void anonymousStringValue(EventType eventType, GrammarState stateVariables);
  public abstract void wildcardStringValue(int eventTypeIndex, int nameId);

  public abstract void anonymousNumberValue(EventType eventType, GrammarState stateVariables);
  public abstract void wildcardNumberValue(int eventTypeIndex, int nameId);

  public abstract void anonymousNullValue(EventType eventType, GrammarState stateVariables);

  public abstract void anonymousBooleanValue(EventType eventType, GrammarState stateVariables);

  public abstract void wildcardBooleanValue(int eventTypeIndex, int nameId);
  public abstract void wildcardNullValue(int eventTypeIndex, int nameId);

  public abstract void startObjectNamed(EventType eventType, GrammarState stateVariables);

  public Grammar startObjectAnonymous(EventType eventType, GrammarState stateVariables) {
    final GrammarState kid = stateVariables.apparatus.pushState();
    int name = kid.name = stateVariables.name;
    int distance = kid.distance = stateVariables.distance + 1;
    final StringTable stringTable = stateVariables.apparatus.stringTable;
    final Grammar objectGrammar;
    LocalNameEntry localNameEntry;
    if (name == StringTable.NAME_DOCUMENT) {
      localNameEntry = stringTable.documentLocalNameEntry;
    }
    else
      localNameEntry = stringTable.localNameEntries[name];
    if ((objectGrammar = (Grammar)localNameEntry.objectGrammars[distance]) != null) {
      objectGrammar.init(kid);
      return objectGrammar;
    }
    else {
      final BuiltinObjectGrammar builtinObjectGrammar;
      builtinObjectGrammar = new BuiltinObjectGrammar(m_grammarCache); 
      builtinObjectGrammar.stringTable = stringTable;
      stringTable.setObjectGrammar(name, distance, builtinObjectGrammar);
      builtinObjectGrammar.init(kid);
      return builtinObjectGrammar;
    }
  }

  abstract Grammar startObjectWildcard(int name, final GrammarState stateVariables);

  public abstract void startArrayNamed(EventType eventType, GrammarState stateVariables);

  public Grammar startArrayAnonymous(final GrammarState stateVariables) {
    final GrammarState kid = stateVariables.apparatus.pushState();
    int name = kid.name = stateVariables.name;
    int distance = kid.distance = stateVariables.distance + 1;
    final StringTable stringTable = stateVariables.apparatus.stringTable;
    final Grammar arrayGrammar;
    LocalNameEntry localNameEntry;
    if (name == StringTable.NAME_DOCUMENT) {
      localNameEntry = stringTable.documentLocalNameEntry;
    }
    else
      localNameEntry = stringTable.localNameEntries[name];
    if ((arrayGrammar = (Grammar)localNameEntry.arrayGrammars[distance]) != null) {
      arrayGrammar.init(kid);
      return arrayGrammar;
    }
    else {
      final BuiltinArrayGrammar builtinArrayGrammar;
      builtinArrayGrammar = new BuiltinArrayGrammar(m_grammarCache); 
      builtinArrayGrammar.stringTable = stringTable;
      stringTable.setArrayGrammar(name, distance, builtinArrayGrammar);
      builtinArrayGrammar.init(kid);
      return builtinArrayGrammar;
    }
  }

  abstract Grammar startArrayWildcard(int name, final GrammarState stateVariables);

//  Grammar startObjectWildcard(int name, final GrammarState stateVariables) {
//    final GrammarState kid = stateVariables.apparatus.pushState();
//    kid.name = name;
//    kid.distance = 0;
//    final StringTable stringTable = stateVariables.apparatus.stringTable;
//    final Grammar objectGrammar;
//    if ((objectGrammar = (Grammar)stringTable.localNameEntries[name].objectGrammars[0]) != null) {
//      objectGrammar.init(kid);
//      return objectGrammar;
//    }
//    else {
//      final BuiltinObjectGrammar builtinObjectGrammar;
//      builtinObjectGrammar = new BuiltinObjectGrammar(m_grammarCache); 
//      builtinObjectGrammar.stringTable = stringTable;
//      stringTable.setObjectGrammar(name, 0, builtinObjectGrammar);
//      builtinObjectGrammar.init(kid);
//      return builtinObjectGrammar;
//    }
//  }

  /**
   * Signals the end of an object.
   */
  public abstract void endObject(GrammarState stateVariables);

  /**
   * Signals the end of an array.
   */
  public abstract void endArray(GrammarState stateVariables);

  public void startDocument(GrammarState stateVariables) {
    assert false;
  }

  public void endDocument(GrammarState stateVariables) {
    assert false;
  }
  
}
