namespace Nagasena.Proc.Common {
  /// <summary>
  /// AlignmentType represents one of the following bit alignment 
  /// styles so as to provide an extra degree of control over the
  /// way information is stored in EXI format.
  /// <br/>
  /// <ul><li><i>bitPacked</i> is the default setting. Data are
  /// stored in the fewest number of bits in sequential streams
  /// that cross byte barriers. Conceptually, 8 boolean values
  /// might be stored in a single byte, for example.</li><br/>
  /// <li><i>byteAligned</i> stores data using EXI tags with 
  /// byte barriers intact. Byte-aligned files are useful for
  /// troubleshooting, because the data are often human-readable
  /// when the values are literally encoded as strings. It
  /// is not meant for data transfer, as the file has not been 
  /// optimized.</li><br/>
  /// <li><i>preCompress</i> is a byte-aligned format that arranges
  /// the data into channels. It is intended for use cases where
  /// file compression is part of the transfer process, so as
  /// not to perform the compression step twice.</li><br/>
  /// <li><i>compress</i> is not a bit alignment in and of itself,
  /// but the Deflate algorithm requires that files be byte-aligned.
  /// When compression is selected, byte-alignment is used, the
  /// data are arranged in channels, and the file is compressed
  /// at the end of processing.</li>
  /// </ul>
  /// </summary>
  public enum AlignmentType {

    bitPacked,
    byteAligned,
    preCompress,
    compress

  }
}