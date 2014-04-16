package org.openexi.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.proc.grammars.ValueApparatus;
import org.openexi.schema.Characters;

public abstract class ValueScanner extends ValueApparatus {
  
  protected InputStream m_istream;

  protected ValueScanner() {
    m_istream = null;
  }
  
  public abstract int getBuiltinRCS(int simpleType);
  
  public abstract void setInputStream(InputStream istream);
  
  public abstract Characters scan(int name, int uri, int tp) throws IOException;

}
