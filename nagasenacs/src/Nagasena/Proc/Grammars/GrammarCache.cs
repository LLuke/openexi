using System.Diagnostics;

using EventType = Nagasena.Proc.Common.EventType;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;
using IGrammarCache = Nagasena.Proc.Common.IGrammarCache;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaLayout = Nagasena.Schema.EXISchemaLayout;

namespace Nagasena.Proc.Grammars {

  /// <summary>
  /// A GrammarCache object represents a set of EXI grammars used 
  /// for processing EXI streams using specific grammar options. 
  /// The GrammarCache is passed as an argument to 
  /// the EXIReader and Transmogrifier prior to processing an EXI stream.
  /// </summary>
  public sealed class GrammarCache : IGrammarCache {

    private readonly EXISchema m_schema;

    private readonly DocumentGrammar m_documentGrammar;
    private readonly Grammar m_fragmentGrammar;

    internal readonly ElementFragmentGrammar elementFragmentGrammar;
    // Type grammars [0 ... #grammars]
    internal readonly EXIGrammar[] exiGrammars;
    // Element grammars [0 ... #elements]
    internal readonly EXIGrammarUse[] exiGrammarUses;

    /// <summary>
    /// Short integer that encapsulates <seealso cref="Nagasena.Proc.Common.GrammarOptions"/>
    /// for the EXI stream.
    /// </summary>
    public readonly short grammarOptions;

    private readonly BuiltinElementGrammar m_builtinElementGrammarTemplate;

    internal BuiltinElementGrammar createBuiltinElementGrammar(string uri, EventType[] eventTypes) {
      return m_builtinElementGrammarTemplate.duplicate(uri, eventTypes);
    }

    /// <summary>
    /// Creates an instance of GrammarCache informed by a schema with default 
    /// grammar options. </summary>
    /// <param name="EXISchema"> compiled schema </param>
    public GrammarCache(EXISchema schema) : this(schema, GrammarOptions.DEFAULT_OPTIONS) {
    }

    /// <summary>
    /// Creates an instance of GrammarCache with the specified grammar options. </summary>
    /// <param name="grammarOptions"> integer value that represents a grammar option configuration </param>
    public GrammarCache(short grammarOptions) : this((EXISchema)null, grammarOptions) {
    }

    /// <summary>
    /// Creates an instance of GrammarCache informed by a schema with the
    /// specified grammar options. </summary>
    /// <param name="EXISchema"> compiled schema </param>
    /// <param name="grammarOptions"> integer value that represents a grammar option configuration </param>
    public GrammarCache(EXISchema schema, short grammarOptions) {
      m_schema = schema;
      this.grammarOptions = grammarOptions;
      if (m_schema != null) {
        int n_grammars = schema.TotalGrammarCount;
        exiGrammars = new EXIGrammar[n_grammars];
        int[] elems = m_schema.Elems;
        exiGrammarUses = new EXIGrammarUse[schema.ElemCountOfSchema];
        int i, elem;
        for (i = elem = 0; elem < elems.Length; elem += EXISchemaLayout.SZ_ELEM, i++) {
          int tp = schema.getTypeOfElem(elem);
          int contentDatatype = schema.isSimpleType(tp) ? tp : schema.getContentDatatypeOfComplexType(tp);
          exiGrammarUses[i] = new EXIGrammarUse(contentDatatype, this);
        }
        int[] grammars = m_schema.Grammars;
        for (int gram = 0; gram < grammars.Length; gram += EXISchema.getSizeOfGrammar(gram, grammars)) {
          int serial = schema.getSerialOfGrammar(gram);
          Debug.Assert(exiGrammars[serial] == null);
          exiGrammars[serial] = new EXIGrammar(this);
        }
        for (int gram = 0; gram < grammars.Length; gram += EXISchema.getSizeOfGrammar(gram, grammars)) {
          int serial = schema.getSerialOfGrammar(gram);
          exiGrammars[serial].substantiate(gram, false);
        }
        EXIGrammar[] elementGrammars;
        elementGrammars = grammarOptions == GrammarOptions.STRICT_OPTIONS ? new EXIGrammar[3 * n_grammars] : null;
        for (i = elem = 0; elem < elems.Length; elem += EXISchemaLayout.SZ_ELEM, i++) {
          if (grammarOptions == GrammarOptions.STRICT_OPTIONS) {
            int nillableTypable = 0;
            if (schema.isNillableElement(elem)) {
              nillableTypable |= 0x01;
            }
            if (schema.isTypableType(schema.getTypeOfElem(elem))) {
              nillableTypable |= 0x02;
            }
            EXIGrammar exiGrammar;
            int gramId = schema.getSerialOfGrammar(schema.getGrammarOfType(schema.getTypeOfElem(elem)));
            if (nillableTypable == 0) { // neither nillable nor typable
              // use type grammar intact.
              exiGrammar = exiGrammars[gramId];
            }
            else {
              int _base = n_grammars * (nillableTypable - 1);
              int ind = _base + gramId;
              if ((exiGrammar = elementGrammars[ind]) == null) {
                exiGrammar = new EXIGrammar(this);
                exiGrammar.substantiate(elem, true);
                elementGrammars[ind] = exiGrammar;
              }
            }
            Debug.Assert(exiGrammar != null);
            exiGrammarUses[i].exiGrammar = exiGrammar;
          }
          else {
            Debug.Assert((grammarOptions & GrammarOptions.STRICT_OPTIONS) == 0);
            int tp = schema.getTypeOfElem(elem);
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

    /// <summary>
    /// Returns one of FragmentGrammar, BuiltinFragmentGrammar or DocumentGrammar.
    /// @y.exclude
    /// </summary>
    public Grammar retrieveRootGrammar(bool isFragment, EventType[] eventTypes) {
      return isFragment ? m_schema != null ? m_fragmentGrammar : ((BuiltinFragmentGrammar)m_fragmentGrammar).duplicate(eventTypes) : m_documentGrammar;
    }

    internal EXIGrammar getTypeGrammar(int tp) {
      int gram = m_schema.getGrammarOfType(tp);
      return exiGrammars[m_schema.getSerialOfGrammar(gram)];
    }

    ///////////////////////////////////////////////////////////////////////////
    // IGrammarCache implementation
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Gets the compiled EXI Schema. </summary>
    /// <returns> an EXI schema. </returns>
    public EXISchema EXISchema {
      get {
        return m_schema;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public IGrammar getElementGrammarUse(int elem) {
      return exiGrammarUses[m_schema.getSerialOfElem(elem)];
    }

  }

}