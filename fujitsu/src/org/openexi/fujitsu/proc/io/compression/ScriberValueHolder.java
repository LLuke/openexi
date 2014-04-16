package org.openexi.fujitsu.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;

import org.openexi.fujitsu.proc.io.ValueScriber;

class ScriberValueHolder extends ValuePlaceHolder {

  private final ValueScriber m_valueScriber;
  private final Object m_value;

  ScriberValueHolder(String localName, String uri, int tp, Object value, ValueScriber valueScriber) {
    super(localName, uri, tp);
    m_value = value;
    m_valueScriber = valueScriber;
  }
  
  void scribeValue(OutputStream outputStream) throws IOException {
    m_valueScriber.doScribe(m_value, m_localName, m_uri, m_tp, outputStream);
  }

}
