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
    m_transmogrifier = new org.openexi.sax.Transmogrifier();
    try {
      m_transmogrifier.setGrammarCache(JsonSchema.getGrammarCache(), new SchemaId("schema-for-json"));
    } 
    catch (EXIOptionsException e) {
      // Never enters here.
      e.printStackTrace();
      assert false;
    }
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
      else {
        // implement START_ARRAY, VALUE_STRING, VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT, VALUE_NULL, VALUE_TRUE, VALUE_FALSE
      }
      
      
    }
    transmogrifier.endDocument();
  }
  
  private void encodeObject(SAXTransmogrifier transmogrifier, String myName) throws IOException, SAXException{
    // SE(j:map) content EE
    m_attributes.clear();
    if (myName != null) {
      // SE(j:map) AT(key) content EE      
      m_attributes.addAttribute("", "key", (String)null, "", myName);
    }
    transmogrifier.startElement(JsonSchema.URI, "map", (String)null, m_attributes);

    JsonToken token;
    while ((token = m_parser.nextToken()) != null) {
      if (token == JsonToken.END_OBJECT) {
        transmogrifier.endElement(JsonSchema.URI, "map", (String)null);
        break;
      }
      if (token == JsonToken.FIELD_NAME) {
        final String name = m_parser.getCurrentName();
        token = m_parser.nextToken();
        if (token == JsonToken.VALUE_STRING) {
          final String stringValue = m_parser.getText();
          encodeString(transmogrifier, stringValue, name);
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
  }
  
  private void encodeString(SAXTransmogrifier transmogrifier, String value, String myName) throws IOException, SAXException{
    // SE(j:string) CH(string-value) EE
    m_attributes.clear();
    if (myName != null) {
      // SE(j:string) AT(key) CH(string-value) EE
      m_attributes.addAttribute("", "key", "key", "", myName);
    }
    transmogrifier.startElement(JsonSchema.URI, "string", (String)null, m_attributes);
    transmogrifier.characters(value.toCharArray(), 0, value.length());
    transmogrifier.endElement(JsonSchema.URI, "string", (String)null);
  }

}
