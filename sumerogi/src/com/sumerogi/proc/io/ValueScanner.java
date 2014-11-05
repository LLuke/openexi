package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.InputStream;

import com.sumerogi.proc.grammars.ValueApparatus;
import com.sumerogi.schema.Characters;

public abstract class ValueScanner extends ValueApparatus {
  
  protected InputStream m_istream;

  protected ValueScanner() {
    m_istream = null;
  }
  
  public abstract void setInputStream(InputStream istream);
  
  public abstract Characters scan(int name) throws IOException;

}
