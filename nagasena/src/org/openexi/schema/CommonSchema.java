package org.openexi.schema;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;

abstract class CommonSchema {
  
  private CommonSchema() {
  }

  protected static EXISchema loadCompiledSchema(URL compiledSchemaURI) {
    EXISchema schema = null;
    if (compiledSchemaURI != null) {
      InputStream is = null;
      try {
        is = compiledSchemaURI.openStream();
        DataInputStream ios = null;
        try {
          ios = new DataInputStream(new BufferedInputStream(is));
          schema = EXISchema.readIn(ios);
        }
        finally {
          if (ios != null)
            ios.close();
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      finally {
        try {
          if (is != null)
            is.close();
        }
        catch (Exception e) {
        }
      }
    }
    return schema;
  }

}
