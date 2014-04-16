package org.openexi.proc;

import org.openexi.proc.grammars.GrammarCache;;

/**
 * Developers have the option of implementing the EXISchemaResolver interface
 * to help EXIReader and EXIDecoder locate the correct grammar cache for parsing 
 * an EXI stream.
 */
public interface EXISchemaResolver {
  
  /**
   * Return a GrammarCache based on a schemaId and grammar options discovered 
   * in the header options of an EXI stream.
   * @param schemaId the specific schema used to decode an EXI stream
   * @param grammarOptions the specific grammar options used to decode an EXI stream
   * @return a GrammarCache object
   */
  public GrammarCache resolveSchema(String schemaId, short grammarOptions);
  
}
