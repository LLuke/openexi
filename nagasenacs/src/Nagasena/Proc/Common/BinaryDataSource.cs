namespace Nagasena.Proc.Common {

  public sealed class BinaryDataSource {

    private byte[] m_byteArray;
    private int m_startIndex;
    private int m_length;

    private long m_n_remainingBytes;

    private IBinaryValueScanner m_scanner;

    public byte[] ByteArray {
      get {
        return m_byteArray;
      }
    }

    public int StartIndex {
      get {
        return m_startIndex;
      }
    }

    public int Length {
      get {
        return m_length;
      }
    }

    public long RemainingBytesCount {
      get {
        return m_n_remainingBytes;
      }
    }

    internal void setValues(byte[] byteArray, int startIndex, int length, IBinaryValueScanner scanner, long n_remainingBytes) {
      m_byteArray = byteArray;
      m_startIndex = startIndex;
      m_length = length;
      m_n_remainingBytes = n_remainingBytes;
      m_scanner = scanner;
    }

    public bool hasNext() {
      return m_n_remainingBytes > 0;
    }

    public int next() {
      if (m_n_remainingBytes > 0) {
        m_scanner.scan(m_n_remainingBytes, this);
        return m_length;
      }
      return -1;
    }

  }

}