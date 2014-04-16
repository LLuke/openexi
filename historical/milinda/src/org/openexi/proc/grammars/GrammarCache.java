package org.openexi.proc.grammars;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaLayout;

public class GrammarCache {

  private final EXISchema m_schemaCorpus;
  private final int[] m_nodes;

  private final DocumentGrammar m_documentGrammar;
  private final Grammar m_fragmentGrammar;
  private final ElementFragmentGrammar m_elementFragmentGrammar;
  private final SchemaInformedElementGrammar[] m_elementGrammars;
  private final ElementTagGrammar[] m_elementTagGrammars;
  private final ContentGrammar[] m_elementContentGrammars;
  
  private final EmptyContentGrammar m_emptyContentGrammar;
  
  private final boolean m_disableCache;
  private final boolean m_checkSanity;
  
  // Grammar options
  public final short grammarOptions;
  
  private final BuiltinElementGrammar m_builtinElementGrammarTemplate; 
  
  final BuiltinElementGrammar createBuiltinElementGrammar(final String uri, final EventTypeNonSchema[] eventTypes) {
    return m_builtinElementGrammarTemplate.duplicate(uri, eventTypes);
  }

  /**
   * Creates an instance of GrammarCache.
   * @param EXISchema Compiled schema
   */
  public GrammarCache(EXISchema schemaCorpus) {
    this(schemaCorpus, false, GrammarOptions.OPTIONS_UNUSED);
  }

  /**
   * Creates an instance of GrammarCache.
   * @param options an int value that represents a grammar option configuration
   */
  public GrammarCache(short grammarOptions) {
    this(null, false, grammarOptions);
  }

  /**
   * Creates an instance of GrammarCache.
   * @param EXISchema Compiled schema
   * @param options an int value that represents a grammar option configuration
   */
  public GrammarCache(EXISchema schemaCorpus, short grammarOptions) {
    this(schemaCorpus, false, grammarOptions);
  }

  /**
   * Creates an instance of SchemaGrammarCache.
   * @param EXISchema Compiled schema
   */
  public GrammarCache(EXISchema schemaCorpus, boolean disableCache, short grammarOptions) {
    m_schemaCorpus = schemaCorpus;
    m_disableCache = disableCache;
    m_checkSanity = m_schemaCorpus != null && !m_schemaCorpus.isUPAGotchaFree();
    this.grammarOptions = grammarOptions;
    m_documentGrammar = new DocumentGrammar(this);
    if (m_schemaCorpus != null) {
      m_nodes = m_schemaCorpus.getNodes();
      m_elementGrammars = new SchemaInformedElementGrammar[schemaCorpus.getTotalElemCount()];
      m_elementTagGrammars = new ElementTagGrammar[schemaCorpus.getTotalTypeCount()];
      m_elementContentGrammars = new ContentGrammar[schemaCorpus.getTotalTypeCount()];
      m_emptyContentGrammar = new EmptyContentGrammar(this);
      m_elementFragmentGrammar = new ElementFragmentGrammar(this);
      m_fragmentGrammar = new FragmentGrammar(this);
    }
    else {
      m_nodes = null;
      m_elementGrammars = null;
      m_elementTagGrammars = null;
      m_elementContentGrammars = null;
      m_emptyContentGrammar = null;
      m_elementFragmentGrammar = null;
      m_fragmentGrammar = new BuiltinFragmentGrammar(this);
    }
    m_builtinElementGrammarTemplate = new BuiltinElementGrammar("", this);
  }

  /**
   * Returns a DocumentGrammar.
   */
  public Grammar retrieveDocumentGrammar(boolean isFragment, EventTypeNonSchema[] eventTypes) {
    return isFragment ? m_schemaCorpus != null ? m_fragmentGrammar : ((BuiltinFragmentGrammar)m_fragmentGrammar).duplicate(eventTypes) : m_documentGrammar;
  }

  public EXISchema getEXISchema() {
    return m_schemaCorpus;
  }
  
  final SchemaInformedGrammar retrieveElementFragmentGrammar(int elemFragment) {
    if (!m_schemaCorpus.isSpecificINodeInFragment(elemFragment))
      return m_elementFragmentGrammar;
    if (m_disableCache)
      return new SchemaInformedElementGrammar(elemFragment, this);
    final int serial = m_nodes[elemFragment + EXISchemaLayout.ELEM_NUMBER];
    final SchemaInformedElementGrammar elementState;
    if ((elementState = m_elementGrammars[serial]) != null)
      return elementState;
    else
      return m_elementGrammars[serial] = new SchemaInformedElementGrammar(elemFragment, this);
  }
  
  public final SchemaInformedElementGrammar retrieveElementGrammar(int elem) {
    if (m_schemaCorpus != null) {
      if (m_disableCache) {
        return new SchemaInformedElementGrammar(elem, this);
      }
      final int serial = m_nodes[elem + EXISchemaLayout.ELEM_NUMBER];
      final SchemaInformedElementGrammar elementState;
      if ((elementState = m_elementGrammars[serial]) != null)
        return elementState;
      else
        return m_elementGrammars[serial] = new SchemaInformedElementGrammar(elem, this);
    }
    return null;
  }
  
  public final ElementTagGrammar retrieveElementTagGrammar(int tp) {
    if (m_schemaCorpus != null) {
      if (m_disableCache) {
        return new ElementTagGrammar(tp, this);
      }
      final int serial = m_nodes[tp + EXISchemaLayout.TYPE_NUMBER];
      final ElementTagGrammar elementTagState;
      if ((elementTagState = m_elementTagGrammars[serial]) != null)
        return elementTagState;
      else
        return m_elementTagGrammars[serial] = new ElementTagGrammar(tp, this);
    }
    return null;
  }

  final ContentGrammar retrieveElementContentGrammar(int tp) {
    if (m_schemaCorpus != null && tp != EXISchema.NIL_NODE) {
      assert m_nodes[tp] == EXISchema.COMPLEX_TYPE_NODE || m_nodes[tp] == EXISchema.SIMPLE_TYPE_NODE;
      final int serial = m_nodes[tp + EXISchemaLayout.TYPE_NUMBER];
      ContentGrammar elementContentState;
      if ((elementContentState = m_elementContentGrammars[serial]) != null) {
        assert !m_disableCache;
        return elementContentState;
      }
      else {
        if (m_schemaCorpus.getNodeType(tp) == EXISchema.COMPLEX_TYPE_NODE) {
          final int contentClass = m_schemaCorpus.getContentClassOfComplexType(tp);
          switch (contentClass) {
            case EXISchema.CONTENT_ELEMENT_ONLY:
            case EXISchema.CONTENT_MIXED:
              elementContentState = new ComplexContentGrammar(tp, this, m_checkSanity);
              break;
            case EXISchema.CONTENT_SIMPLE:
              int stype = m_schemaCorpus.getSimpleTypeOfComplexType(tp);
              elementContentState = new SimpleContentGrammar(stype, this);
              break;
            case EXISchema.CONTENT_EMPTY:
              elementContentState = m_emptyContentGrammar;
              break;
            default:
              assert false;
              break;
          }
        }
        else {
          elementContentState =  new SimpleContentGrammar(tp, this);
        }
        if (m_disableCache) {
          return elementContentState;
        }
        return m_elementContentGrammars[serial] = elementContentState;
      }
    }
    return null;
  }
  
  public EmptyContentGrammar getEmptyContentGrammar() {
    return m_emptyContentGrammar;
  }

  /**
   * For test purposes only.
   */
  SchemaInformedGrammar getCachedGrammar(short veriety, int i) {
    switch (veriety) {
      case Grammar.SCHEMA_GRAMMAR_ELEMENT_CONTENT:
        return m_elementContentGrammars[i];
      case Grammar.SCHEMA_GRAMMAR_ELEMENT_TAG:
        return m_elementTagGrammars[i];
      case Grammar.SCHEMA_GRAMMAR_ELEMENT:
        return m_elementGrammars[i];
      default:
        return null;
    }
  }

}
