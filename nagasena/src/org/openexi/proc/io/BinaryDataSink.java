package org.openexi.proc.io;

import java.io.IOException;

/**
 * BinaryDataSink represents a sink that accepts successive chunks of binary data.
 */
public interface BinaryDataSink {
  
  /** @y.exclude */
  public void startBinaryData(long totalSize, Scribble scribble, Scriber scriber) throws ScriberRuntimeException, IOException;
  
  /** @y.exclude */
  public void binaryData(byte[] byteArray, int startIndex, int length, Scribble scribble, Scriber scriber) throws IOException;
  
  /** @y.exclude */
  public void endBinaryData(Scribble scribble, int localName, int uri, Scriber scriber) throws ScriberRuntimeException, IOException;
  
}
