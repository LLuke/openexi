package com.sumerogi.proc.io;

import java.lang.reflect.Constructor;

import com.sumerogi.proc.common.AlignmentType;

public final class ScannerFactory {

  private static final Constructor<?> m_channellingScannerConstructor;
  private static final Object[] preCompress_args;
  private static final Object[] compress_args;

  static {
    ClassLoader cloader = ScannerFactory.class.getClassLoader();
    Constructor<?> constructor = null;
    try {
      Class<?> channellingScannerClass = cloader.loadClass("com.sumerogi.proc.io.compression.ChannellingScanner");
      constructor = channellingScannerClass.getConstructor(boolean.class);
    }
    catch (ClassNotFoundException cnfe) {
    }
    catch (NoSuchMethodException nsme) {
      nsme.printStackTrace();
      assert false;
    }
    m_channellingScannerConstructor = constructor;
    preCompress_args = new Object[] { Boolean.FALSE };
    compress_args = new Object[] { Boolean.TRUE };
  }
  
  public static Scanner createScanner(AlignmentType alignmentType, int inflatorBufSize) {
    final Scanner scanner;
    switch (alignmentType) {
      case bitPacked:
        scanner = new BitPackedScanner();
        break;
      case byteAligned:
        scanner = new ByteAlignedScanner();
        break;
      case preCompress:
        if (m_channellingScannerConstructor != null) {
          try {
            scanner = (Scanner)m_channellingScannerConstructor.newInstance(preCompress_args);
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
            scanner = (Scanner)m_channellingScannerConstructor.newInstance(compress_args);
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
  
}
