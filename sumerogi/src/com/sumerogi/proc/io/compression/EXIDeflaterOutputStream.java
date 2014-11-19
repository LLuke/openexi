package com.sumerogi.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;

import java.util.zip.Deflater;

final class EXIDeflaterOutputStream extends OutputStream {
  
  private final OutputStream m_outputStream;

  private final Deflater m_deflater;
  
  private final byte[] m_bytesIn;
  private int m_n_bytesIn;
  
  private final byte[] m_bytesOut;
  
  public EXIDeflaterOutputStream(OutputStream outputStream, Deflater deflater) {
    m_outputStream = outputStream;
    deflater.reset();
    m_deflater = deflater; 
    
    m_bytesIn  = new byte[65536];
    m_n_bytesIn = 0;    
    m_bytesOut = new byte[8192];
  }

  @Override
  public void flush() throws IOException {
    flushInput();
    m_outputStream.flush();
  }
  
  @Override
  public void close() throws IOException {
    m_outputStream.close();
  }
  
  @Override
  public void write(int bt) throws IOException {
    m_bytesIn[m_n_bytesIn] = (byte)bt;
    if (++m_n_bytesIn == m_bytesIn.length) {
      flushInput();
    }
  }
  
  private void flushInput() throws IOException {
    final long initialBytesRead = m_deflater.getBytesRead();
    m_deflater.setInput(m_bytesIn, 0, m_n_bytesIn);
    int n_bytesOut;
    do {
      if ((n_bytesOut = m_deflater.deflate(m_bytesOut)) != 0) { 
        for (int i = 0; i < n_bytesOut; i++) {
          m_outputStream.write(0xFF & m_bytesOut[i]);
        }
      }
      else
        break;
    }
    while (true);
    final int bytesRead = (int)(m_deflater.getBytesRead() - initialBytesRead);
    if (bytesRead == m_n_bytesIn)
      m_n_bytesIn = 0;
    else if (bytesRead != 0) {
      m_n_bytesIn -= bytesRead;
      System.arraycopy(m_bytesIn, bytesRead, m_bytesIn, 0, m_n_bytesIn);
    }
  }

  void resetDeflater() throws IOException {
    if (m_n_bytesIn != 0)
      flushInput();
    m_deflater.setInput(m_bytesIn, 0, m_n_bytesIn);
    m_deflater.finish();
    while (!m_deflater.finished()) {
      int len = m_deflater.deflate(m_bytesOut);
      for (int i = 0; i < len; i++) {
        m_outputStream.write(0xFF & m_bytesOut[i]);
      }
    }
    m_deflater.reset();
  }
  
}
