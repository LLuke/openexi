package org.openexi.fujitsu.proc.io;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.util.URIConst;

abstract class StringValueScanner extends ValueScannerBase {
  
  protected StringValueScanner() {
    super(new QName("exi:string", URIConst.W3C_2009_EXI_URI));
  }

  public abstract void setValueMaxLength(int valueMaxLength);

}
