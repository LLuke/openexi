package org.openexi.proc.grammars;

import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.IGrammar;
import org.openexi.proc.common.IGrammarCache;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaLayout;

/**
 * A GrammarCache object represents a set of EXI grammars used 
 * for processing EXI streams using specific grammar options. 
 * The GrammarCache is passed as an argument to 
 * the EXIReader and Transmogrifier prior to processing an EXI stream.
 */
public final class GrammarCache implements IGrammarCache {

  private final EXISchema m_schema;

  private final DocumentGrammar m_documentGrammar;
  private final Grammar m_fragmentGrammar;

  final ElementFragmentGrammar elementFragmentGrammar;
  // Type grammars [0 ... #grammars]
  final EXIGrammar[] exiGrammars;
  // Element grammars [0 ... #elements]
  final EXIGrammarUse[] exiGrammarUses;
  
  /**
   * Short integer that encapsulates {@link org.openexi.proc.common.GrammarOptions}
   * for the EXI stream.
   */
  public final short grammarOptions;
  
  private final BuiltinElementGrammar m_builtinElementGrammarTemplate; 
  
  final BuiltinElementGrammar createBuiltinElementGrammar(final String uri, final EventType[] eventTypes) {
    return m_builtinElementGrammarTemplate.duplicate(uri, eventTypes);
  }

  /**
   * Creates an instance of GrammarCache informed by a schema with default 
   * grammar options.
   * @param EXISchema compiled schema
   */
  public GrammarCache(EXISchema schema) {
    this(schema, GrammarOptions.DEFAULT_OPTIONS);
  }

  /**
   * Creates an instance of GrammarCache with the specified grammar options.
   * @param grammarOptions integer value that represents a grammar option configuration
   */
  public GrammarCache(short grammarOptions) {
    this((EXISchema)null, grammarOptions);
  }

  /**
   * Creates an instance of GrammarCache informed by a schema with the
   * specified grammar options.
   * @param EXISchema compiled schema
   * @param grammarOptions integer value that represents a grammar option configuration
   */
  public GrammarCache(EXISchema schema, short grammarOptions) {
    m_schema = schema;
    this.grammarOptions = grammarOptions;
    if (m_schema != null) {
      final int n_grammars = schema.getTotalGrammarCount();
      exiGrammars = new EXIGrammar[n_grammars];
      final int[] elems = m_schema.getElems();
      exiGrammarUses = new EXIGrammarUse[schema.getElemCountOfSchema()];
      int i, elem;
      for (i = elem = 0; elem < elems.length; elem += EXISchemaLayout.SZ_ELEM, i++) {
        final int tp = schema.getTypeOfElem(elem);
        final int contentDatatype = schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
        exiGrammarUses[i] = new EXIGrammarUse(contentDatatype, this);
      }
      final int[] grammars = m_schema.getGrammars();
      for (int gram = 0; gram < grammars.length; gram += EXISchema.getSizeOfGrammar(gram, grammars)) {
        final int serial = schema.getSerialOfGrammar(gram);
        assert exiGrammars[serial] == null;
        exiGrammars[serial] = new EXIGrammar(this);
      }
      for (int gram = 0; gram < grammars.length; gram += EXISchema.getSizeOfGrammar(gram, grammars)) {
        final int serial = schema.getSerialOfGrammar(gram);
        exiGrammars[serial].substantiate(gram, false);
      }
      final EXIGrammar[] elementGrammars;
      elementGrammars = grammarOptions == GrammarOptions.STRICT_OPTIONS ? new EXIGrammar[3 * n_grammars] : null;
      for (i = elem = 0; elem < elems.length; elem += EXISchemaLayout.SZ_ELEM, i++) {
        if (grammarOptions == GrammarOptions.STRICT_OPTIONS) {
          int nillableTypable = 0;
          if (schema.isNillableElement(elem))
            nillableTypable |= 0x01;
          if (schema.isTypableType(schema.getTypeOfElem(elem)))
            nillableTypable |= 0x02;
          EXIGrammar exiGrammar; 
          final int gramId = schema.getSerialOfGrammar(schema.getGrammarOfType(schema.getTypeOfElem(elem)));
          if (nillableTypable == 0) { // neither nillable nor typable
            // use type grammar intact.
            exiGrammar = exiGrammars[gramId];
          }
          else {
            final int _base = n_grammars * (nillableTypable - 1);
            final int ind = _base + gramId;
            if ((exiGrammar = elementGrammars[ind]) == null) {
              exiGrammar = new EXIGrammar(this);
              exiGrammar.substantiate(elem, true);
              elementGrammars[ind] = exiGrammar;
            }
          }
          assert exiGrammar != null;
          exiGrammarUses[i].exiGrammar = exiGrammar;
        }
        else {
          assert (grammarOptions & GrammarOptions.STRICT_OPTIONS) == 0;
          final int tp = schema.getTypeOfElem(elem);
          exiGrammarUses[i].exiGrammar = exiGrammars[schema.getSerialOfGrammar(schema.getGrammarOfType(tp))];
        }
      }
      elementFragmentGrammar = new ElementFragmentGrammar(this);
      m_fragmentGrammar = new FragmentGrammar(this);
    }
    else {
      elementFragmentGrammar = null;
      m_fragmentGrammar = new BuiltinFragmentGrammar(this);
      exiGrammars = null;
      exiGrammarUses = null;
    }
    m_documentGrammar = new DocumentGrammar(this);
    m_builtinElementGrammarTemplate = new BuiltinElementGrammar("", this);
  }

  /**
   * Returns one of FragmentGrammar, BuiltinFragmentGrammar or DocumentGrammar.
   * @y.exclude
   */
  public Grammar retrieveRootGrammar(boolean isFragment, EventType[] eventTypes) {
    return isFragment ? m_schema != null ? m_fragmentGrammar : ((BuiltinFragmentGrammar)m_fragmentGrammar).duplicate(eventTypes) : m_documentGrammar;
  }

  /** @y.exclude */
  public EXIGrammar getTypeGrammar(int tp) {
    final int gram = m_schema.getGrammarOfType(tp);
    return exiGrammars[m_schema.getSerialOfGrammar(gram)];
  }

  ///////////////////////////////////////////////////////////////////////////
  // IGrammarCache implementation
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Gets the compiled EXI Schema.
   * @return an EXI schema.
   */
  public EXISchema getEXISchema() {
    return m_schema;
  }

  /** @y.exclude */
  public IGrammar getElementGrammarUse(int elem) {
    return exiGrammarUses[m_schema.getSerialOfElem(elem)];
  }

}
