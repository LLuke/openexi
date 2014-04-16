namespace Nagasena.Proc.IO {

  /// <summary>
  /// BinaryDataSink represents a sink that accepts successive chunks of binary data.
  /// </summary>
  public interface BinaryDataSink {

    /// <summary>@y.exclude</summary>
    /// <exception cref="ScriberRuntimeException"></exception>
    void startBinaryData(long totalSize, Scribble scribble, Scriber scriber);

    /// <summary>@y.exclude</summary>
    void binaryData(byte[] byteArray, int startIndex, int length, Scribble scribble, Scriber scriber);

    /// <summary>@y.exclude</summary>
    /// <exception cref="ScriberRuntimeException"></exception>
    void endBinaryData(Scribble scribble, int localName, int uri, Scriber scriber);
  }

}