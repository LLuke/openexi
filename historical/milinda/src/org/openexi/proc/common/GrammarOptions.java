package org.openexi.proc.common;

public final class GrammarOptions {
  
  /**
   * restrict the infusion of xsi:nil and xsi:type if the field is on.
   */
  private static final short RESTRICT_XSI_NIL_TYPE_MASK = 0x0001;
  /**
   * add undeclared SE, AT and EE event types if the field is on.
   */
  private static final short ADD_UNDECLARED_EA_MASK     = 0x0002;
  private static final short ADD_NS                     = 0x0004;
  private static final short ADD_SC                     = 0x0008;
  private static final short ADD_DTD                    = 0x0010;
  private static final short ADD_CM                     = 0x0020;
  private static final short ADD_PI                     = 0x0040;
  
  /**
   * OPTIONS_UNUSED signifies that grammar options value has not yet been set.
   */
  public static final short OPTIONS_UNUSED = 0; 
  public static final short DEFAULT_OPTIONS = ADD_UNDECLARED_EA_MASK;
  public static final short STRICT_OPTIONS  = RESTRICT_XSI_NIL_TYPE_MASK;
  
  private GrammarOptions() {
  }
  
  static short restrictXsiNilType(short options, boolean val) {
    return (short)(val ? options | RESTRICT_XSI_NIL_TYPE_MASK : options & ~RESTRICT_XSI_NIL_TYPE_MASK); 
  }

  public static boolean isXsiNilTypeRestricted(short options) {
    return (options & RESTRICT_XSI_NIL_TYPE_MASK) != 0;
  }
  
  public static boolean hasUndeclaredEA(short options) {
    return (options & ADD_UNDECLARED_EA_MASK) != 0;
  }

  public static boolean hasNS(short options) {
    return (options & ADD_NS) != 0;
  }

  public static boolean hasSC(short options) {
    return (options & ADD_SC) != 0;
  }
  
  public static boolean hasDTD(short options) {
    return (options & ADD_DTD) != 0;
  }

  public static boolean hasCM(short options) {
    return (options & ADD_CM) != 0;
  }

  public static boolean hasPI(short options) {
    return (options & ADD_PI) != 0;
  }

  public static short addNS(short options) {
    return (short)(options | ADD_NS); 
  }

  public static short addSC(short options) {
    return (short)(options | ADD_SC); 
  }

  public static short addDTD(short options) {
    return (short)(options | ADD_DTD);
  }
  
  public static short addCM(short options) {
    return (short)(options | ADD_CM);
  }
  
  public static short addPI(short options) {
    return (short)(options | ADD_PI);
  }
  
}
