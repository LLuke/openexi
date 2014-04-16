package org.openexi.proc.io.compression;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

class EXIInflaterInputStream extends FilterInputStream {
  
  private final Inflater m_inflater;

  private final byte[] outputBuffer;
  private int outputOffset;
  private int outputLimit;

  private final byte[] inputBuffer;
  private int inputLength;
  
  public EXIInflaterInputStream(InputStream inputStream, Inflater inflater, int bufSize) {
    super(inputStream);
    m_inflater = inflater;
    inputBuffer = new byte[bufSize];
    inputLength = 0;
    outputBuffer = new byte[bufSize];
    outputOffset = outputLimit = 0;
  }
  
  @Override
  public int read() throws IOException {
    while (outputOffset == outputLimit && !m_inflater.finished()) {
      outputOffset = 0;
      try {
        final int remain;
        m_inflater.setInput(inputBuffer, 0, inputLength);
        outputLimit = m_inflater.inflate(outputBuffer);
        if ((remain = m_inflater.getRemaining()) != 0 && remain != inputLength) {
          final int inputOffset;
          inputOffset = inputLength - remain;
          inputLength = remain;
          int i, pos;
          for (i = 0, pos = inputOffset; i < inputLength; i++, pos++) {
            inputBuffer[i] = inputBuffer[pos];
          }
        }
        else { // remain == 0 || remain == inputLength
          if (outputLimit == 0 && remain == 0) {
            assert !m_inflater.finished();
            if ((inputLength = super.read(inputBuffer, 0, inputBuffer.length)) < 0) {
              inputLength = 0;
              return -1;
            }
          }
          else
            inputLength = remain;
        }
      }
      catch (DataFormatException dfe) {
        throw new IOException(dfe.getMessage());
      }
    }
    return outputOffset != outputLimit ? outputBuffer[outputOffset++] & 0x00ff : -1;
  }
  
  void resetInflator() throws IOException {
    for (int bt = 0; !m_inflater.finished() && bt != -1;) {
      bt = read();
    }
    m_inflater.reset();
  }

  void end() {
  }

}
