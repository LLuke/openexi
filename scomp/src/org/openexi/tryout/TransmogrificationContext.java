package org.openexi.tryout;

import javax.xml.parsers.SAXParserFactory;

import org.openexi.schema.EXISchema;

interface TransmogrificationContext {

  EXISchema getEXISchema();
  boolean useCompression();
  
  String getSystemId();

  long getFileSize();
  
  SAXParserFactory getSAXParserFactory();
  
  void setDone(byte[] exiStream);
  
}
