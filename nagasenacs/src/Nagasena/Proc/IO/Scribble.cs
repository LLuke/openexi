using System;

using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  public sealed class Scribble {

    public int intValue1;
    public int intValue2;

    public long longValue;

    public bool booleanValue1;
    public bool booleanValue2;

    public string stringValue1;
    public string stringValue2;

    public Scribble[] listOfScribbles;
    public ValueScriber valueScriber;

    internal XSDateTime dateTime;
    public byte[] binaryValue;

    public Scribble() {
    }

    public Scribble(Scribble scribble) {
      this.intValue1 = scribble.intValue1;
      this.intValue2 = scribble.intValue2;
      this.longValue = scribble.longValue;
      this.booleanValue1 = scribble.booleanValue1;
      this.booleanValue2 = scribble.booleanValue2;
      this.stringValue1 = scribble.stringValue1;
      this.stringValue2 = scribble.stringValue2;
      this.dateTime = scribble.dateTime;
      byte[] srcBinaryValue;
      if ((srcBinaryValue = scribble.binaryValue) != null) {
        int len = srcBinaryValue.Length;
        byte[] binaryValue = new byte[len];
        Array.Copy(srcBinaryValue, 0, binaryValue, 0, len);
        this.binaryValue = binaryValue;
      }
      else {
        this.binaryValue = null;
      }
      // for now, there is no need for copying listOfScribbles and valueScriber fields.
    }

    public void clear() {
      intValue1 = intValue2 = 0;
      longValue = 0;
      booleanValue1 = booleanValue2 = false;
      stringValue1 = stringValue2 = null;
      listOfScribbles = null;
      valueScriber = null;
      dateTime = null;
      binaryValue = null;
    }

    /// <summary>
    /// Expand the byte array if necessary.
    /// </summary>
    public byte[] expandOctetArray(int n_maxBytes) {
      int length;
      if (binaryValue != null && (length = binaryValue.Length) != 0) {
        if (length < n_maxBytes) {
          do {
            length <<= 1;
          }
          while (length < n_maxBytes);
        }
        else {
          return binaryValue;
        }
      }
      else if (n_maxBytes != 0) {
        length = n_maxBytes;
      }
      else {
        return binaryValue;
      }
      return new byte[length];
    }

  }

}