package org.openexi.proc.io;

import java.lang.reflect.Constructor;

import org.openexi.proc.common.AlignmentType;

public final class ScannerFactory {

  private static final Constructor<?> m_channellingScannerConstructor;

  static {
    ClassLoader cloader = ScannerFactory.class.getClassLoader();
    Constructor<?> constructor = null;
    try {
      Class<?> channellingScannerClass = cloader.loadClass("org.openexi.proc.io.compression.ChannellingScanner");
      constructor = channellingScannerClass.getConstructor(boolean.class, boolean.class);
    }
    catch (ClassNotFoundException cnfe) {
    }
    catch (NoSuchMethodException nsme) {
      nsme.printStackTrace();
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
  public static Scanner createScanner(AlignmentType alignmentType, int inflatorBufSize, boolean useThreadedInflater) {
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
            scanner = (Scanner)m_channellingScannerConstructor.newInstance(
                new Object[] { Boolean.FALSE, useThreadedInflater });
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
            scanner = (Scanner)m_channellingScannerConstructor.newInstance(
                new Object[] { Boolean.TRUE, useThreadedInflater });
          }
          catch (Exception exc) {
            return null;
          }
        }
        else
          return null;
        scanner.init(inflatorBufSize);
        break;
      default:
        return null;
    }
    return scanner;
  }

  /**
   * Create a scanner for processing the header option document of a stream.
   */
  public static BitPackedScanner createHeaderOptionsScanner() {
    return new BitPackedScanner(true);
  }
  
}
