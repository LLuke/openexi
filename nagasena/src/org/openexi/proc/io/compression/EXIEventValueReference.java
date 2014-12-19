package org.openexi.proc.io.compression;

import java.io.IOException;
import java.io.InputStream;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.io.BinaryValueScanner;
import org.openexi.proc.io.ValueScanner;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;

public final class EXIEventValueReference implements EventDescription {

  byte eventKind;
  
  EventType eventType;

  String prefix;
  String uri;
  String name;
  
  int uriId;
  int nameId;
  int tp;
  
  Characters text;
  BinaryDataSource binaryData;
  
  EXIEventValueReference() {
    eventKind = -1;
    eventType = null;
    prefix = null;
    uri = null;
    name = null;
    uriId = -1;
    nameId = -1;
    tp = EXISchema.NIL_NODE;
    text = null;
    binaryData = null;
  }
  
  void scanText(ChannellingScanner scanner, boolean binaryDataEnabled, InputStream istream) throws IOException {
    final ValueScanner valueScanner = scanner.getValueScanner(tp);
    if (binaryDataEnabled) {
      final short codecId = valueScanner.getCodecID();
      if (codecId == Apparatus.CODEC_BASE64BINARY || codecId == Apparatus.CODEC_HEXBINARY) {
        binaryData = ((BinaryValueScanner)valueScanner).scan(-1, (BinaryDataSource)null);
        eventKind = EventDescription.EVENT_BLOB;
        return;
      }
    }
    text = valueScanner.scan(nameId, uriId, tp);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventDescription interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventKind() {
    return eventKind;
  }

  public int getURIId() {
    return uriId;
  }
  
  public int getNameId() {
    return nameId;
  }

  public String getPrefix() {
    return prefix;
  }
  
  public Characters getCharacters() {
    return text;
  }
  
  public BinaryDataSource getBinaryDataSource() {
    return binaryData;
  }

  public EventType getEventType() {
    return eventType;
  }
  
  public String getName() {
    return name;
  }

  public String getURI() {
    return uri;
  }

}
