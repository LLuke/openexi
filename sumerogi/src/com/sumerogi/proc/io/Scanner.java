package com.sumerogi.proc.io;

import java.math.BigInteger;
import java.io.IOException;
import java.io.InputStream;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EXIOptions;
import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.grammars.Apparatus;
import com.sumerogi.proc.grammars.Grammar;
import com.sumerogi.proc.grammars.GrammarState;
import com.sumerogi.schema.Characters;

/**
 * 
 * The Scanner class provides methods for scanning events 
 * in the body of an EXI stream.
 */
public abstract class Scanner extends Apparatus {

  /**
   * Pairs of (elementURI, elementLocalName)
   * @y.exclude 
   **/
  protected final int[] m_nameLocusStack;
  /** @y.exclude */
  protected int m_nameLocusLastDepth;
  
  /** @y.exclude */
  protected final StringValueScanner m_stringValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_booleanValueScannerInherent;
  
  protected final NumberValueScanner m_numberValueScannerInherent;
  
  /** @y.exclude */
  protected final FloatValueScanner m_floatValueScannerInherent;
  /** @y.exclude */
  protected final DecimalValueScanner m_decimalValueScannerInherent;
  /** @y.exclude */
  protected final IntegerValueScanner m_integerValueScannerInherent;

  private EXIOptions m_exiHeaderOptions;

  /**
   * Not for public use.
   * @y.exclude
   */
  protected InputStream m_inputStream;
  
  private static final Characters TRUE; // "true" (4)
  private static final Characters FALSE; // "false" (5)
  static {
    TRUE = new Characters("true".toCharArray(), 0, "true".length(), false);
    FALSE = new Characters("false".toCharArray(), 0, "false".length(), false);
  }
  
  protected final CharacterBuffer m_characterBuffer;
  
  /**
   * Creates a string table for use with a scanner. 
   * @param schema a schema that contains initial entries of the string table
   * @return a string table for use with a scanner
   * Not for public use.
   * @y.exclude
   */
  public static StringTable createStringTable() {
    return new StringTable(StringTable.Usage.decoding);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Constructor
  ///////////////////////////////////////////////////////////////////////////

  protected Scanner() {
    m_nameLocusStack = new int[128];
    m_nameLocusLastDepth = -2;
    
    m_characterBuffer = new CharacterBuffer(true);
    
    m_stringValueScannerInherent = new StringValueScanner(this);
    m_booleanValueScannerInherent = new BooleanValueScanner();
    m_integerValueScannerInherent = new IntegerValueScanner();
    m_decimalValueScannerInherent = new DecimalValueScanner();
    m_floatValueScannerInherent = new FloatValueScanner();
    
    m_numberValueScannerInherent = new NumberValueScanner();
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */ 
  protected void init(int inflatorBufSize) {
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */  
  private void initValueScanners(InputStream istream) {
    m_stringValueScannerInherent.setInputStream(istream);
    m_booleanValueScannerInherent.setInputStream(istream);
    m_integerValueScannerInherent.setInputStream(istream);
    m_decimalValueScannerInherent.setInputStream(istream);
    m_floatValueScannerInherent.setInputStream(istream);
    m_numberValueScannerInherent.setInputStream(istream);
  }

  /**
   * Not for public use.
   * @y.exclude
   */
  @Override
  public void reset() {
    super.reset();
    m_nameLocusLastDepth = -2;
  }
  
  /**
   * Gets the next event from the EXI stream.
   * @return EXIEvent
   * @throws IOException
   */
  public abstract EventDescription nextEvent() throws IOException;
  
  /**
   * Prepares the scanner ready for getting nextEvent() called.
   * @throws IOException
   * Not for public use.
   * @y.exclude
   */
  public void prepare() throws IOException {
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public void setInputStream(InputStream istream) {
    m_inputStream = istream;
    initValueScanners(istream);
  }
  
  /**
   * Close the input stream.
   */
  public void closeInputStream() throws IOException {
    m_inputStream.close();
  }
  
  /**
   * Set one of FragmentGrammar, BuiltinFragmentGrammar or DocumentGrammar.
   * @y.exclude
   */
  public final void setGrammar(Grammar grammar) {
    grammar.init(currentState);
  }

  /** @y.exclude */
  @Override
  public final void setStringTable(StringTable stringTable) {
    super.setStringTable(stringTable);
    m_stringValueScannerInherent.setStringTable(stringTable);
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public abstract void setBlockSize(int blockSize);
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public final void setHeaderOptions(EXIOptions headerOptions) {
    m_exiHeaderOptions = headerOptions;
  }
  /**
   * Returns the EXI Header options from the header of the
   * EXI stream, if present. Otherwise, returns null.
   * @return EXIOptions or <i>null</i> if no header options are set.
   */
  public final EXIOptions getHeaderOptions() {
    return m_exiHeaderOptions;
  }

  /**
   * Returns the current grammar state if the alignment type is bit-packed or byte-alignment. 
   * @return current grammar state
   * @y.exclude
   */
  public final GrammarState getGrammarState() {
    switch (getAlignmentType()) {
      case bitPacked:
        return currentState;
      default:
        assert false;
        return null;
    }
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scanner Functions
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Read a localName.
   * @return localName ID
   * @y.exclude
   */
  protected final int readName(StringTable stringTable) throws IOException {
    final int length = readUnsignedInteger(m_inputStream);
    if (length != 0) {
      final String name = readLiteralString(length - 1, m_inputStream).makeString();
      return stringTable.addName(name);
    }
    else {
      return readNBitUnsigned(stringTable.width, m_inputStream);
    }
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Crude bits Reader functions
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Not for public use.
   * @y.exclude
   */
  protected abstract boolean readBoolean(InputStream istream) throws IOException;
  
  /**
   * Not for public use.
   * @y.exclude
   */
  protected final int readUnsignedInteger(InputStream istream) throws IOException {
    int shift = 0;
    int uint = 0;
    do {
      final int nextByte;
      if (((nextByte = readEightBitsUnsigned(istream)) & 0x0080) != 0) { // check continuation flag
        uint |= ((nextByte & 0x007F) << shift);
        shift += 7;
      }
      else {
        return uint | (nextByte << shift);
      }
    }
    while (true);
  }

  /**
   * Not for public use.
   * @y.exclude
   */
  protected final long readUnsignedIntegerAsLong(InputStream istream) throws IOException {
    int shift = 0;
    long uint = 0;
    do {
      final int nextByte;
      if (((nextByte = readEightBitsUnsigned(istream)) & 0x0080) != 0) { // check continuation flag
        uint |= (((long)nextByte & 0x007F) << shift);
        shift += 7;
      }
      else {
        return uint | ((long)nextByte << shift);
      }
    }
    while (true);
  }

  /**
   * Digits are stored into the character array in reverse order. 
   * @y.exclude
   */
  protected final int readUnsignedIntegerChars(InputStream istream, boolean addOne, char[] resultChars) throws IOException {
    int shift = 0;
    int uint = addOne ? 1 : 0;
    int pos = 0;
    boolean continued = true;
    do {
      int nextByte = readEightBitsUnsigned(istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      uint += (nextByte << shift);
      if (!continued) {
        do {
          resultChars[pos++] = (char)(48 + uint % 10);
          uint /= 10;
        }
        while (uint != 0);
        return pos;
      }
      shift += 7;
    }
    while (shift != 28);
    
    final int shiftLimit = addOne ? 56 : 63;
    long ulong = uint;
    do {
      long nextByte = readEightBitsUnsigned(istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      ulong += (nextByte << shift);
      if (!continued) {
        while (ulong != 0) {
          resultChars[pos++] = (char)(48 + (int)(ulong % 10L));
          ulong /= 10L;
        }
        return pos;
      }
      shift += 7;
    }
    while (shift != shiftLimit);
    
    BigInteger uinteger = BigInteger.valueOf(ulong);
    do {
      int nextByte = readEightBitsUnsigned(istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      uinteger = uinteger.add(BigInteger.valueOf(nextByte).shiftLeft(shift));
      shift += 7;
    }
    while (continued);

    // NOTE: Let BigInteger to the job of the conversion. It's just faster that way.
    final String digitsString = uinteger.toString();
    final int n_digits = digitsString.length();
    int i, ind;
    for (i = 0, ind = n_digits; i < n_digits; i++)
      resultChars[pos++] = digitsString.charAt(--ind);
    return pos;
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */  
  protected final Characters readLiteralString(int ucsCount, InputStream istream) throws IOException {
    m_characterBuffer.ensureCharacters(ucsCount);
    char[] characters = m_characterBuffer.characters;
    int charactersIndex = m_characterBuffer.allocCharacters(ucsCount);
    final int _ucsCount = ucsCount;
    assert charactersIndex != -1;
    int length = 0;;
    for (boolean foundNonBMP = false; ucsCount != 0; --ucsCount) {
      final int c;
      if (((c = readUnsignedInteger(istream)) & 0xFFFF0000) != 0) { // non-BMP character
        if (!foundNonBMP) {
          final char[] _characters = new char[2 * _ucsCount];
          for (int i = 0; i < length; i++) {
            _characters[i] = characters[charactersIndex + i];
          }
          charactersIndex = 0;
          characters = _characters;
          m_characterBuffer.redeemCharacters(_ucsCount);
          foundNonBMP = true;
        }
        characters[length++] = (char)(((c - 0x10000) >> 10) | 0xD800);
        characters[length++] = (char)(((c - 0x10000) & 0x3FF) | 0xDC00);
        continue;
      }
      characters[charactersIndex + length++] = (char)c;
    }
    return new Characters(characters, charactersIndex, length, m_characterBuffer.isVolatile);
  }

  /** @y.exclude */
  protected abstract int readNBitUnsigned(int width, InputStream istream) throws IOException;

  /** @y.exclude */
  protected abstract int readEightBitsUnsigned(InputStream istream) throws IOException;
  
  ///////////////////////////////////////////////////////////////////////////
  /// Value Scanners
  ///////////////////////////////////////////////////////////////////////////
  
  private final class BooleanValueScanner extends ValueScannerBase {
    @Override
    public Characters scan(int localNameId) throws IOException {
      final boolean val = readBoolean(m_istream);
      return val ? TRUE : FALSE;
    }
  }
  
  protected final class NumberValueScanner extends ValueScannerBase {
    @Override
    public Characters scan(int localName) throws IOException {
      final int numberType = readNBitUnsigned(NUMBER_TYPE_WIDTH, m_istream);
      switch (numberType) {
        case INTEGER_VALUE:
          return m_integerValueScannerInherent.scan(localName);
        case DECIMAL_VALUE:
          return m_decimalValueScannerInherent.scan(localName);
        default:
          return m_floatValueScannerInherent.scan(localName);
//          assert false;
//          return null;
      }
    }
  }

  protected final class IntegerValueScanner extends ValueScannerBase {
    private final char[] m_digitsBuffer;
    IntegerValueScanner() {
      m_digitsBuffer  = new char[128];
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public Characters scan(int localName) throws IOException {
      final boolean isNegative = readBoolean(m_istream);
      int pos = readUnsignedIntegerChars(m_istream, isNegative, m_digitsBuffer);
      if (isNegative)
        m_digitsBuffer[pos++] = '-';
      m_characterBuffer.ensureCharacters(pos);
      return m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
    }
  }

  private final class DecimalValueScanner extends ValueScannerBase {
    private final char[] m_integralDigitsChars;
    private final char[] m_fractionDigitsChars;
    DecimalValueScanner() {
      m_integralDigitsChars = new char[128];
      m_fractionDigitsChars = new char[128];
    }
    /** @y.exclude */
    @Override
    public Characters scan(int localName) throws IOException {
      final boolean isNegative = readBoolean(m_istream);
      int n_integralDigits = readUnsignedIntegerChars(m_istream, false, m_integralDigitsChars);
      if (isNegative)
        m_integralDigitsChars[n_integralDigits++] = '-';
      final int n_fractionDigits = readUnsignedIntegerChars(m_istream, false, m_fractionDigitsChars);
      final int totalLength = n_integralDigits + 1 + n_fractionDigits;
      m_characterBuffer.ensureCharacters(totalLength);
      return m_characterBuffer.addDecimalChars(m_integralDigitsChars, n_integralDigits, m_fractionDigitsChars, n_fractionDigits, totalLength);
    }
  }

  private final class FloatValueScanner extends ValueScannerBase {
    FloatValueScanner() {
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public Characters scan(int localNameId) throws IOException {
      final boolean isNegative = readBoolean(m_istream);
      long longValue = readUnsignedInteger63(m_istream);
      if (isNegative)
        longValue = -longValue - 1;
      final String mantissaDigitsString = Long.toString(longValue); 
      final boolean isNegativeExponent = readBoolean(m_istream);
      int intValue = readUnsignedInteger(m_istream);
      if (isNegativeExponent)
        ++intValue;
      final String stringValue;
      if (16384 != intValue) {
        stringValue = mantissaDigitsString + 'E' + (isNegativeExponent ? "-" : "") +  Integer.toString(intValue); 
      }
      else {
        stringValue = longValue == 1 ? "INF" : longValue == -1 ? "-INF" : "NaN";    
      }
      final int length = stringValue.length();
      m_characterBuffer.ensureCharacters(length);
      return m_characterBuffer.addString(stringValue, length);
    }
    /**
     * Read an unsigned integer value of range [0 ... 2^63 - 1].
     * Possible effective number of bits 7, 14, 21, 28, 35, 42, 49, 56, 63.
     */
    private final long readUnsignedInteger63(InputStream istream) throws IOException {
      int shift = 0;
      boolean continued = true;
      long ulong = 0;
      do {
        long nextByte = readEightBitsUnsigned(istream);
        if ((nextByte & 0x0080) != 0) // check continuation flag
          nextByte &= 0x007F;
        else
          continued = false;
        ulong += (nextByte << shift);
        if (!continued)
          return ulong;
        shift += 7;
      }
      while (shift != 63);
      assert !continued;
      return ulong;
    }
  }

}
