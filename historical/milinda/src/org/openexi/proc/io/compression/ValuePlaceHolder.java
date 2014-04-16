package org.openexi.proc.io.compression;

abstract class ValuePlaceHolder {

  protected final String m_localName;
  protected final String m_uri;
  protected final int m_tp;

  protected ValuePlaceHolder(String localName, String uri, int tp) {
    m_localName = localName;
    m_uri = uri;
    m_tp = tp;
  }

}
