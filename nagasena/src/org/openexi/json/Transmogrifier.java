package org.openexi.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.SchemaId;
import org.openexi.sax.SAXTransmogrifier;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public final class Transmogrifier {
  
  private JsonParser m_parser;

  private final org.openexi.sax.Transmogrifier m_transmogrifier;

  private OutputStream m_outputStream;
  
  private final AttributesImpl m_attributes;

  public Transmogrifier() {
    this(true);
  }

  public Transmogrifier(boolean useBuiltinElementGrammar) {
    m_transmogrifier = new org.openexi.sax.Transmogrifier();
    try {
      m_transmogrifier.setGrammarCache(EXI4JsonSchema.getGrammarCache(), new SchemaId("exi4json"));
    } 
    catch (EXIOptionsException e) {
      // Never enters here.
      e.printStackTrace();
      assert false;
    }
    m_transmogrifier.setUseBuiltinElementGrammar(useBuiltinElementGrammar);
    m_attributes = new AttributesImpl();
  }

  private void reset() {
    // REVISIT: do I need to re-create a parser for every encode process?
    m_parser = null;
  }
  
  public final void setOutputStream(OutputStream ostream) {
    m_outputStream = ostream;
  }
  
  public void encode(String inputString) throws IOException, SAXException {
    reset();
    m_parser = new JsonFactory().createParser(inputString);
    encodeDocument();
  }
  
  public void encode(InputStream inputStream) throws IOException, SAXException {
    reset();
    m_parser = new JsonFactory().createParser(inputStream);
    encodeDocument();
  }
  
  private void encodeDocument() throws IOException, SAXException {
    m_transmogrifier.setOutputStream(m_outputStream);
    
    SAXTransmogrifier transmogrifier = m_transmogrifier.getSAXTransmogrifier();
    
    transmogrifier.startDocument();
    
    JsonToken token;
    if ((token = m_parser.nextToken()) != null) {
      if (token == JsonToken.START_OBJECT) {
        encodeObject(transmogrifier, (String)null);
      }
      else if (token == JsonToken.START_ARRAY) {
        encodeArray(transmogrifier, (String)null);
      }
      else if (token == JsonToken.VALUE_STRING) {
        encodeValue(transmogrifier, "string", false, m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), (String)null);
      }
      else if (token == JsonToken.VALUE_NUMBER_INT) {
        encodeValue(transmogrifier, "integer", true, m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), (String)null);
      }
      else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
        encodeValue(transmogrifier, "number", false, m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), (String)null);
      }
      else {
        // implement VALUE_NULL, VALUE_TRUE, VALUE_FALSE
        assert false;
      }
      
    }
    transmogrifier.endDocument();
  }
  
  private void encodeObject(SAXTransmogrifier transmogrifier, String myName) throws IOException, SAXException{
    if (myName != null) {
      transmogrifier.startElement(EXI4JsonSchema.URI, myName, (String)null, m_attributes);
    }
    transmogrifier.startElement(EXI4JsonSchema.URI, "map", (String)null, m_attributes);

    JsonToken token;
    while ((token = m_parser.nextToken()) != null) {
      if (token == JsonToken.END_OBJECT) {
        transmogrifier.endElement(EXI4JsonSchema.URI, "map", (String)null);
        break;
      }
      if (token == JsonToken.FIELD_NAME) {
        final String name = m_parser.getCurrentName();
        token = m_parser.nextToken();
        if (token == JsonToken.START_OBJECT) {
          encodeObject(transmogrifier, name);
        }
        else if (token == JsonToken.START_ARRAY) {
          encodeArray(transmogrifier, name);
        }
        else if (token == JsonToken.VALUE_STRING) {
          encodeValue(transmogrifier, "string", false, m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), name);
        }
        else if (token == JsonToken.VALUE_NUMBER_INT) {
          encodeValue(transmogrifier, "integer", true, m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), name);
        }
        else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
          encodeValue(transmogrifier, "number", false, m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), name);
        }
        else {
          // implement
          assert false;
        }
      }
      else {
        // REVISIT: Should never enter here. Throw an exception.
        assert false;
      }
    }
    if (myName != null) {
      transmogrifier.endElement(EXI4JsonSchema.URI, myName, (String)null);
    }
  }

  private void encodeArray(SAXTransmogrifier transmogrifier, String myName) throws IOException, SAXException{
    if (myName != null) {
      transmogrifier.startElement(EXI4JsonSchema.URI, myName, (String)null, m_attributes);
    }
    transmogrifier.startElement(EXI4JsonSchema.URI, "array", (String)null, m_attributes);

    JsonToken token;
    while ((token = m_parser.nextToken()) != null) {
      if (token == JsonToken.END_ARRAY) {
        transmogrifier.endElement(EXI4JsonSchema.URI, "array", (String)null);
        break;
      }
      if (token == JsonToken.START_OBJECT) {
        encodeObject(transmogrifier, (String)null);
      }
      else if (token == JsonToken.VALUE_STRING) {
        encodeValue(transmogrifier, "string", false, m_parser.getTextCharacters(), m_parser.getTextOffset(), m_parser.getTextLength(), (String)null);

      }
      else {
        assert false;
      }
    }
    if (myName != null) {
      transmogrifier.endElement(EXI4JsonSchema.URI, myName, (String)null);
    }
  }

  private void encodeValue(SAXTransmogrifier transmogrifier, String typeName, boolean isOther, char[] characters, int offset, int length, String propertyName) throws IOException, SAXException{
    if (propertyName != null) {
      transmogrifier.startElement(EXI4JsonSchema.URI, propertyName, (String)null, m_attributes);
    }
    if (isOther) {
      transmogrifier.startElement(EXI4JsonSchema.URI, "other", (String)null, m_attributes);
    }
    transmogrifier.startElement(EXI4JsonSchema.URI, typeName, (String)null, m_attributes);
    transmogrifier.characters(characters, offset, length);
    transmogrifier.endElement(EXI4JsonSchema.URI, typeName, (String)null);
    if (isOther) {
      transmogrifier.endElement(EXI4JsonSchema.URI, "other", (String)null);
    }
    if (propertyName != null) {
      transmogrifier.endElement(EXI4JsonSchema.URI, propertyName, (String)null);
    }
  }
  
}
