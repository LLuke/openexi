package com.sumerogi.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;

import com.sumerogi.proc.io.Scriber;
import com.sumerogi.proc.io.ValueScriber;

final class ScriberValueHolder {

  private int m_localName;
  
  private final ValueScriber m_valueScriber;
  private final Object m_value;

  ScriberValueHolder(int localName, Object value, ValueScriber valueScriber) {
    m_localName = localName;
    m_value = value;
    m_valueScriber = valueScriber;
  }
  
  void scribeValue(OutputStream outputStream, Scriber scriber) throws IOException {
    m_valueScriber.doScribe(m_value, m_localName, outputStream, scriber);
  }

}
