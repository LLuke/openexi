package org.openexi.proc.io.compression;

import org.openexi.proc.common.BinaryDataSource;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventDescription;
import org.openexi.schema.Characters;

final class EXIEventElement implements EventDescription {

  String prefix;
  EventType eventType;
  
  public EXIEventElement() {
    prefix = null;
    eventType = null;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EXIEvent interface
  ///////////////////////////////////////////////////////////////////////////

  public final byte getEventKind() {
    return EventDescription.EVENT_SE;
  }

  public String getName() {
    return eventType.name;
  }

  public String getURI() {
    return eventType.uri;
  }

  public int getNameId() {
    return eventType.getNameId();
  }

  public int getURIId() {
    return eventType.getURIId();
  }

  public String getPrefix() {
    return prefix;
  }
  
  public final Characters getCharacters() {
    return null;
  }
  
  public BinaryDataSource getBinaryDataSource() {
    return null;
  }

  public final EventType getEventType() {
    return eventType;
  }

}
