package org.openexi.scomp;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.SAXTransmogrifier;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.GrammarSchema;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public class EncodeGrammarFragments {

  private static final String FIXTURE_GRAMMARS_INSTANCE  = "FixtureGrammars.xml"; 
  private static final String FIXTURE_GRAMMARS_ENCODED   = "FixtureGrammars.exi";

  private static final String FIXTURE_TYPES_INSTANCE  = "FixtureTypes.xml"; 
  private static final String FIXTURE_TYPES_ENCODED   = "FixtureTypes.exi"; 

  private static final String FIXTURE_NAMES_NONAMESPACE_INSTANCE  = "FixtureNamesNoNamespace.xml"; 
  private static final String FIXTURE_NAMES_NONAMESPACE_ENCODED  = "FixtureNamesNoNamespace.exi"; 

  private static final String FIXTURE_NAMES_XMLNAMESPACE_INSTANCE  = "FixtureNamesXmlNamespace.xml"; 
  private static final String FIXTURE_NAMES_XMLNAMESPACE_ENCODED  = "FixtureNamesXmlNamespace.exi"; 

  private static final String FIXTURE_NAMES_XSINAMESPACE_INSTANCE  = "FixtureNamesXsiNamespace.xml"; 
  private static final String FIXTURE_NAMES_XSINAMESPACE_ENCODED  = "FixtureNamesXsiNamespace.exi"; 

  private static final String FIXTURE_NAMES_XSDNAMESPACE_INSTANCE  = "FixtureNamesXsdNamespace.xml"; 
  private static final String FIXTURE_NAMES_XSDNAMESPACE_ENCODED  = "FixtureNamesXsdNamespace.exi"; 

  public static void main(String[] args) throws IOException {
    final EncodeGrammarFragments encoder = new EncodeGrammarFragments();
    encoder.encodeFixtureGrammars();
    encoder.encodeFixtureTypes();
    encoder.encodeFixtureNames();
  }
  
  private void encodeFixtureGrammars() throws IOException {
    doEncode(FIXTURE_GRAMMARS_INSTANCE, FIXTURE_GRAMMARS_ENCODED);
  }

  private void encodeFixtureTypes() throws IOException {
    doEncode(FIXTURE_TYPES_INSTANCE, FIXTURE_TYPES_ENCODED);
  }

  private void encodeFixtureNames() throws IOException {
    doEncode(FIXTURE_NAMES_NONAMESPACE_INSTANCE, FIXTURE_NAMES_NONAMESPACE_ENCODED);
    doEncode(FIXTURE_NAMES_XMLNAMESPACE_INSTANCE, FIXTURE_NAMES_XMLNAMESPACE_ENCODED);
    doEncode(FIXTURE_NAMES_XSINAMESPACE_INSTANCE, FIXTURE_NAMES_XSINAMESPACE_ENCODED);
    doEncode(FIXTURE_NAMES_XSDNAMESPACE_INSTANCE, FIXTURE_NAMES_XSDNAMESPACE_ENCODED);
  }

  private void doEncode(String instanceFileName, String encodedFileName) throws IOException {

    final URL inputUrl = EncodeGrammarFragments.class.getResource(instanceFileName);

    final byte[] fixtureGrammarsBytes;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      
      Transmogrifier transmogrifier = new Transmogrifier();
      try {
        transmogrifier.setGrammarCache(new GrammarCache(GrammarSchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS));
      }
      catch (EXIOptionsException eoe) {
        throw new RuntimeException(eoe);
      }
      transmogrifier.setFragment(true);
      transmogrifier.setOutputStream(baos);
      SAXTransmogrifier saxTransmogrifier = transmogrifier.getSAXTransmogrifier();
      
      final Unwrapper unwrapper = new Unwrapper();
      unwrapper.setContentHandler(saxTransmogrifier);

      final SAXParserFactory saxParserFactory;
      final XMLReader xmlReader;
      
      saxParserFactory = SAXParserFactory.newInstance();
      saxParserFactory.setNamespaceAware(true);
      try {
        xmlReader = saxParserFactory.newSAXParser().getXMLReader();
      }
      catch (ParserConfigurationException pce) {
        throw new RuntimeException(pce);
      }
      xmlReader.setContentHandler(unwrapper);
      
      InputSource inputSource;
      InputStream inputStream = inputUrl.openStream();
      inputSource = new InputSource(inputStream);
      inputSource.setSystemId(inputUrl.toString());
      
      try {
        xmlReader.parse(inputSource);
      }
      catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      finally {
        inputStream.close();
      }
      fixtureGrammarsBytes = baos.toByteArray();
    }
    catch (SAXException se) {
      throw new RuntimeException(se);
    }
    
    FileOutputStream fos = null;
    try {
      URL outputUrl = new URL(inputUrl, encodedFileName);
      fos = new FileOutputStream(outputUrl.getFile());
      fos.write(fixtureGrammarsBytes);
      fos.flush();
    }
    finally {
      if (fos != null) fos.close();
    }
  }
  
  private static class Unwrapper extends XMLFilterImpl {
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      if ("Dummy".equals(localName))
        return;
      else 
        super.startElement(uri, localName, qName, atts);
    }
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if ("Dummy".equals(localName))
        return;
      else 
        super.endElement(uri, localName, qName);
    }
  }
  

}
