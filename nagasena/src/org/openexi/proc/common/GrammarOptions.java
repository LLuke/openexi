package org.openexi.proc.common;
/**
  * GrammarOptions computes a short integer that represents settings in the EXI Grammar settings
  * that determine how an EXI file will be encoded or decoded.
  *<br/><br/>
  * Values are set using binary switch values (represented as short integers).
  * <br /><br />
  * If no options have been set, the value is 0 (this is an temporary internal
  * value, rather than a valid setting).
  * <br/><br/>
  * If an XSD is available, and the file to be processed is 100% compatible with the XSD, choosing
  * STRICT_OPTIONS (1) provides the best performance. No other options can be set 
  * when files are encoded or decoded in strict mode.
  * <br/><br/>
  * The DEFAULT_OPTIONS setting is 2. The following table lists all of the options and their values. 
  * <br/><br/>
  * <table align="center" border="1" cellpadding="3" width="640"><tr>
  * <th>Constant</th><th>Value</th>
  * </tr><tr>
  * <td>STRICT_OPTIONS</td><td>1</td>
  * </tr><tr>
  * <td>DEFAULT_OPTIONS</td><td>2</td>
  * </tr><tr><td>
  * ADD_NS</td><td>4
  * </td></tr><tr><td>
  * ADD_SC</td><td>N/A*
  * </td></tr><tr><td>
  * ADD_DTD</td><td>16
  * </td></tr><tr><td>
  * ADD_CM</td><td>32
  * </td></tr><tr><td>
  * ADD_PI</td><td>64
  * </td></tr>
  * <tr><td colspan="2">*The self-contained option is not supported in this release.
  * </td></tr>
  * </table>
  * <br/><br/>
  * The value of DEFAULT_OPTIONS is 2. When you pass the options variable to an add[??] method, 
  * the binary switch value is added to the current value of the options variable.  
  * The sum of all additional switches becomes a concise list of the selected options. 
  * <br/><br/>
  * For example, if you preserve comments and processing instructions, the total is
  * 98 (2 + 32 + 64). The bitwise options are set correctly, because there is one and only one 
  * combination of options that sums up to 98.
  * <br/><br/>
  * If your application uses the same options every time, you can hard code the numeric value 
  * as a short integer and use it to create your GrammarCache. For example:
  * <pre>
  * GrammarCache gc = new GrammarCache((EXISchema)null,98);
  * </pre>
  * Be careful to use the correct numeric value, to avoid unexpected results.
  */
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
   * OPTIONS_UNUSED is an internal value.
   * It indicates that the grammar options value has not yet been set.
   */
  public static final short OPTIONS_UNUSED = 0; 

  /**
   * Indicates that undeclared elements and attributes will be 
   * processed when the XML stream is encoded and decoded.
   */
  public static final short DEFAULT_OPTIONS = ADD_UNDECLARED_EA_MASK;

  /**
   * Indicates that undeclared elements and attributes will throw an
   * exception when the XML stream is encoded and decoded. When 
   * STRICT_OPTIONS is set, all other Grammar Options are ignored.
   */
  public static final short STRICT_OPTIONS  = RESTRICT_XSI_NIL_TYPE_MASK;
  
  private GrammarOptions() {
  }
  /**
   * If STRICT_OPTIONS is true, set the first bit to 1. Otherwise, set the first bit to 0.
   */
  static short restrictXsiNilType(short options, boolean val) {
    return (short)(val ? options | RESTRICT_XSI_NIL_TYPE_MASK : options & ~RESTRICT_XSI_NIL_TYPE_MASK); 
  }

  /**
   * Returns <i>true</i> if STRICT_OPTIONS is set to true.
   * Not for public use.
   * @y.exclude
   */
  public static boolean isXsiNilTypeRestricted(short options) {
    return (options & RESTRICT_XSI_NIL_TYPE_MASK) != 0;
  }

  /**
   * Returns <i>true</i> if DEFAULT_OPTIONS is set to true.
   */
  public static boolean isPermitDeviation(short options) {
    return (options & ADD_UNDECLARED_EA_MASK) != 0;
  }

  /**
   * Returns <i>true</i> if Preserve Namespaces is true.
   */
  public static boolean hasNS(short options) {
    return (options & ADD_NS) != 0;
  }
  /**
   * The self-contained option is not supported in this release.
   * @y.exclude
   */
  public static boolean hasSC(short options) {
    return (options & ADD_SC) != 0;
  }

  /**
   * Returns <i>true</i> if Preserve Document Type Definition is true.
   */  
  public static boolean hasDTD(short options) {
    return (options & ADD_DTD) != 0;
  }

  /**
   * Returns <i>true</i> if Preserve Comments is true.
   */  
  public static boolean hasCM(short options) {
    return (options & ADD_CM) != 0;
  }

  /**
   * Returns <i>true</i> if Preserve Processing Instructions is true.
   */  
  public static boolean hasPI(short options) {
    return (options & ADD_PI) != 0;
  }

  /**
   * Sets Preserve Namespaces to <i>true</i>. (Adds 4 to the <i>options</i> value.)
   * @return the new <i>options</i> short integer value
   */  
  public static short addNS(short options) {
    return (short)(options | ADD_NS); 
  }
  /**
   * The self-contained option is not supported in this release.
   * @y.exclude
   */
  public static short addSC(short options) {
    return (short)(options | ADD_SC); 
  }

  /**
   * Sets Preserve Document Type Definition to <i>true</i>. (Adds 16 to the <i>options</i> value.)
   * @return the new <i>options</i> short integer value
   */  
  public static short addDTD(short options) {
    return (short)(options | ADD_DTD);
  }

  /**
   * Sets Preserve Comments to <i>true</i>. (Adds 32 to the <i>options</i> value.) 
   * @return the new <i>options</i> short integer value
   */  
  public static short addCM(short options) {
    return (short)(options | ADD_CM);
  }

  /**
   * Sets Preserve Processing Instructions to <i>true</i>. (Adds 64 to the <i>options</i> value.)
   * @return the new <i>options</i> short integer value
   */  
  public static short addPI(short options) {
    return (short)(options | ADD_PI);
  }
  
}
