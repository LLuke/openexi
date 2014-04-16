package org.openexi.sax;

import org.openexi.proc.io.BinaryDataSink;
import org.xml.sax.SAXException;

public interface BinaryDataHandler {

  /**
   * Mark the start of a binary value.
   */
  public BinaryDataSink startBinaryData(long totalSize) throws SAXException;

  /**
   * Writes a binary value where the schema expects a binary value.
   */
  public void binaryData(byte[] byteArray, int offset, int length, BinaryDataSink binaryDataSink) throws SAXException;
  
  /**
   * Mark the end of a binary value.
   */
  public void endBinaryData(BinaryDataSink binaryDataSink) throws SAXException;


}
