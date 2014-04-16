using System.Diagnostics;

using EventCode = Nagasena.Proc.Common.EventCode;
using EventType = Nagasena.Proc.Common.EventType;

namespace Nagasena.Proc.Grammars {

  internal sealed class ArrayEventCodeTuple : EventCodeTuple {

    /// <summary>
    /// Constructor
    /// </summary>
    internal ArrayEventCodeTuple() : base(EventCode.ITEM_TUPLE, false) {
      eventCodes = null;
    }

    internal EventCode[] Items {
      set {
        Debug.Assert(value != null && value.Length > 0);
        eventCodes = value;
        itemsCount = eventCodes.Length;
        int n, _width;
        for (_width = 0, n = eventCodes.Length - 1; n > 0; _width++) {
          n >>= 1;
        }
        width = _width;
        for (int i = 0; i < value.Length; i++) {
          EventCode ith = value[i];
          // Productions of ITEM_SCHEMA_NIL do not participate in TypeEmpty grammars,
          // which renders an item null.
          if (ith != null) {
            ith.setParentalContext(i, this);
            if (ith.itemType != EventCode.ITEM_TUPLE) {
              ((EventType)ith).computeItemPath();
            }
          }
        }
        headItem = value[0];
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation of EventCodeTuple interface
    ///////////////////////////////////////////////////////////////////////////

    public override EventCode getItem(int i) {
      return eventCodes[i];
    }

  }
}