package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.openexi.proc.common.QName;
import org.openexi.schema.EXISchema;

final class ListValueScriber extends ValueScriberBase {

  private final ArrayList<String> stringItems;
  private final StringBuffer stringBuffer; 

  public ListValueScriber() {
    super((QName)null);
    stringItems = new ArrayList<String>();
    stringBuffer = new StringBuffer();
  }
  
  @Override
  public short getCodecID() {
    return Scriber.CODEC_LIST;
  }

  @Override
  public int getBuiltinRCS(int simpleType, Scriber scriber) {
    final EXISchema schema = scriber.schema;
    assert schema.getVarietyOfSimpleType(simpleType) == EXISchema.LIST_SIMPLE_TYPE;
    
    final int itemType = schema.getItemTypeOfListSimpleType(simpleType);
    
    final ValueScriber itemValueScriber;
    itemValueScriber = scriber.getValueScriber(itemType);
    
    return itemValueScriber.getBuiltinRCS(itemType, scriber);
  }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean process(String value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
    assert schema.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
    
    final int itemType;
    itemType = schema.getItemTypeOfListSimpleType(tp);

    stringItems.clear();
    final int limit = value.length();
    
    int pos;
    boolean isWhitespaces;
    for (pos = 0, isWhitespaces = true; pos < limit; pos++) {
      final char c;
      switch (c = value.charAt(pos)) {
        case '\t':
        case '\n':
        case '\r':
        case ' ':
          if (!isWhitespaces) {
            stringItems.add(stringBuffer.toString());
            stringBuffer.setLength(0);
            isWhitespaces = true;
          }
          break;
        default:
          if (isWhitespaces) {
            isWhitespaces = false;
          }
          stringBuffer.append(c);
          break;
      }
    }
    if (!isWhitespaces) {
      assert stringBuffer.length() > 0;
      stringItems.add(stringBuffer.toString());
      stringBuffer.setLength(0);
    }
    
    final ValueScriber itemValueScriber;
    itemValueScriber = scriber.getValueScriber(itemType);

    final int n_items;
    if ((n_items = stringItems.size()) != 0) {
      if (scribble.listOfScribbles == null) {
        scribble.listOfScribbles = new Scribble[n_items];
        for (int i = 0; i < n_items; i++) {
          scribble.listOfScribbles[i] = new Scribble();
        }
      }
      else {
        final int length;
        if ((length = scribble.listOfScribbles.length) < n_items) {
          int newLength = length;
          do {
            newLength <<= 1;
          } while (newLength < n_items);
          final Scribble[] newList = new Scribble[newLength];
          System.arraycopy(scribble.listOfScribbles, 0, newList, 0, length);
          for (int i = length; i < newLength; i++)
            newList[i] = new Scribble();
          scribble.listOfScribbles = newList; 
        }
      }
      int i = 0;
      do {
        final Scribble ith = scribble.listOfScribbles[i];
        if (!itemValueScriber.process(stringItems.get(i), itemType, schema, ith, scriber)) {
          return false;
        }
      } while (++i < n_items);
    }
    scribble.intValue1 = n_items;
    scribble.valueScriber = itemValueScriber;
    return true;
  }
  
  @Override
  public void scribe(String value, Scribble scribble, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException {
    scribeListValue(scribble.intValue1, scribble.listOfScribbles, scribble.valueScriber, localName, uri, tp, channelStream, scriber);
  }
  
  ////////////////////////////////////////////////////////////
  
  private static class ListValue {
    int n_items;
    Scribble[] listOfScribbles;
    ValueScriber itemValueScriber;
    ListValue(int n_items, Scribble[] listOfScribbles, ValueScriber itemValueScriber) {
      this.n_items = n_items;
      this.listOfScribbles = listOfScribbles;
      this.itemValueScriber = itemValueScriber;
    }
  }

  @Override
  public Object toValue(String value, Scribble scribble, Scriber scriber) {
    final int n_items = scribble.intValue1;
    final Scribble[] listOfScribbles = new Scribble[n_items];
    for (int i = 0; i < n_items; i++) {
      listOfScribbles[i] = new Scribble(scribble.listOfScribbles[i]);
    }
    final ValueScriber itemValueScriber = scribble.valueScriber;
    return new ListValue(n_items, listOfScribbles, itemValueScriber);
  }
 
  @Override
  public void doScribe(Object value, int localName, int uri, int tp, OutputStream channelStream, Scriber scriber) throws IOException  {
    final ListValue listValue = (ListValue)value;
    scribeListValue(listValue.n_items, listValue.listOfScribbles, listValue.itemValueScriber, localName, uri, tp, channelStream, scriber);
  }

  ////////////////////////////////////////////////////////////

  private void scribeListValue(int n_items, Scribble[] listOfScribbles, ValueScriber valueScriber, 
      int localName, int uri, int tp, OutputStream ostream, Scriber scriber) throws IOException {
    final EXISchema schema = scriber.schema;
    scriber.writeUnsignedInteger32(n_items, ostream);
    int itemType = schema.getItemTypeOfListSimpleType(tp);
    int i;
    for (i = 0; i < n_items; i++) {
      final Scribble ith = listOfScribbles[i];
      valueScriber.scribe(ith.stringValue1, ith, localName, uri, itemType, ostream, scriber);
    }
  }

}
