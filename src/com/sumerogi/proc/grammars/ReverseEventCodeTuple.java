package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventType;

final class ReverseEventCodeTuple extends EventCodeTuple {
  
  private int m_initial_width;
  private int m_initial_itemsCount;

  public ReverseEventCodeTuple() {
    super(EventType.ITEM_TUPLE, true);
    eventCodes = new EventCode[16];
  }
  
  void checkPoint() {
    m_initial_width = width;
    m_initial_itemsCount = itemsCount;
  }
  
  void reset() {
    width = m_initial_width;
    itemsCount = m_initial_itemsCount;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeTuple interface
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  public EventCode getItem(int i) {
    return eventCodes[itemsCount - (i + 1)];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Methods
  ///////////////////////////////////////////////////////////////////////////

  void setInitialSoloTuple(EventCodeTuple tuple) {
    assert itemsCount == 0;
    if (itemsCount == eventCodes.length) {
      final EventCode[] items = new EventCode[itemsCount + 16];
      System.arraycopy(eventCodes, 0, items, 0, itemsCount);
      eventCodes = items;
    }
    eventCodes[itemsCount] = tuple;
    tuple.setParentalContext(itemsCount++, this);
    updateWidth();
    headItem = tuple;
  }
  
  void setInitialItems(EventType eventTypeEndObject, EventCode tuple) {
    assert itemsCount == 0 && (eventTypeEndObject.itemType == EventType.ITEM_END_OBJECT || eventTypeEndObject.itemType == EventType.ITEM_END_ARRAY); 
    if (itemsCount > eventCodes.length - 2) {
      final EventCode[] items = new EventCode[itemsCount + 16];
      System.arraycopy(eventCodes, 0, items, 0, itemsCount);
      eventCodes = items;
    }
    eventCodes[itemsCount] = tuple;
    eventCodes[itemsCount + 1] = eventTypeEndObject;
    // set position in reverse order
    tuple.setParentalContext(itemsCount++, this);
    eventTypeEndObject.setParentalContext(itemsCount++, this);
    updateWidth();
    eventTypeEndObject.computeItemPath();
    headItem = eventCodes[0];
  }

  void setInitialItems(EventType elementWildcard, EventType endDocument, EventCode tuple) {
    assert itemsCount == 0 && elementWildcard.itemType == EventType.ITEM_START_OBJECT_WILDCARD && 
      endDocument.itemType == EventType.ITEM_END_DOCUMENT;
    if (tuple != null) {
      if (itemsCount == eventCodes.length) {
        final EventCode[] items = new EventCode[itemsCount + 16];
        System.arraycopy(eventCodes, 0, items, 0, itemsCount);
        eventCodes = items;
      }
      eventCodes[itemsCount] = tuple;
      tuple.setParentalContext(itemsCount++, this);
    }
    if (itemsCount > eventCodes.length - 2) {
      final EventCode[] items = new EventCode[itemsCount + 16];
      System.arraycopy(eventCodes, 0, items, 0, itemsCount);
      eventCodes = items;
    }
    eventCodes[itemsCount] = endDocument;
    eventCodes[itemsCount + 1] = elementWildcard;
    // set position in reverse order
    endDocument.setParentalContext(itemsCount++, this);
    elementWildcard.setParentalContext(itemsCount++, this);
    updateWidth();
    endDocument.computeItemPath();
    elementWildcard.computeItemPath();
    headItem = eventCodes[0];
  }

  void addItem(EventType eventType) {
    if (itemsCount == eventCodes.length) {
      final EventCode[] items = new EventCode[itemsCount + 16];
      System.arraycopy(eventCodes, 0, items, 0, itemsCount);
      eventCodes = items;
    }
    eventCodes[itemsCount] = eventType;
    eventType.setParentalContext(itemsCount++, this);
    updateWidth();
    eventType.computeItemPath();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Private methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Update width.
   */
  private void updateWidth() {
    switch (itemsCount) {
      case 1:
        width = 0;
        break;
      case 2:
        width = 1;
        break;
      case 3:
      case 4:
        width = 2;
        break;
      case 5:
      case 6:
      case 7:
      case 8:
        width = 3;
        break;
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
        width = 4;
        break;
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
        width = 5;
        break;
      default:
        int n, _width;
        for (_width = 0, n = itemsCount - 1; n != 0; _width++) {
          n >>= 1;
        }
        width = _width;
        break;
    }
  }

}