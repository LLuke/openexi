using BinaryDataSink = Nagasena.Proc.IO.BinaryDataSink;
using SAXException = Org.System.Xml.Sax.SaxException;

namespace Nagasena.Sax {

  public interface BinaryDataHandler {

    /// <summary>
    /// Mark the start of a binary value.
    /// </summary>
    BinaryDataSink startBinaryData(long totalSize);

    /// <summary>
    /// Writes a binary value where the schema expects a binary value.
    /// </summary>
    void binaryData(byte[] byteArray, int offset, int length, BinaryDataSink binaryDataSink);

    /// <summary>
    /// Mark the end of a binary value.
    /// </summary>
    void endBinaryData(BinaryDataSink binaryDataSink);

  }

}