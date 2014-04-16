using System;
using System.Diagnostics;
using System.Collections.Generic;
using System.IO;
using System.Text;

using QName = Nagasena.Proc.Common.QName;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal sealed class ListValueScriber : ValueScriberBase {

    private readonly List<string> stringItems;
    private readonly StringBuilder stringBuffer;

    public ListValueScriber() : base((QName)null) {
      stringItems = new List<string>();
      stringBuffer = new StringBuilder();
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_LIST;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      EXISchema schema = scriber.schema;
      Debug.Assert(schema.getVarietyOfSimpleType(simpleType) == EXISchema.LIST_SIMPLE_TYPE);

      int itemType = schema.getItemTypeOfListSimpleType(simpleType);

      ValueScriber itemValueScriber;
      itemValueScriber = scriber.getValueScriber(itemType);

      return itemValueScriber.getBuiltinRCS(itemType, scriber);
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      Debug.Assert(schema.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE);

      int itemType;
      itemType = schema.getItemTypeOfListSimpleType(tp);

      stringItems.Clear();
      int limit = value.Length;

      int pos;
      bool isWhitespaces;
      for (pos = 0, isWhitespaces = true; pos < limit; pos++) {
        char c;
        switch (c = value[pos]) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            if (!isWhitespaces) {
              stringItems.Add(stringBuffer.ToString());
              stringBuffer.Length = 0;
              isWhitespaces = true;
            }
            break;
          default:
            if (isWhitespaces) {
              isWhitespaces = false;
            }
            stringBuffer.Append(c);
            break;
        }
      }
      if (!isWhitespaces) {
        Debug.Assert(stringBuffer.Length > 0);
        stringItems.Add(stringBuffer.ToString());
        stringBuffer.Length = 0;
      }

      ValueScriber itemValueScriber;
      itemValueScriber = scriber.getValueScriber(itemType);

      int n_items;
      if ((n_items = stringItems.Count) != 0) {
        if (scribble.listOfScribbles == null) {
          scribble.listOfScribbles = new Scribble[n_items];
          for (int i = 0; i < n_items; i++) {
            scribble.listOfScribbles[i] = new Scribble();
          }
        }
        else {
          int length;
          if ((length = scribble.listOfScribbles.Length) < n_items) {
            int newLength = length;
            do {
              newLength <<= 1;
            }
            while (newLength < n_items);
            Scribble[] newList = new Scribble[newLength];
            Array.Copy(scribble.listOfScribbles, 0, newList, 0, length);
            for (int i = length; i < newLength; i++) {
              newList[i] = new Scribble();
            }
            scribble.listOfScribbles = newList;
          }
        }
        int _i = 0;
        do {
          Scribble ith = scribble.listOfScribbles[_i];
          if (!itemValueScriber.process(stringItems[_i], itemType, schema, ith, scriber)) {
            return false;
          }
        }
        while (++_i < n_items);
      }
      scribble.intValue1 = n_items;
      scribble.valueScriber = itemValueScriber;
      return true;
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeListValue(scribble.intValue1, scribble.listOfScribbles, scribble.valueScriber, localName, uri, tp, channelStream, scriber);
    }

    ////////////////////////////////////////////////////////////

    private class ListValue {
      internal int n_items;
      internal Scribble[] listOfScribbles;
      internal ValueScriber itemValueScriber;
      internal ListValue(int n_items, Scribble[] listOfScribbles, ValueScriber itemValueScriber) {
        this.n_items = n_items;
        this.listOfScribbles = listOfScribbles;
        this.itemValueScriber = itemValueScriber;
      }
    }

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      int n_items = scribble.intValue1;
      Scribble[] listOfScribbles = new Scribble[n_items];
      for (int i = 0; i < n_items; i++) {
        listOfScribbles[i] = new Scribble(scribble.listOfScribbles[i]);
      }
      ValueScriber itemValueScriber = scribble.valueScriber;
      return new ListValue(n_items, listOfScribbles, itemValueScriber);
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      ListValue listValue = (ListValue)value;
      scribeListValue(listValue.n_items, listValue.listOfScribbles, listValue.itemValueScriber, localName, uri, tp, channelStream, scriber);
    }

    ////////////////////////////////////////////////////////////

    private void scribeListValue(int n_items, Scribble[] listOfScribbles, ValueScriber valueScriber, int localName, int uri, int tp, Stream ostream, Scriber scriber) {
      EXISchema schema = scriber.schema;
      scriber.writeUnsignedInteger32(n_items, ostream);
      int itemType = schema.getItemTypeOfListSimpleType(tp);
      int i;
      for (i = 0; i < n_items; i++) {
        Scribble ith = listOfScribbles[i];
        valueScriber.scribe(ith.stringValue1, ith, localName, uri, itemType, ostream, scriber);
      }
    }

  }

}