namespace Nagasena.Proc.Grammars {

  using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
  using EXISchema = Nagasena.Schema.EXISchema;
  using HeaderOptionsSchema = Nagasena.Schema.HeaderOptionsSchema;

  public sealed class OptionsGrammarCache {

    private static readonly GrammarCache m_grammarCache;
    static OptionsGrammarCache() {
      EXISchema schema = HeaderOptionsSchema.EXISchema;
      m_grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);
    }

    private OptionsGrammarCache() {
    }

    public static GrammarCache GrammarCache {
      get {
        return m_grammarCache;
      }
    }

  }

}