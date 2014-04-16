using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;

namespace Nagasena.Proc {

  /// <summary>
  /// Developers have the option of implementing the EXISchemaResolver interface
  /// to help EXIReader and EXIDecoder locate the correct grammar cache for parsing 
  /// an EXI stream.
  /// </summary>
  public interface EXISchemaResolver {

    /// <summary>
    /// Return a GrammarCache based on a schemaId and grammar options discovered 
    /// in the header options of an EXI stream. </summary>
    /// <param name="schemaId"> the specific schema used to decode an EXI stream </param>
    /// <param name="grammarOptions"> the specific grammar options used to decode an EXI stream </param>
    /// <returns> a GrammarCache object </returns>
    GrammarCache resolveSchema(string schemaId, short grammarOptions);

  }

}