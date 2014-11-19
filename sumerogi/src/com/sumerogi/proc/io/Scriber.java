package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.grammars.Apparatus;
import com.sumerogi.proc.grammars.GrammarCache;
import com.sumerogi.schema.Characters;

public abstract class Scriber extends Apparatus {

  static final BigInteger BIGINTEGER_0x007F  = BigInteger.valueOf(0x007F);

//  private static final byte[] COOKIE = { 36, 69, 88, 73 }; // "$", "E", "X", "I"

  protected static final ValueScriber stringValueScriber;
  protected static final ValueScriber booleanValueScriber;
  public static final NumberValueScriber numberValueScriber;
  static {
    stringValueScriber = StringValueScriber.instance;
    booleanValueScriber = BooleanValueScriber.instance;
    numberValueScriber = NumberValueScriber.instance;
  }

  protected CharacterBuffer m_characterBuffer;
  
  // Used by writeLiteralString method to temporarily store UCS characters
  private int[] m_ucsBuffer;
  // Used by some of the ValueScribers to temporarily store digits
  public final StringBuilder stringBuilder1, stringBuilder2;
  final Scribble scribble1;
  
  protected OutputStream m_outputStream;

  /**
   * Creates a string table for use with a scriber. 
   * @param schema a schema that contains initial entries of the string table
   * @return a string table for use with a scriber
   */
  public static StringTable createStringTable(GrammarCache grammarCache) {
    return new StringTable(StringTable.Usage.encoding);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Constructor
  ///////////////////////////////////////////////////////////////////////////

  protected Scriber() {
  	super();
    
    m_characterBuffer = new CharacterBuffer(false);
    m_ucsBuffer = new int[1024];
    stringBuilder1 = new StringBuilder();
    stringBuilder2 = new StringBuilder();
    scribble1 = new Scribble();
    
    m_outputStream = null;
  }
  
  public abstract ValueScriber getStringValueScriber();
  public abstract ValueScriber getBooleanValueScriber();
  public abstract ValueScriber getNumberValueScriber();
  
  protected final CharacterBuffer ensureCharacters(final int length) {
    CharacterBuffer characterBuffer = m_characterBuffer;
    final int availability;
    if ((availability = m_characterBuffer.availability()) < length) {
      final int bufSize = length > CharacterBuffer.BUFSIZE_DEFAULT ? length : CharacterBuffer.BUFSIZE_DEFAULT;
      characterBuffer = new CharacterBuffer(bufSize, false);
    }
    if (characterBuffer != m_characterBuffer) {
      final int _availability = characterBuffer.availability();
      if (_availability != 0 && availability < _availability) {
        m_characterBuffer = characterBuffer;
      }
    }
    return characterBuffer;
  }
  
  public abstract void writeHeaderPreamble() throws IOException;

  /**
   * Set an output stream to which encoded streams are written out.
   * @param dataStream output stream
   */
  public abstract void setOutputStream(OutputStream dataStream);
  
  public abstract void setBlockSize(int blockSize);
  
  ///////////////////////////////////////////////////////////////////////////
  /// Methods for controlling Deflater parameters
  ///////////////////////////////////////////////////////////////////////////

  public void setDeflateParams(int level, int strategy) {
    // Do nothing.
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scriber Functions
  ///////////////////////////////////////////////////////////////////////////
  
  public abstract void writeEventType(EventType eventType) throws IOException;

  public abstract int writeName(String name, EventType eventType) throws IOException;

  /**
   * Write a text content item. 
   * Text content items are used in CM, PI, DT.
   */
  public final void writeText(String text) throws IOException {
    writeLiteralString(text, 0, m_outputStream);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Other Functions
  ///////////////////////////////////////////////////////////////////////////

  public abstract void finish() throws IOException;
  
  protected abstract void writeUnsignedInteger32(int uint, OutputStream ostream) throws IOException;
  protected abstract void writeUnsignedInteger64(long ulong, OutputStream ostream) throws IOException;
  protected abstract void writeUnsignedInteger(BigInteger uint, OutputStream ostream) throws IOException;

  protected final void writeLiteralCharacters(Characters str, int length, int lengthOffset, OutputStream ostream) 
    throws IOException {
    final int n_ucsCount = str.ucsCount;
    writeUnsignedInteger32(lengthOffset + n_ucsCount, ostream);
    final char[] characters = str.characters;
    final int charactersIndex = str.startIndex;
    for (int i = 0; i < length; i++) {
      final int c = characters[charactersIndex + i];
      final int ucs;
      if ((c & 0xFC00) != 0xD800) 
        ucs = c;
      else { // high surrogate
        final char c2 = characters[charactersIndex + ++i];
        if ((c2 & 0xFC00) == 0xDC00) { // low surrogate
          ucs = (((c & 0x3FF) << 10) | (c2 & 0x3FF)) + 0x10000; 
        }
        else {
          --i;
          ucs = c;
        }
      }
      writeUnsignedInteger32(ucs, ostream);
    }
  }

  /**
   * Write out a local name.
   * @return localName ID
   */
  protected final int writeLocalName(String localName, StringTable stringTable, OutputStream structureChannelStream) throws IOException {
    final int n_names, width, id;
    n_names = stringTable.n_strings;
    width = stringTable.width;
    if ((id = stringTable.internName(localName)) < n_names) {
      writeUnsignedInteger32(0, structureChannelStream);
      writeNBitUnsigned(id, width, structureChannelStream);
    }
    else {
      writeLiteralString(localName, 1, structureChannelStream);
    }
    return id;
  }
  
  private void writeLiteralString(String str, int lengthOffset, OutputStream structureChannelStream) throws IOException {
    final int length = str.length();
    if (length > m_ucsBuffer.length) {
      m_ucsBuffer = new int[length + 256];
    }
    int ucsCount = 0;
    for (int i = 0; i < length; ++ucsCount) {
      final char c = str.charAt(i++);
      int ucs = c;
      if ((c & 0xFC00) == 0xD800) { // high surrogate
        if (i < length) {
          final char c2 = str.charAt(i);
          if ((c2 & 0xFC00) == 0xDC00) { // low surrogate
            ucs = (((c & 0x3FF) << 10) | (c2 & 0x3FF)) + 0x10000;
            ++i;
          }
        }
      }
      m_ucsBuffer[ucsCount] = ucs;
    }
    writeUnsignedInteger32(lengthOffset + ucsCount, structureChannelStream);
    for (int i = 0; i < ucsCount; i++) {
      writeUnsignedInteger32(m_ucsBuffer[i], structureChannelStream);
    }
  }
  
  protected abstract void writeNBitUnsigned(int val, int width, OutputStream ostream) throws IOException;
  
  protected abstract void writeBoolean(boolean val, OutputStream ostream) throws IOException;

}
