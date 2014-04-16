package org.openexi.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.ValueScriber;

final class ScriberValueHolder {

  private int m_localName;
  private int m_uri;
  private int m_tp;
  
  private final ValueScriber m_valueScriber;
  private final Object m_value;

  ScriberValueHolder(int localName, int uri, int tp, Object value, ValueScriber valueScriber) {
    m_localName = localName;
    m_uri = uri;
    m_tp = tp;
    m_value = value;
    m_valueScriber = valueScriber;
  }
  
  void scribeValue(OutputStream outputStream, Scriber scriber) throws IOException {
    m_valueScriber.doScribe(m_value, m_localName, m_uri, m_tp, outputStream, scriber);
  }

}
