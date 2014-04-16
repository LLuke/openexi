package org.openexi.fujitsu.proc;

import org.openexi.fujitsu.schema.EXISchema;

public interface EXISchemaResolver {

  public EXISchema resolveSchema(String schemaId);
  
}
