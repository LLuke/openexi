package org.openexi.proc.io;

import java.io.InputStream;

import org.openexi.proc.common.QName;
import org.openexi.schema.EXISchema;

abstract class ValueScannerBase extends ValueScanner {
  
  private final QName m_name;

  ValueScannerBase(QName name) {
    m_name = name;
  }
 
  @Override
  public int getBuiltinRCS(int simpleType) {
    return EXISchema.NIL_NODE;
  }

  @Override
  public final void setInputStream(InputStream istream) {
    m_istream = istream;
  }

  @Override
  public final QName getName() {
    return m_name;
  }

}
