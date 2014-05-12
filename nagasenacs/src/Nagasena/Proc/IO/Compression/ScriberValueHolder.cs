using System.IO;

namespace Nagasena.Proc.IO.Compression {

  internal sealed class ScriberValueHolder {

    private int m_localName;
    private int m_uri;
    private int m_tp;

    private readonly ValueScriber m_valueScriber;
    private readonly object m_value;

    internal ScriberValueHolder(int localName, int uri, int tp, object value, ValueScriber valueScriber) {
      m_localName = localName;
      m_uri = uri;
      m_tp = tp;
      m_value = value;
      m_valueScriber = valueScriber;
    }

    internal void scribeValue(Stream outputStream, Scriber scriber) {
      m_valueScriber.doScribe(m_value, m_localName, m_uri, m_tp, outputStream, scriber);
    }

  }

}