package org.openexi.proc;

import org.openexi.schema.EXISchema;

public interface EXISchemaResolver {

  public EXISchema resolveSchema(String schemaId);
  
}
