package com.sumerogi.proc.io.compression;

import java.io.IOException;
import java.io.InputStream;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.io.ValueScanner;
import com.sumerogi.schema.Characters;

public final class EXIEventValueReference implements EventDescription {

  byte eventKind;
  
  EventType eventType;

  String name;
  
  int nameId;
  
  Characters text;
  
  EXIEventValueReference() {
    eventKind = -1;
    eventType = null;
    name = null;
    nameId = -1;
    text = null;
  }
  
  void scanText(ChannellingScanner scanner, InputStream istream) throws IOException {
    final ValueScanner valueScanner = null; 
//        scanner.getValueScanner(tp);
    text = valueScanner.scan(nameId);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventDescription interface
  ///////////////////////////////////////////////////////////////////////////

  public byte getEventKind() {
    return eventKind;
  }

  public int getNameId() {
    return nameId;
  }

  public Characters getCharacters() {
    return text;
  }
  
  public EventType getEventType() {
    return eventType;
  }
  
  public String getName() {
    return name;
  }

}
