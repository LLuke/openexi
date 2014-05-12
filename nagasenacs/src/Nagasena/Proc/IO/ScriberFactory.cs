using System;
using System.Diagnostics;
using System.Reflection;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;

namespace Nagasena.Proc.IO {

  public sealed class ScriberFactory {

    private static readonly ConstructorInfo m_channellingScriberConstructor;

    private static readonly Object[] ARGS_PRECOMPRESS = new Object[] { false };
    private static readonly Object[] ARGS_COMPRESS = new Object[] { true };

    static ScriberFactory() {
      Type channellingScriberType = Type.GetType("Nagasena.Proc.IO.Compression.ChannellingScriber");
      m_channellingScriberConstructor = channellingScriberType == null ? null :
        channellingScriberType.GetConstructor(new Type[] { typeof(bool) });
    }

    /// <summary>
    /// Create a scriber. </summary>
    /// <param name="alignmentType"> </param>
    /// <returns> a scriber, or null if AlignmentType.preCompress or AlignmentType.compress was 
    /// specified as alignmentType when EXI compression classes are not made available. </returns>
    public static Scriber createScriber(AlignmentType alignmentType) {
      switch (alignmentType) {
        case AlignmentType.bitPacked:
          return new BitPackedScriber(false);
        case AlignmentType.byteAligned:
          return new ByteAlignedScriber();
        case AlignmentType.preCompress:
          if (m_channellingScriberConstructor != null) {
            try {
              return (Scriber)m_channellingScriberConstructor.Invoke(ARGS_PRECOMPRESS);
            }
            catch (Exception) {
            }
          }
          break;
        case AlignmentType.compress:
          if (m_channellingScriberConstructor != null) {
            try {
              return (Scriber)m_channellingScriberConstructor.Invoke(ARGS_COMPRESS);
            }
            catch (Exception) {
            }
          }
          break;
      }
      return null;
    }

    internal static BitPackedScriber createHeaderOptionsScriber() {
      return new BitPackedScriber(true);
    }

  }

}