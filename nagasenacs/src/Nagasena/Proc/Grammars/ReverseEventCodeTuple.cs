using System;
using System.Diagnostics;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;

namespace Nagasena.Proc.Grammars {

  internal sealed class ReverseEventCodeTuple : EventCodeTuple {

    private int m_initial_width;
    private int m_initial_itemsCount;

    public ReverseEventCodeTuple() : base(EventType.ITEM_TUPLE, true) {
      eventCodes = new EventCode[16];
    }

    internal void checkPoint() {
      m_initial_width = width;
      m_initial_itemsCount = itemsCount;
    }

    internal void reset() {
      width = m_initial_width;
      itemsCount = m_initial_itemsCount;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EventCodeTuple interface
    ///////////////////////////////////////////////////////////////////////////

    public override EventCode getItem(int i) {
      return eventCodes[itemsCount - (i + 1)];
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

    internal EventCodeTuple InitialSoloTuple {
      set {
        Debug.Assert(itemsCount == 0);
        if (itemsCount == eventCodes.Length) {
          EventCode[] items = new EventCode[itemsCount + 16];
          Array.Copy(eventCodes, 0, items, 0, itemsCount);
          eventCodes = items;
        }
        eventCodes[itemsCount] = value;
        value.setParentalContext(itemsCount++, this);
        updateWidth();
        headItem = value;
      }
    }

    internal void setInitialItems(EventType eventTypeEndElement, EventCode tuple) {
      Debug.Assert(itemsCount == 0 && eventTypeEndElement.itemType == EventType.ITEM_EE);
      if (itemsCount > eventCodes.Length - 2) {
        EventCode[] items = new EventCode[itemsCount + 16];
        Array.Copy(eventCodes, 0, items, 0, itemsCount);
        eventCodes = items;
      }
      eventCodes[itemsCount] = tuple;
      eventCodes[itemsCount + 1] = eventTypeEndElement;
      // set position in reverse order
      tuple.setParentalContext(itemsCount++, this);
      eventTypeEndElement.setParentalContext(itemsCount++, this);
      updateWidth();
      eventTypeEndElement.computeItemPath();
      headItem = eventCodes[0];
    }

    internal void setInitialItems(EventType elementWildcard, EventType endDocument, EventCode tuple) {
      Debug.Assert(itemsCount == 0 && elementWildcard.itemType == EventType.ITEM_SE_WC && endDocument.itemType == EventType.ITEM_ED);
      if (tuple != null) {
        if (itemsCount == eventCodes.Length) {
          EventCode[] items = new EventCode[itemsCount + 16];
          Array.Copy(eventCodes, 0, items, 0, itemsCount);
          eventCodes = items;
        }
        eventCodes[itemsCount] = tuple;
        tuple.setParentalContext(itemsCount++, this);
      }
      if (itemsCount > eventCodes.Length - 2) {
        EventCode[] items = new EventCode[itemsCount + 16];
        Array.Copy(eventCodes, 0, items, 0, itemsCount);
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

    internal void addItem(EventType eventType) {
      if (itemsCount == eventCodes.Length) {
        EventCode[] items = new EventCode[itemsCount + 16];
        Array.Copy(eventCodes, 0, items, 0, itemsCount);
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

    /// <summary>
    /// Update width.
    /// </summary>
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
}