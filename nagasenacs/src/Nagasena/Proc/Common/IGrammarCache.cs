using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.Common {

  /// <exclude/>
  public interface IGrammarCache {

    EXISchema EXISchema { get; }

    IGrammar getElementGrammarUse(int elem);

  }

}