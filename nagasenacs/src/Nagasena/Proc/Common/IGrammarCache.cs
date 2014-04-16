using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.Common {

  public interface IGrammarCache {

    EXISchema EXISchema { get; }

    IGrammar getElementGrammarUse(int elem);

  }

}