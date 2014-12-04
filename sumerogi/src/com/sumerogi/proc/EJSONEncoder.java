package com.sumerogi.proc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.sumerogi.proc.common.AlignmentType;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
import com.sumerogi.proc.common.StringTable;
import com.sumerogi.proc.grammars.GrammarCache;
import com.sumerogi.proc.io.Scribble;
import com.sumerogi.proc.io.Scriber;
import com.sumerogi.proc.io.ScriberFactory;

public final class EJSONEncoder {
  
  private final GrammarCache m_grammarCache;

  private OutputStream m_outputStream;
  
  private JsonParser m_parser;
  
  private Scriber m_scriber;
  private Scribble m_scribble;

  public EJSONEncoder() {
    m_outputStream = null;
    m_scribble = new Scribble();
    m_grammarCache = new GrammarCache();
    m_scriber = ScriberFactory.createScriber(AlignmentType.bitPacked); 
    m_scriber.setStringTable(Scriber.createStringTable(m_grammarCache));
  }
  
  private void reset() {
    // REVISIT: do I need to re-create a parser for every encode process?
    m_parser = null;
  }
  
  public final void setAlignmentType(AlignmentType alignmentType) {
    if (m_scriber.getAlignmentType() != alignmentType) {
      m_scriber = ScriberFactory.createScriber(alignmentType);
      m_scriber.setStringTable(Scriber.createStringTable(m_grammarCache));
    }
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
    m_scriber.writeHeaderPreamble();
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
          m_scriber.getStringValueScriber().scribe(stringValue, m_scribble, m_scriber.currentState.name, m_scriber);
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
          encodeInteger(StringTable.NAME_DOCUMENT);
        }
        else {
          assert false;
        }
      }
      else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
        if ((eventType = eventTypes.getNumberValueAnonymous()) != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.anonymousNumberValue(eventType);
          encodeFloat(StringTable.NAME_DOCUMENT);
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
          if (m_scriber.getBooleanValueScriber().process(m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), m_scribble, m_scriber)) {
            m_scriber.getBooleanValueScriber().scribe((String)null, m_scribble, StringTable.NAME_DOCUMENT, m_scriber);
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
          if ((eventType = eventTypes.getEventType(EventType.ITEM_STRING_VALUE_NAMED, name)) != null) {
            m_scriber.writeEventType(eventType);
            final int nameId = eventType.getNameId();
            m_scriber.getStringValueScriber().scribe(stringValue, m_scribble, nameId, m_scriber);
          }
          else {
            eventType = eventTypes.getStringValueWildcard();
            assert eventType != null;
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.getStringValueScriber().scribe(stringValue, m_scribble, nameId, m_scriber);
            m_scriber.wildcardStringValue(eventType.getIndex(), nameId);
          }
        }
        else if (token == JsonToken.VALUE_NUMBER_INT) {
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getEventType(EventType.ITEM_NUMBER_VALUE_NAMED, name)) != null) {
            m_scriber.writeEventType(eventType);
            encodeInteger(eventType.getNameId());
          }
          else if ((eventType = eventTypes.getNumberValueWildcard()) != null) {
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.wildcardNumberValue(eventType.getIndex(), nameId); 
            encodeInteger(nameId);
          }
          else {
            assert false;
          }
        }
        else if (token == JsonToken.VALUE_NUMBER_FLOAT) { 
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getEventType(EventType.ITEM_NUMBER_VALUE_NAMED, name)) != null) {
            m_scriber.writeEventType(eventType);
            encodeFloat(eventType.getNameId());
          }
          else if ((eventType = eventTypes.getNumberValueWildcard()) != null) {
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.wildcardNumberValue(eventType.getIndex(), nameId); 
            encodeFloat(nameId);
          }
          else
            assert false;
        }
        else if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
          final int nameId;
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getEventType(EventType.ITEM_BOOLEAN_VALUE_NAMED, name)) != null) {
            nameId = eventType.getNameId();
            m_scriber.writeEventType(eventType);
          }
          else if ((eventType = eventTypes.getBooleanValueWildcard()) != null) {
            m_scriber.writeEventType(eventType);
            nameId = m_scriber.writeName(name, eventType);
            m_scriber.wildcardBooleanValue(eventType.getIndex(), nameId);
          }
          else {
            assert false;
            nameId = StringTable.NAME_NONE;
          }
          
          final char[] characters = m_parser.getTextCharacters();
          final int offset = m_parser.getTextOffset();
          final int length = m_parser.getTextLength();
          if (m_scriber.getBooleanValueScriber().process(characters, offset, length, m_scribble, m_scriber)) {
            m_scriber.getBooleanValueScriber().scribe((String)null, m_scribble, nameId, m_scriber);
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
        else if (token == JsonToken.VALUE_NULL) { 
          eventTypes = m_scriber.getNextEventTypes();
          if ((eventType = eventTypes.getEventType(EventType.ITEM_NULL_NAMED, name)) != null) {
            m_scriber.writeEventType(eventType);
          }
          else if ((eventType = eventTypes.getNullValueWildcard()) != null) {
            m_scriber.writeEventType(eventType);
            final int nameId = m_scriber.writeName(name, eventType);
            m_scriber.wildcardNullValue(eventType.getIndex(), nameId); 
          }
          else
            assert false;
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
          m_scriber.getStringValueScriber().scribe(stringValue, m_scribble, m_scriber.currentState.name, m_scriber);
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

  private void encodeInteger(int name) throws IOException {
    
    final char[] characters = m_parser.getTextCharacters();
    final int offset = m_parser.getTextOffset();
    final int length = m_parser.getTextLength();
    Scriber.numberValueScriber.processInteger(characters, offset, length, m_scribble, m_scriber);
    m_scriber.getNumberValueScriber().scribe((String)null, m_scribble, name, m_scriber);
  }
  

  private void encodeFloat(int name) throws IOException {
    
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
    
      m_scriber.getNumberValueScriber().scribe((String)null, m_scribble, name, m_scriber);
  }
  
  /**
   * Resolve a string representing an uri into an absolute URI given a base URI.
   * Null is returned if the uri is null or the uri seems to be a relative one
   * with baseURI being null.
   * @param uri
   * @param baseURI
   * @return absolute URI
   * @throws URISyntaxException
   */
  private static URI resolveURI(String uri, URI baseURI)
      throws URISyntaxException {
    URI resolved = null;
    if (uri != null) {
      int pos;
      if ((pos = uri.indexOf(':')) <= 1) {
        if (pos == 1) {
          char firstChar = uri.charAt(0);
          if ('A' <= firstChar && firstChar <= 'Z' ||
              'a' <= firstChar && firstChar <= 'z') {
            resolved = new File(uri).toURI();
          }
        }
        else { // relative URI
          if (baseURI != null)
            resolved = baseURI.resolve(uri);
          else
            return null;
        }
      }
      if (resolved == null)
        resolved = new URI(uri); // cross your fingers
    }
    return resolved;
  }
  
  public static void main(String args[]) throws IOException {

    int pos = 0;
    AlignmentType alignment = AlignmentType.bitPacked;
    if (args.length == 3) {
      String alignmentString = args[0];
      if (alignmentString.charAt(0) == '-') {
        alignmentString = alignmentString.substring(1);
        if ("c".equals(alignmentString)) {
          alignment = AlignmentType.compress;
        }
        else if ("p".equals(alignmentString)) {
          alignment = AlignmentType.preCompress;
        }
        else if (!"b".equals(alignmentString)) {
          printSynopsis();
          System.exit(1);
        }
        ++pos;
      }
      else {
        printSynopsis();
        System.exit(1);
      }
    }
    else if (args.length != 2) {
      printSynopsis();
      System.exit(1);
      return;
    }

    final URI baseURI = new File(System.getProperty("user.dir")).toURI().resolve("whatever");

    URI jsonUri;
    try {
      jsonUri = resolveURI(args[pos], baseURI);
    }
    catch (URISyntaxException use) {
      System.err.println("'" + args[pos] + "' is not a valid URI.");
      System.exit(1);
      return;
    }
    assert jsonUri != null;
    
    ++pos;
    URI outputUri;
    try {
      outputUri = resolveURI(args[pos], baseURI);
    }
    catch (URISyntaxException use) {
      System.err.println("'" + args[pos] + "' is not a valid URI.");
      System.exit(1);
      return;
    }
    assert outputUri != null;

    EJSONEncoder encoder = new EJSONEncoder();
    encoder.setAlignmentType(alignment);

    FileOutputStream outputStream;
    outputStream = new FileOutputStream(outputUri.toURL().getFile());
    encoder.setOutputStream(outputStream);
    
    InputStream inputStream;
    try {
      inputStream = jsonUri.toURL().openStream();
    }
    catch (IOException e) {
      outputStream.close();
      throw e;
    }

    try {
      encoder.encode(inputStream);
    }
    finally {
      inputStream.close();
      outputStream.close();
    }
  }
  
  private static void printSynopsis() {
    System.err.println("USAGE: " + EJSONEncoder.class.getName() +
        " [-b|-c|-p] JSON_File Output_File");
  }
  
}