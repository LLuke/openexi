package org.openexi.sax;

import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

import org.openexi.proc.grammars.GrammarCache;

/**
 * Applications can directly feed SAX events into a transmogrifier through SAXTransmogrifier.
 */
public interface SAXTransmogrifier extends ContentHandler, LexicalHandler, BinaryDataHandler {

  /**
   * Returns the GrammarCache that is in use by this SAXTransmogrifier.
   * @return a GrammarCache
   */
  public GrammarCache getGrammarCache();
  
}
