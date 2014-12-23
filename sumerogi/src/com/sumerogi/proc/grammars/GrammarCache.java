package com.sumerogi.proc.grammars;

/**
 * A GrammarCache object represents a set of EXI grammars used 
 * for processing EXI streams using specific grammar options. 
 * The GrammarCache is passed as an argument to 
 * the EXIReader and Transmogrifier prior to processing an EXI stream.
 */
public final class GrammarCache {

  private final DocumentGrammar m_documentGrammar;

  /**
   * Creates an instance of GrammarCache informed by a schema with the
   * specified grammar options.
   * @param EXISchema compiled schema
   * @param grammarOptions integer value that represents a grammar option configuration
   */
  public GrammarCache() {
    m_documentGrammar = new DocumentGrammar(this);
  }

  /**
   * Returns DocumentGrammar.
   * @y.exclude
   */
  public Grammar getDocumentGrammar() {
    return m_documentGrammar;
  }

}
