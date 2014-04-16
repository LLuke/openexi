package org.openexi.proc.io;

import org.openexi.proc.common.QName;
import org.openexi.proc.util.ExiUriConst;

abstract class StringValueScanner extends ValueScannerBase {
  
  protected StringValueScanner() {
    super(new QName("exi:string", ExiUriConst.W3C_2009_EXI_URI));
  }

  public abstract void setValueMaxLength(int valueMaxLength);

}
