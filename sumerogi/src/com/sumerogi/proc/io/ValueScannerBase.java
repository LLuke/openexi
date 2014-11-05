package com.sumerogi.proc.io;

import java.io.InputStream;

abstract class ValueScannerBase extends ValueScanner {
  
  ValueScannerBase() {
  }
 
  @Override
  public final void setInputStream(InputStream istream) {
    m_istream = istream;
  }

}
