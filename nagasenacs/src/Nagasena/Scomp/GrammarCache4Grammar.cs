namespace Nagasena.Scomp {

  using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
  using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
  using GrammarSchema = Nagasena.Schema.GrammarSchema;

  /// <exclude/>
  public sealed class GrammarCache4Grammar {

    public static readonly GrammarCache m_grammarCache;
    static GrammarCache4Grammar() {
      m_grammarCache = new GrammarCache(GrammarSchema.EXISchema, GrammarOptions.STRICT_OPTIONS);
    }

    public static GrammarCache GrammarCache {
      get {
        return m_grammarCache;
      }
    }

    private GrammarCache4Grammar() {
    }

  }

}