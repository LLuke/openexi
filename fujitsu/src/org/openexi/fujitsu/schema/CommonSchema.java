package org.openexi.fujitsu.schema;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;

class CommonSchema {

  protected static EXISchema loadCompiledSchema(URL compiledSchemaURI) {
    EXISchema schema = null;
    if (compiledSchemaURI != null) {
      InputStream is = null;
      try {
        is = compiledSchemaURI.openStream();
        ObjectInputStream ois = null;
        try {
          ois = new ObjectInputStream(new BufferedInputStream(is));
          schema = (EXISchema)ois.readObject();
        }
        finally {
          if (ois != null)
            ois.close();
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
