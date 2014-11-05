package com.sumerogi.proc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.grammars.GrammarCache;
import com.sumerogi.proc.io.BitPackedScriber;
import com.sumerogi.proc.io.Scribble;
import com.sumerogi.proc.io.Scriber;

public final class EJSONEncoder {
  
  private final GrammarCache m_grammarCache;

  private OutputStream m_outputStream;
  
  private JsonParser m_parser;
  
  private Scriber m_scriber;
  private Scribble m_scribble;

  public EJSONEncoder() {
    m_outputStream = null;
    m_scriber = new BitPackedScriber();
    m_scribble = new Scribble();
    m_grammarCache = new GrammarCache();
    
    setStringTable(Scriber.createStringTable(m_grammarCache));
  }
  
  private void reset() {
    // REVISIT: do I need to re-create a parser for every encode process?
    m_parser = null;
  }
  
  private void setStringTable(StringTable stringTable) {
    m_scriber.setStringTable(stringTable);
  }

  public final void setOutputStream(OutputStream ostream) {
    m_outputStream = ostream;
  }
  
  public void encode(String inputString) throws IOException {
    reset();
    m_parser = new JsonFactory().createParser(inputString);
    encodeDocument();
  }
  
  public void encode(InputStream inputStream) throws IOException {
    reset();
    m_parser = new JsonFactory().createParser(inputStream);
    encodeDocument();
  }
  
  private void encodeDocument() throws IOException {
    m_scriber.reset();
    m_scriber.setOutputStream(m_outputStream);
    m_grammarCache.getDocumentGrammar().init(m_scriber.currentState);
    
    EventTypeList eventTypes = m_scriber.getNextEventTypes();
    EventType eventType;
    if ((eventType = eventTypes.getSD()) != null)
      m_scriber.startDocument();
    else {
      // REVISIT: Throw an exception
      assert false;
    }
    m_scriber.writeEventType(eventType);

    JsonToken token;
    if ((token = m_parser.nextToken()) != null) {
      eventTypes = m_scriber.getNextEventTypes();
      if (token == JsonToken.START_OBJECT) {
        eventType = eventTypes.getStartObjectAnonymous();
        if (eventType != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.startObjectAnonymous(eventType);
          encodeObject();
        }
        else {
          // REVISIT: Throw an exception
          assert false;
        }
        
      }
      else if (token == JsonToken.START_ARRAY) {
        eventType = eventTypes.getStartArrayAnonymous();
        if (eventType != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.startArrayAnonymous();
          encodeArray();
        }
        else {
          // REVISIT: Throw an exception
          assert false;
        }
      }
      else if (token == JsonToken.VALUE_STRING) {
        final String stringValue = m_parser.getText();
        eventTypes = m_scriber.getNextEventTypes();
        if ((eventType = eventTypes.getStringValueAnonymous()) != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.anonymousStringValue(eventType);
          Scriber.stringValueScriber.scribe(stringValue, m_scribble, m_scriber.currentState.name, m_scriber);
        }
        else {
          assert false;
        }
      }
      else if (token == JsonToken.VALUE_NUMBER_INT) {
        eventTypes = m_scriber.getNextEventTypes();
        if ((eventType = eventTypes.getNumberValueAnonymous()) != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.anonymousNumberValue(eventType);
          encodeInteger();
        }
        else {
          assert false;
        }
      }
      else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
        if ((eventType = eventTypes.getNumberValueAnonymous()) != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.anonymousNumberValue(eventType);
          encodeFloat();
        }
        else
          assert false;
      }
      else if (token == JsonToken.VALUE_NULL) {
        eventTypes = m_scriber.getNextEventTypes();
        if ((eventType = eventTypes.getNullValueAnonymous()) != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.anonymousNullValue(eventType);
        }
        else
          assert false;
      }
      else if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
        eventTypes = m_scriber.getNextEventTypes();
        if ((eventType = eventTypes.getBooleanValueAnonymous()) != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.anonymousBooleanValue(eventType);
          if (Scriber.booleanValueScriber.process(m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), m_scribble, m_scriber)) {
            Scriber.booleanValueScriber.scribe((String)null, m_scribble, m_scriber.currentState.name, m_scriber);
          }
          else
            assert false;
        }
        else
          assert false;
      }
      else {
        // REVISIT: Support other values here.
      }
    }
    else {
      // REVISIT: Error-handling
    }
    m_scriber.finish();
  }
  
  private void encodeObject() throws IOException {
    EventTypeList eventTypes;
    EventType eventType;
    
    JsonToken token;
    while ((token = m_parser.nextToken()) != null) {
      if (token == JsonToken.END_OBJECT) {
        eventTypes = m_scriber.getNextEventTypes();
        eventType = eventTypes.getEndObject();
        m_scriber.writeEventType(eventType);
        m_scriber.endObject();
        break;
      }
      if (token == JsonToken.FIELD_NAME) {
        final String name = m_parser.getCurrentName();
        token = m_parser.nextToken();
        if (token == JsonToken.VALUE_STRING) {
          final String stringValue = m_parser.getText();
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getValueNamed(name, EventType.ITEM_STRING_VALUE_NAMED)) != null) {
            m_scriber.writeEventType(eventType);
            final int nameId = eventType.getNameId();
            Scriber.stringValueScriber.scribe(stringValue, m_scribble, nameId, m_scriber);
          }
          else {
            eventType = eventTypes.getStringValueWildcard();
            assert eventType != null;
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            Scriber.stringValueScriber.scribe(stringValue, m_scribble, nameId, m_scriber);
            m_scriber.wildcardStringValue(eventType.getIndex(), nameId);
          }
        }
        else if (token == JsonToken.VALUE_NUMBER_INT) {
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getValueNamed(name, EventType.ITEM_NUMBER_VALUE_NAMED)) != null) {
            m_scriber.writeEventType(eventType);
            encodeInteger();
          }
          else if ((eventType = eventTypes.getNumberValueWildcard()) != null) {
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.wildcardNumberValue(eventType.getIndex(), nameId); 
            encodeInteger();
          }
          else {
            assert false;
          }
        }
        else if (token == JsonToken.VALUE_NUMBER_FLOAT) { 
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getValueNamed(name, EventType.ITEM_NUMBER_VALUE_NAMED)) != null) {
            m_scriber.writeEventType(eventType);
            encodeFloat();
          }
          else if ((eventType = eventTypes.getNumberValueWildcard()) != null) {
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.wildcardNumberValue(eventType.getIndex(), nameId); 
            encodeFloat();
          }
          else
            assert false;
        }
        else if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getValueNamed(name, EventType.ITEM_BOOLEAN_VALUE_NAMED)) != null) {
            m_scriber.writeEventType(eventType);
          }
          else if ((eventType = eventTypes.getBooleanValueWildcard()) != null) {
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.wildcardBooleanValue(eventType.getIndex(), nameId);
          }
          else
            assert false;
          
          final char[] characters = m_parser.getTextCharacters();
          final int offset = m_parser.getTextOffset();
          final int length = m_parser.getTextLength();
          if (Scriber.booleanValueScriber.process(characters, offset, length, m_scribble, m_scriber)) {
            Scriber.booleanValueScriber.scribe((String)null, m_scribble, m_scriber.currentState.name, m_scriber);
          }
          else
            assert false;
        }
        else if (token == JsonToken.START_OBJECT) {
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getStartObjectNamed(name)) != null) {
            m_scriber.writeEventType(eventType);
            m_scriber.startObjectNamed(eventType);
          }
          else {
            eventType = eventTypes.getStartObjectWildcard();
            assert eventType != null;
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.startObjectWildcard(nameId);
          }
          encodeObject();
          
        }
        else if (token == JsonToken.START_ARRAY) {
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getStartArrayNamed(name)) != null) {
            m_scriber.writeEventType(eventType);
            m_scriber.startArrayNamed(eventType);
          }
          else {
            eventType = eventTypes.getStartArrayWildcard();
            assert eventType != null;
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.startArrayWildcard(nameId);
          }
          encodeArray();
        }
        
      }
      else {
        // REVISIT: Should never enter here. Throw an exception.
        assert false;
      }
    }
  }

  private void encodeArray() throws IOException {
    EventTypeList eventTypes;
    EventType eventType;
    
    JsonToken token;
    while ((token = m_parser.nextToken()) != null) {
      if (token == JsonToken.END_ARRAY) {
        eventTypes = m_scriber.getNextEventTypes();
        eventType = eventTypes.getEndArray();
        m_scriber.writeEventType(eventType);
        m_scriber.endArray();
        break;
      }
      if (token == JsonToken.VALUE_STRING) {
        final String stringValue = m_parser.getText();
        eventTypes = m_scriber.getNextEventTypes();
        if ((eventType = eventTypes.getStringValueAnonymous()) != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.anonymousStringValue(eventType);
          Scriber.stringValueScriber.scribe(stringValue, m_scribble, m_scriber.currentState.name, m_scriber);
        }
        else {
          assert false;
        }
      }
      else if (token == JsonToken.START_ARRAY) {
        eventTypes = m_scriber.getNextEventTypes();
        eventType = eventTypes.getStartArrayAnonymous();
        if (eventType != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.startArrayAnonymous();
          encodeArray();
        }
        else {
          // REVISIT: Throw an exception
          assert false;
        }
      }
      else if (token == JsonToken.START_OBJECT) {
        eventTypes = m_scriber.getNextEventTypes();
        eventType = eventTypes.getStartObjectAnonymous();
        if (eventType != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.startObjectAnonymous(eventType);
          encodeObject();
        }
        else {
          // REVISIT: Throw an exception
          assert false;
        }
      }
      
      
//      if (token == JsonToken.FIELD_NAME) {
//        final String name = m_parser.getCurrentName();
//        token = m_parser.nextToken();
//        if (token == JsonToken.VALUE_STRING) {
//          final String stringValue = m_parser.getText();
//          eventTypes = m_scriber.getNextEventTypes();
//          if ((eventType = eventTypes.getStringValueNamed(name)) != null) {
//            m_scriber.writeEventType(eventType);
//            final int nameId = eventType.getNameId();
//            Scriber.stringValueScriber.scribe(stringValue, m_scribble, nameId, m_scriber);
//          }
//          else {
//            eventType = eventTypes.getStringValueWildcard();
//            assert eventType != null;
//            m_scriber.writeEventType(eventType);
//            final int nameId = m_scriber.writeName(name, eventType);
//            Scriber.stringValueScriber.scribe(stringValue, m_scribble, nameId, m_scriber);
//            m_scriber.wildcardStringValue(eventType.getIndex(), nameId);
//          }
//        }
//        else if (token == JsonToken.START_OBJECT) {
//          eventTypes = m_scriber.getNextEventTypes();
//          if ((eventType = eventTypes.getStartObjectNamed(name)) != null) {
//            m_scriber.writeEventType(eventType);
//            m_scriber.startObjectNamed(eventType);
//          }
//          else {
//            eventType = eventTypes.getStartObjectWildcard();
//            assert eventType != null;
//            m_scriber.writeEventType(eventType);
//            final int nameId = m_scriber.writeName(name, eventType);
//            m_scriber.startObjectWildcard(nameId);
//          }
//          encodeObject();
//          
//        }
//        
//      }
//      else {
//        // REVISIT: Should never enter here. Throw an exception.
//        assert false;
//      }
    }
    
    
  }
  
  private void encodeInteger() throws IOException {
  
    final char[] characters = m_parser.getTextCharacters();
    final int offset = m_parser.getTextOffset();
    final int length = m_parser.getTextLength();
    Scriber.numberValueScriber.processInteger(characters, offset, length, m_scribble, m_scriber);
    Scriber.numberValueScriber.scribe((String)null, m_scribble, m_scriber.currentState.name, m_scriber);

  }
  
  
  private void encodeFloat() throws IOException {
    
   
    
      final char[] characters = m_parser.getTextCharacters();
      final int offset = m_parser.getTextOffset();
      final int length = m_parser.getTextLength();
      
      final boolean isDecimal;;
      
      final int limit = offset + length;
      int pos = offset;
      if (characters[pos] == '-')
        ++pos;
      while (pos < limit && '0' <= characters[pos] && characters[pos] <= '9') pos++;
      if (pos < limit) {
        char d = characters[pos];
        if (d == '.') {
          pos++;
          if (pos < limit && '0' <= characters[pos] && characters[pos] <= '9') {
            pos++;
            while (pos < limit && '0' <= characters[pos] && characters[pos] <= '9') pos++;
          }
          // 'e' or 'E' ==> TOK_DOUBLE, otherwise TOK_DECIMAL
          if (pos < limit) {
            d = characters[pos];
            if (d == 'e' || d == 'E') { // [0-9]+ "." [0-9]* [eE] [+-]? [0-9]+
              pos++;
              if (pos < limit) {
                d = characters[pos];
                if (d == '+' || d == '-')
                  pos++;
                while (pos < limit && '0' <= characters[pos] && characters[pos] <= '9') pos++;
              }
              isDecimal = false;
            }
            else { // [0-9]+ "." [0-9]*
              isDecimal = true;
            }
          }
          else { // [0-9]+ "." [0-9]*
            isDecimal = true;
          }
        }
        else if (d == 'e' || d == 'E') { // [0-9]+ [eE] [+-]? [0-9]+
          pos++;
          if (pos < limit) {
            d = characters[pos];
            if (d == '+' || d == '-')
              pos++;
            while (pos < limit && '0' <= characters[pos] && characters[pos] <= '9') pos++;
          }
          isDecimal = false;
        }
        else { // [0-9]+
          assert false;
          isDecimal = true;
        }
      }
      else { // [0-9]+
        assert false;
        isDecimal = true;
      }

      if (isDecimal) {
        if (Scriber.numberValueScriber.doProcessDecimal(characters, offset, length, m_scribble, m_scriber.stringBuilder1, m_scriber.stringBuilder2))
          ;
        else
          assert false;
      }
      else {
        if (Scriber.numberValueScriber.doProcessFloat(characters, offset, length, m_scribble, m_scriber.stringBuilder1))
          ;
        else
          assert false;
      }
//      EventType eventType;
//      if ((eventType = eventTypes.getNumberValueAnonymous()) != null) {
//        m_scriber.writeEventType(eventType);
//        m_scriber.anonymousNumberValue(eventType);
//      }
//      else
//        assert false;
    
      Scriber.numberValueScriber.scribe((String)null, m_scribble, m_scriber.currentState.name, m_scriber);
    
  }
  
}