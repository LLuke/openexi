package org.openexi.proc.grammars;

public class ApparatusUtil {

  /**
   * Returns the codec ID effective for a type. 
   */
  public static short getCodecID(Apparatus apparatus, int typeSerial) {
    return apparatus.m_codecTable[typeSerial];
  }
  
}
