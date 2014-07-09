package org.openexi.tryout;

import org.openexi.schema.EXISchema;

interface IValidationContext {
  
  String getSystemId();
  
  void setDone(EXISchema schema, boolean hasFatalError, int n_errors);

}
