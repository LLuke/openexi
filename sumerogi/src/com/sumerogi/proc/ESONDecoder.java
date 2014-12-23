package com.sumerogi.proc;

import java.io.InputStream;
import java.io.IOException;

import com.sumerogi.proc.common.AlignmentType;
import com.sumerogi.proc.common.EXIOptions;
import com.sumerogi.proc.common.EXIOptionsException;
import com.sumerogi.proc.grammars.GrammarCache;
import com.sumerogi.proc.io.Scanner;
import com.sumerogi.proc.io.ScannerFactory;

/**
 * ESONDecoder provides methods to configure and 
 * instantiate a {@link com.sumerogi.proc.io.Scanner} object
 * you can use to parse the contents of an ESON stream. 
 */
public class ESONDecoder {

  private Scanner m_scanner; 
  
  private GrammarCache m_grammarCache;
  
  private InputStream m_inputStream;

  private final EXIOptions m_exiOptions;
  
  private static final int DEFAULT_INFLATOR_BUF_SIZE = 8192; 
  //private final int m_inflatorBufSize;
  
  /**
   * Creates an instance of EXIDecoder with the default inflator 
   * buffer size of 8192 bytes.  Buffer size is only used when
   * the EXI stream is encoded with EXI compression.
   */
  public ESONDecoder() {
    this(DEFAULT_INFLATOR_BUF_SIZE);
  }

  /**
   * Creates an instance of EXIDecoder with the specified inflator buffer 
   * size. When dynamic memory is limited on the target device, reducing 
   * the buffer size can improve performance and avoid runtime errors. Buffer 
   * size is only used when the EXI stream is encoded with EXI compression.
   * @param inflatorBufSize size of the buffer, in bytes.
   * @param useThreadedInflater Inflater will be run in its own thread if true
   */
  public ESONDecoder(int inflatorBufSize) {
    //m_inflatorBufSize = inflatorBufSize;
    m_exiOptions = new EXIOptions();
    m_grammarCache = null;
    m_scanner = ScannerFactory.createScanner(AlignmentType.bitPacked, DEFAULT_INFLATOR_BUF_SIZE);
    m_scanner.setStringTable(Scanner.createStringTable());
  }

  /**
   * Set an input stream from which the encoded stream is read.
   * @param istream InputSream to be read.
   */
  public final void setInputStream(InputStream istream) {
    m_inputStream = istream;
  }
  
  private final void setAlignmentType(AlignmentType alignmentType) {
    if (m_scanner.getAlignmentType() != alignmentType) {
      m_scanner = ScannerFactory.createScanner(alignmentType, DEFAULT_INFLATOR_BUF_SIZE);
      m_scanner.setStringTable(Scanner.createStringTable());
    }
  }
  
  /**
   * Set the GrammarCache used in decoding EXI streams. 
   * @param grammarCache {@link com.sumerogi.proc.grammars.GrammarCache}
   */
  public final void setGrammarCache(GrammarCache grammarCache) {
    if (m_grammarCache != grammarCache) {
      m_grammarCache = grammarCache;
    }
  }
  
  /**
   * Set the size, in number of values, of the information that will be 
   * processed as a chunk of the entire EXI stream. Reducing the block size 
   * can improve performance for devices with limited dynamic memory. 
   * Default is 1,000,000 items (not 1MB, but 1,000,000 complete Attribute 
   * and Element values). Block size is only used when the EXI stream is
   * encoded with EXI-compression.
   * @param blockSize number of values in each processing block. Default is 1,000,000.
   * @throws EXIOptionsException
   */
  public final void setBlockSize(int blockSize) throws EXIOptionsException {
    m_exiOptions.setBlockSize(blockSize);
  }
  
  /**
   * This method reads and configures any header options present
   * in the EXI stream, then returns a {@link com.sumerogi.proc.io.Scanner} 
   * object you can use to parse the values from the EXI stream.  
   * @return Scanner parsable object with header options applied.
   * @throws IOException
   */
  public Scanner processHeader() throws IOException {
    final AlignmentType alignmentType;
    alignmentType = AlignmentType.getAlignmentType(m_inputStream.read());
    
    setAlignmentType(alignmentType);
    
    final Scanner scanner;
    final GrammarCache grammarCache;
    scanner = m_scanner;
    scanner.setBlockSize(m_exiOptions.getBlockSize());
    scanner.setHeaderOptions(null);
    grammarCache = m_grammarCache;
    scanner.reset();
  
    scanner.setInputStream(m_inputStream);
  
    scanner.setGrammar(grammarCache.getDocumentGrammar());
    scanner.prepare();
    
    return scanner;
  }
  
}
