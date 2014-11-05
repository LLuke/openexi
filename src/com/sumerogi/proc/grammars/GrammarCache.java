package com.sumerogi.proc.grammars;

//import com.sumerogi.proc.common.EventType;

/**
 * A GrammarCache object represents a set of EXI grammars used 
 * for processing EXI streams using specific grammar options. 
 * The GrammarCache is passed as an argument to 
 * the EXIReader and Transmogrifier prior to processing an EXI stream.
 */
public final class GrammarCache {//implements IGrammarCache {

  private final DocumentGrammar m_documentGrammar;
//  private final Grammar m_fragmentGrammar;

//  /**
//   * Short integer that encapsulates {@link com.sumerogi.proc.common.GrammarOptions}
//   * for the EXI stream.
//   */
//  public final short grammarOptions;
  
//  private final BuiltinObjectGrammar m_builtinElementGrammarTemplate; 
  
//  final BuiltinObjectGrammar createBuiltinObjectGrammar(final EventType[] eventTypes) {
//    return m_builtinElementGrammarTemplate.duplicate(eventTypes);
//  }

  /**
   * Creates an instance of GrammarCache informed by a schema with the
   * specified grammar options.
   * @param EXISchema compiled schema
   * @param grammarOptions integer value that represents a grammar option configuration
   */
  public GrammarCache() {
    m_documentGrammar = new DocumentGrammar(this);
//    m_builtinElementGrammarTemplate = new BuiltinObjectGrammar(this);
  }

  /**
   * Returns DocumentGrammar.
   * @y.exclude
   */
  public Grammar getDocumentGrammar() {
    return m_documentGrammar;
  }

  ///////////////////////////////////////////////////////////////////////////
  // IGrammarCache implementation
  ///////////////////////////////////////////////////////////////////////////

//  /**
//   * Gets the compiled EXI Schema.
//   * @return an EXI schema.
//   */
//  public EXISchema getEXISchema() {
//    return m_schema;
//  }
//
//  /** @y.exclude */
//  public IGrammar getElementGrammarUse(int elem) {
//    return exiGrammarUses[m_schema.getSerialOfElem(elem)];
//  }

}
