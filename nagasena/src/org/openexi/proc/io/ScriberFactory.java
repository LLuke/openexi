package org.openexi.proc.io;

import java.lang.reflect.Constructor;

import org.openexi.proc.common.AlignmentType;

public final class ScriberFactory {
  
  private static final Constructor<?> m_channellingScriberConstructor;
  private static final Object[] ARGS_PRECOMPRESS = new Object[] { Boolean.FALSE };
  private static final Object[] ARGS_COMPRESS = new Object[] { Boolean.TRUE };

  static {
    ClassLoader cloader = ScriberFactory.class.getClassLoader();
    Constructor<?> constructor = null;
    try {
      Class<?> channellingScriberClass = cloader.loadClass("org.openexi.proc.io.compression.ChannellingScriber");
      constructor = channellingScriberClass.getDeclaredConstructor(boolean.class); 
    }
    catch (ClassNotFoundException cnfe) {
    }
    catch (NoSuchMethodException nsme) {
      assert false;
    }
    m_channellingScriberConstructor = constructor;
  }

  /**
   * Create a scriber.
   * @param alignmentType
   * @return a scriber, or null if AlignmentType.preCompress or AlignmentType.compress was 
   * specified as alignmentType when EXI compression classes are not made available.
   */
  public static Scriber createScriber(AlignmentType alignmentType) {
    switch (alignmentType) {
      case bitPacked:
        return new BitPackedScriber(false);
      case byteAligned:
        return new ByteAlignedScriber();
      case preCompress:
        if (m_channellingScriberConstructor != null) {
          try {
            return (Scriber)m_channellingScriberConstructor.newInstance(ARGS_PRECOMPRESS);
          }
          catch (Exception exc) {
          }
        }
      case compress:
        if (m_channellingScriberConstructor != null) {
          try {
            return (Scriber)m_channellingScriberConstructor.newInstance(ARGS_COMPRESS);
          }
          catch (Exception exc) {
          }
        }
    }
    return null;
  }

  public static BitPackedScriber createHeaderOptionsScriber() {
    return new BitPackedScriber(true);
  }
  
}
