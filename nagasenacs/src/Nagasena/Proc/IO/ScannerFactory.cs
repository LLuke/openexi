using System;
using System.Diagnostics;
using System.Reflection;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;

namespace Nagasena.Proc.IO {


  public sealed class ScannerFactory {

    private static readonly ConstructorInfo m_channellingScannerConstructor;

    private static readonly Object[] ARGS_PRECOMPRESS = new Object[] { false };
    private static readonly Object[] ARGS_COMPRESS = new Object[] { true };

    static ScannerFactory() {
      Type channellingScannerType = Type.GetType("Nagasena.Proc.IO.Compression.ChannellingScanner");
      m_channellingScannerConstructor = channellingScannerType == null ? null :
        channellingScannerType.GetConstructor(new Type[] { typeof(bool) });
    }

    /// <summary>
    /// Create a scanner for processing the EXI body of a stream. </summary>
    /// <param name="alignmentType"> </param>
    /// <param name="inflatorBufSize"> </param>
    /// <returns>a scanner, or null if AlignmentType.preCompress or AlignmentType.compress was 
    /// specified as alignmentType and EXI compression classes are not available.
    /// </returns>
    public static Scanner createScanner(AlignmentType alignmentType, int inflatorBufSize) {
      Scanner scanner;
      switch (alignmentType) {
        case AlignmentType.bitPacked:
          scanner = new BitPackedScanner(false);
          break;
        case AlignmentType.byteAligned:
          scanner = new ByteAlignedScanner();
          break;
        case AlignmentType.preCompress:
          if (m_channellingScannerConstructor != null) {
            try {
              scanner = (Scanner)m_channellingScannerConstructor.Invoke(ARGS_PRECOMPRESS);
            }
            catch (Exception) {
              return null;
            }
          }
          else {
            return null;
          }
          break;
        case AlignmentType.compress:
          if (m_channellingScannerConstructor != null) {
            try {
              scanner = (Scanner)m_channellingScannerConstructor.Invoke(ARGS_COMPRESS);
            }
            catch (Exception) {
              return null;
            }
          }
          else {
            return null;
          }
          scanner.init(inflatorBufSize);
          break;
        default:
          return null;
      }
      return scanner;
    }

    /// <summary>
    /// Create a scanner for processing the header option document of a stream.
    /// </summary>
    internal static BitPackedScanner createHeaderOptionsScanner() {
      return new BitPackedScanner(true);
    }

  }

}