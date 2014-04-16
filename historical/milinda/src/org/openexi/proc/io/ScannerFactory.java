package org.openexi.proc.io;

import java.lang.reflect.Constructor;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.grammars.DocumentGrammarState;

public final class ScannerFactory {

  private static final Constructor m_channellingScannerConstructor;
  private static final Object[] ARGS_PRECOMPRESS = new Object[] { Boolean.FALSE };
  private static final Object[] ARGS_COMPRESS = new Object[] { Boolean.TRUE };

  static {
    ClassLoader cloader = ScriberFactory.class.getClassLoader();
    Constructor constructor = null;
    try {
      Class channellingScriberClass = cloader.loadClass("org.openexi.proc.io.compression.ChannellingScanner");
      constructor = channellingScriberClass.getConstructor(boolean.class);
    }
    catch (ClassNotFoundException cnfe) {
    }
    catch (NoSuchMethodException nsme) {
      assert false;
    }
    m_channellingScannerConstructor = constructor;
  }
  
  /**
   * Create a scanner for processing the EXI body of a stream.
   * @param alignmentType
   * @param inflatorBufSize
   * @param documentGrammarState
   * @return a scanner, or null if AlignmentType.preCompress or AlignmentType.compress was 
   * specified as alignmentType when EXI compression classes are not made available.
   */
  public static Scanner createScanner(AlignmentType alignmentType, int inflatorBufSize, DocumentGrammarState documentGrammarState) {
    final Scanner scanner;
    switch (alignmentType) {
      case bitPacked:
        scanner = new BitPackedScanner(false);
        break;
      case byteAligned:
        scanner = new ByteAlignedScanner();
        break;
      case preCompress:
        if (m_channellingScannerConstructor != null) {
          try {
            scanner = (Scanner)m_channellingScannerConstructor.newInstance(ARGS_PRECOMPRESS);
          }
          catch (Exception exc) {
            return null;
          }
        }
        else
          return null;
        break;
      case compress:
        if (m_channellingScannerConstructor != null) {
          try {
            scanner = (Scanner)m_channellingScannerConstructor.newInstance(ARGS_COMPRESS);
          }
          catch (Exception exc) {
            return null;
          }
        }
        else
          return null;
        break;
      default:
        return null;
    }
    scanner.init(documentGrammarState, inflatorBufSize);
    return scanner;
  }

  /**
   * Create a scanner for processing the header option document of a stream.
   */
  public static BitPackedScanner createHeaderOptionsScanner(DocumentGrammarState documentGrammarState) {
    final BitPackedScanner scanner = new BitPackedScanner(true);
    scanner.init(documentGrammarState);
    return scanner;
  }
  
}
