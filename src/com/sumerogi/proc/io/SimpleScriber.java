package com.sumerogi.proc.io;

import java.io.IOException;
import java.io.OutputStream;

import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.grammars.EventCodeTuple;

abstract class SimpleScriber extends Scriber {
  
  SimpleScriber() {
  }
  
  @Override
  public void setBlockSize(int blockSize) {
    // Do nothing.
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scriber Functions
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  public final void writeEventType(EventType eventType) throws IOException {
    EventCode[] path;
    path = eventType.getItemPath();

    int i, len;
    EventCode item = path[0].parent;
    for (i = 0, len = path.length; i < len; i++) {
      EventCodeTuple parent = (EventCodeTuple)item;
      item = path[i];
      final int width;
      if ((width = parent.width) != 0) {
        writeNBitUnsigned(parent.reversed ? parent.itemsCount - 1 - item.position : item.position, width, (OutputStream)null);
      }
    }
  }
  
  @Override
  public int writeName(String name, EventType eventType) throws IOException {
    final byte itemType = eventType.itemType;
    final int localNameId;
    switch (itemType) {
      case EventType.ITEM_STRING_VALUE_WILDCARD:
      case EventType.ITEM_NUMBER_VALUE_WILDCARD:
      case EventType.ITEM_BOOLEAN_VALUE_WILDCARD:
      case EventType.ITEM_NL_WC:
      case EventType.ITEM_START_ARRAY_WILDCARD:
      case EventType.ITEM_START_OBJECT_WILDCARD:
        localNameId = writeLocalName(name, stringTable, (OutputStream)null);
        break;
      case EventType.ITEM_STRING_VALUE_NAMED:
      case EventType.ITEM_NUMBER_VALUE_NAMED:
      case EventType.ITEM_BOOLEAN_VALUE_NAMED:
      case EventType.ITEM_NL_NAMED:
      case EventType.ITEM_START_OBJECT_NAMED:
      case EventType.ITEM_START_ARRAY_NAMED:
        localNameId = eventType.getNameId();
        break;
      default:
        localNameId = -1;
        assert false;
    }
    return localNameId;
  }

}
