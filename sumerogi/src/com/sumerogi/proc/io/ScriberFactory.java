package com.sumerogi.proc.io;

import java.lang.reflect.Constructor;

import com.sumerogi.proc.common.AlignmentType;

public final class ScriberFactory {
  
  private static final Constructor<?> m_channellingScriberConstructor;
  private static final Object[] preCompress_args;
  private static final Object[] compress_args;

  static {
    ClassLoader cloader = ScriberFactory.class.getClassLoader();
    Constructor<?> constructor = null;
    try {
      Class<?> channellingScriberClass = cloader.loadClass("com.sumerogi.proc.io.compression.ChannellingScriber");
      constructor = channellingScriberClass.getConstructor(boolean.class);
    }
    catch (ClassNotFoundException cnfe) {
    }
    catch (NoSuchMethodException nsme) {
      nsme.printStackTrace();
      assert false;
    }
    m_channellingScriberConstructor = constructor;
    preCompress_args = new Object[] { Boolean.FALSE };
    compress_args = new Object[] { Boolean.TRUE };
  }
  
  public static Scriber createScriber(AlignmentType alignmentType) {
    switch (alignmentType) {
      case bitPacked:
        return new BitPackedScriber();
      case byteAligned:
        return new ByteAlignedScriber();        
      case preCompress:
        if (m_channellingScriberConstructor != null) {
          try {
            return (Scriber)m_channellingScriberConstructor.newInstance(preCompress_args);
          }
          catch (Exception exc) {
            return null;
          }
        }
        return null;
      case compress:
        if (m_channellingScriberConstructor != null) {
          try {
            return (Scriber)m_channellingScriberConstructor.newInstance(compress_args);
          }
          catch (Exception exc) {
            return null;
          }
        }
        return null;
    }
    return null;
  }

}
