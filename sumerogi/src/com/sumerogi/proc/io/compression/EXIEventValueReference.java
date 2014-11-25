package com.sumerogi.proc.io.compression;

import java.io.IOException;
import java.io.InputStream;

import com.sumerogi.proc.common.EventDescription;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.io.ValueScanner;
import com.sumerogi.schema.Characters;

public final class EXIEventValueReference implements EventDescription {

  byte eventKind;
  
  EventType eventType;

  String name;
  
  int channelName;
  int nameId;
  
  Characters text;
  
  EXIEventValueReference() {
    eventKind = -1;
    eventType = null;
    name = null;
    channelName = StringTable.NAME_NONE;
    nameId = StringTable.NAME_NONE;
    text = null;
  }
  
  void scanText(ChannellingScanner scanner, InputStream istream) throws IOException {
    ValueScanner valueScanner = null; 
    switch (eventType.itemType) {
      case EventType.ITEM_STRING_VALUE_ANONYMOUS:
      case EventType.ITEM_STRING_VALUE_WILDCARD:
      case EventType.ITEM_STRING_VALUE_NAMED:
        valueScanner = scanner.stringValueScanner;
        break;
      case EventType.ITEM_NUMBER_VALUE_ANONYMOUS:
      case EventType.ITEM_NUMBER_VALUE_WILDCARD:
      case EventType.ITEM_NUMBER_VALUE_NAMED:
        valueScanner = scanner.numberValueScanner;
        break;
      case EventType.ITEM_BOOLEAN_VALUE_ANONYMOUS:
      case EventType.ITEM_BOOLEAN_VALUE_WILDCARD:
      case EventType.ITEM_BOOLEAN_VALUE_NAMED:
        valueScanner = scanner.booleanValueScanner;
        break;
      case EventType.ITEM_NULL_ANONYMOUS:
      case EventType.ITEM_NULL_WILDCARD:
      case EventType.ITEM_NULL_NAMED:
        break;
      default:
        assert false;
        break;
    }
    text = valueScanner != null ? valueScanner.scan(channelName) : Characters.CHARACTERS_NULL;
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
