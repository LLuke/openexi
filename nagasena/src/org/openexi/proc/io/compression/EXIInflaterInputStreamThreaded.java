package org.openexi.proc.io.compression;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class EXIInflaterInputStreamThreaded extends FilterInputStream {

  private final Inflater m_inflater;

  private final OutputBuffer[] alternateBuffers;

  private final EXIInflator m_exiInflator;

  private int m_bufferIndex;
  
  private Exception m_inflatorException;
  
  private final byte[] m_bytes;
  private int m_bufLen;
  private int m_curPos;

  public EXIInflaterInputStreamThreaded(InputStream inputStream, Inflater inflater, int bufSize) {
    super(inputStream);
    m_inflater = inflater;
    alternateBuffers = new OutputBuffer[2];
    alternateBuffers[0] = new OutputBuffer(bufSize * 200);
    alternateBuffers[1] = new OutputBuffer(bufSize * 200);
    
    m_bufferIndex = 0;
    
    m_bytes = new byte[8192];
    m_curPos = m_bufLen = 0;
    
    m_exiInflator = new EXIInflator(bufSize);
    new Thread(m_exiInflator).start();
  }
  
  private int fill() throws IOException {
    assert m_curPos == m_bufLen;
    m_curPos = 0;
    if ((m_bufLen = read(m_bytes, 0, m_bytes.length)) == -1) {
      m_bufLen = 0;
      return -1; /** EOF */
    }
    return m_bufLen;
  }

  @Override
  public int read() throws IOException {
    if (m_curPos == m_bufLen && fill() == -1) {
      return -1;
    }
    return m_bytes[m_curPos++] & 0x00ff;
  }

  @Override
  public void close() throws IOException {
    for (int i = 0; i < 2; i++) {
      final OutputBuffer outputBuffer;
      synchronized (outputBuffer = alternateBuffers[i]) {
        outputBuffer.quit = true;
        outputBuffer.notify();
      }
    }
    super.close();
  }
  
  @Override
  public int read(byte[] b, final int off, final int len) throws IOException {
    final int limit = off + len;
    int offset = off;
    while (true) {
      OutputBuffer outputBuffer;
      synchronized (outputBuffer = alternateBuffers[m_bufferIndex]) {
        try {
          Status status;
          while ((status = outputBuffer.status) == Status.doInflate) {
            ++outputBuffer.waitCount;
            outputBuffer.wait();
            --outputBuffer.waitCount;
          }
          switch (status) {
            case dataAvailable:
              break;
            case none:
              return -1;
            case errorFormat:
              final DataFormatException dataFormatException = (DataFormatException)m_inflatorException;
              m_inflatorException = null;
              outputBuffer.status = Status.none;
              outputBuffer.quit = true;
              outputBuffer.notify();
              throw new IOException(dataFormatException.getMessage());
            case errorInteruption:
              final InterruptedException interruptedException = (InterruptedException)m_inflatorException;
              m_inflatorException = null;
              outputBuffer.status = Status.none;
              outputBuffer.quit = true;
              throw interruptedException;
            case errorIO:
              final IOException ioe = (IOException)m_inflatorException;
              m_inflatorException = null;
              outputBuffer.status = Status.none;
              outputBuffer.quit = true;
              outputBuffer.notify();
              throw ioe;
            default:
              assert false;
              break;
          }
          assert outputBuffer.status == Status.dataAvailable;
          while (outputBuffer.offset != outputBuffer.limit && offset != limit) {
            b[offset++] = outputBuffer.bts[outputBuffer.offset++];
          }
          if (outputBuffer.offset == outputBuffer.limit) {
            if (outputBuffer.endOfStream)
              outputBuffer.status = Status.none;
            else {
              outputBuffer.status = Status.doInflate;
              m_bufferIndex = 1 - m_bufferIndex;
              if (outputBuffer.waitCount > 0)
                outputBuffer.notify();
            }
          }
          return offset - off;
        }
        catch (InterruptedException e) {
          outputBuffer.status = Status.none;
          outputBuffer.quit = true;
          outputBuffer.notify();
          throw new IOException(e.getMessage());
        }
      }
    }
  }
  
  private class EXIInflator implements Runnable {

    private final byte[] inputBuffer;
    private int inputOffset;
    private int inputLimit;
    
    private int outputBufferIndex;
    
    private final int m_quorum;
    
    public EXIInflator(int bufSize) {
      inputBuffer = new byte[bufSize];
      m_quorum = 199 * bufSize;
      inputOffset = inputLimit = 0;
      outputBufferIndex = 0;
    }
    
    public void run() {
      OutputBuffer outputBuffer = null;
      try {
          inflatorLife: 
          while (true) {
            synchronized (outputBuffer = alternateBuffers[outputBufferIndex]) {
              boolean quit = false;
              while (outputBuffer.status != Status.doInflate || (quit = outputBuffer.quit)) {
                ++outputBuffer.waitCount;
                outputBuffer.wait();
                --outputBuffer.waitCount;
              }
              if (quit)
                break inflatorLife;
              assert outputBuffer.offset == outputBuffer.limit; // i.e. deplete
              outputBuffer.offset = outputBuffer.limit = 0;
              while (outputBuffer.limit < m_quorum) {
                if (m_inflater.finished())
                  m_inflater.reset();
                final int n_inputBytes;
                if ((n_inputBytes = EXIInflaterInputStreamThreaded.super.read(inputBuffer, inputLimit, inputBuffer.length - inputLimit)) != -1)
                  inputLimit += n_inputBytes;
                else if ((inputLimit - inputOffset) == 0) {
                  m_inflater.reset();
                  outputBuffer.endOfStream = true;
                  outputBuffer.status = Status.dataAvailable;
                  if (outputBuffer.waitCount > 0)
                    outputBuffer.notify();
                  break inflatorLife;
                }
                m_inflater.setInput(inputBuffer, inputOffset, inputLimit - inputOffset);
                final int n_inflatedBytes;
                n_inflatedBytes = m_inflater.inflate(outputBuffer.bts, outputBuffer.limit, outputBuffer.bts.length - outputBuffer.limit);
                outputBuffer.limit += n_inflatedBytes;
                final int remain = m_inflater.getRemaining();
                if (remain == 0)
                  inputOffset = inputLimit = 0;
                else
                  inputOffset += (inputLimit - inputOffset) - remain;
              }
              outputBuffer.status = Status.dataAvailable;
              outputBufferIndex = 1 - outputBufferIndex;
              if (outputBuffer.waitCount > 0)
                outputBuffer.notify();
            }
          }
      }
      catch (DataFormatException dfe) {
        m_inflatorException = dfe;
        outputBuffer.status = Status.errorFormat;
      }
      catch (InterruptedException ie) {
        m_inflatorException = ie;
        outputBuffer.status = Status.errorInteruption;
      }
      catch (IOException ioe) {
        m_inflatorException = ioe;
        outputBuffer.status = Status.errorIO;
      }
    }
  }

  private class OutputBuffer {
    final byte[] bts;
    int offset;
    int limit;
    
    int waitCount;
    
    boolean quit;
    
    Status status;
    boolean endOfStream;
    
    OutputBuffer(int bufSize) {
      bts = new byte[bufSize];
      offset = limit = 0;
      waitCount = 0;
      quit = false;
      status = Status.doInflate;
      endOfStream = false;
    }
  }

  private static enum Status {
    none,
    doInflate,
    dataAvailable,
    errorFormat,
    errorInteruption,
    errorIO
  }

}
