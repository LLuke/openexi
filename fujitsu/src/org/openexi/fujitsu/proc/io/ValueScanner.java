package org.openexi.fujitsu.proc.io;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.schema.EXISchema;

public abstract class ValueScanner extends ValueApparatus {
  
  protected InputStream m_istream;

  protected ValueScanner() {
    m_istream = null;
  }
  
  public int getBuiltinRCS(int simpleType) {
    return EXISchema.NIL_NODE;
  }
  
  public final void setInputStream(InputStream istream) {
    m_istream = istream;
  }
  
  public abstract CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException;

}
