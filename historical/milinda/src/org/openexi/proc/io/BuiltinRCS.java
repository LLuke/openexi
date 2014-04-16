package org.openexi.proc.io;

final class BuiltinRCS {

  public static final int RCS_ID_BASE64BINARY = -2;
  public static final int RCS_ID_HEXBINARY = -3; 
  public static final int RCS_ID_BOOLEAN = -4;
  public static final int RCS_ID_DATETIME = -5;
  public static final int RCS_ID_DECIMAL = -6;
  public static final int RCS_ID_DOUBLE = -7;
  public static final int RCS_ID_INTEGER = -8;

  /**
   * WIDTHS maps number of characters in RCS to width in bits,
   * accounting the extraneous escape indicator. 
   */
  public static final byte[] WIDTHS = {
    0, // 0 
    1, // 1
    2, 2, // 2...3
    3, 3, 3, 3, // 4...7
    4, 4, 4, 4, 4, 4, 4, 4, // 8...15
    5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, // 16...31
    6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 
    6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, // 32...63
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, // 64...127
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 
    8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, // 128...255 
  };

  public static final int[] RCS_BASE64BINARY = new int[] { 
    0x9, 0xA, 0xD, 0x20, '+', '/',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '=',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 
    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
  } ;

  public static final int[] RCS_HEXBINARY = new int[] {
    0x9, 0xA, 0xD, 0x20,
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
    'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f'
  };
  
  public static final int[] RCS_BOOLEAN = new int[] {
    0x9, 0xA, 0xD, 0x20,
    '0', '1', 'a', 'e', 'f', 'l', 'r', 's', 't', 'u'
  };
  
  public static final int[] RCS_DATETIME = new int[] {
    0x9, 0xA, 0xD, 0x20, '+', '-', '.',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', 'T', 'Z'
  };
  
  public static final int[] RCS_DECIMAL = new int[] {
    0x9, 0xA, 0xD, 0x20, '+', '-', '.',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
  };
  
  public static final int[] RCS_DOUBLE = new int[] {
    0x9, 0xA, 0xD, 0x20, '+', '-', '.',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
    'E', 'F', 'I', 'N', 'a', 'e'
  };

  public static final int[] RCS_INTEGER = new int[] {
    0x9, 0xA, 0xD, 0x20, '+', '-',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
  };
  
  public static final short RCS_BASE64BINARY_WIDTH = WIDTHS[RCS_BASE64BINARY.length];
  public static final short RCS_HEXBINARY_WIDTH = WIDTHS[RCS_HEXBINARY.length]; 
  public static final short RCS_BOOLEAN_WIDTH = WIDTHS[RCS_BOOLEAN.length];
  public static final short RCS_DATETIME_WIDTH = WIDTHS[RCS_DATETIME.length];
  public static final short RCS_DECIMAL_WIDTH = WIDTHS[RCS_DECIMAL.length];
  public static final short RCS_DOUBLE_WIDTH = WIDTHS[RCS_DOUBLE.length]; 
  public static final short RCS_INTEGER_WIDTH = WIDTHS[RCS_INTEGER.length]; 
  
  private BuiltinRCS() {
  }
  
}
