package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import junit.framework.Assert;

import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.EXIReader;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.TestBase;
import org.xml.sax.InputSource;

public class OpenDaylightTest extends TestBase {

  public OpenDaylightTest(String name) {
    super(name);
  }

  /**
   * OpenDaylight use case revealed a problem in reusing Transmogrifier.
   */
  public void testReuseTransmogrifier() throws Exception{

    final SAXTransformerFactory FACTORY = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

    String xml1 ="<rpc xmlns=\"urn:foo\">" +
        "<edit-config>" +
        "<target/>" +
        "<config>" +
        "<modules xmlns=\"urn:goo\"/>" +
        "</config>" +
        "</edit-config>" +
        "</rpc>";

    String xml2 = "<rpc xmlns=\"urn:foo\">" +
        "<edit-config>" +
        "<target/>" +
        "<default-operation/>" +
        "<config>" +
        "<modules xmlns=\"urn:goo\"/>" +
        "</config>" +
        "</edit-config>" +
        "</rpc>";
    
    final GrammarCache grammarCache = new GrammarCache(GrammarOptions.addNS(GrammarOptions.DEFAULT_OPTIONS));
    
    final Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setAlignmentType(AlignmentType.bitPacked);
    transmogrifier.setGrammarCache(grammarCache);
    transmogrifier.setOutputOptions(HeaderOptionsOutputType.none);
    
    final EXIReader reader = new EXIReader();
    reader.setAlignmentType(AlignmentType.bitPacked);
    reader.setGrammarCache(grammarCache);

    ByteArrayOutputStream outputStream;
    TransformerHandler handler;
    StreamResult streamResult;
    StringWriter stringWriter;

    outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(xml1)));

    handler = FACTORY.newTransformerHandler();
    handler.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    reader.setContentHandler(handler);
    
    stringWriter = new StringWriter();
    streamResult = new StreamResult(stringWriter);
    handler.setResult(streamResult);
    reader.parse(new InputSource(new ByteArrayInputStream(outputStream.toByteArray())));
    Assert.assertEquals(xml1, stringWriter.getBuffer().toString());
    
    outputStream = new ByteArrayOutputStream();
    transmogrifier.setOutputStream(outputStream);
    transmogrifier.encode(new InputSource(new StringReader(xml2)));

    handler = FACTORY.newTransformerHandler();
    handler.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    reader.setContentHandler(handler);

    stringWriter = new StringWriter();
    streamResult = new StreamResult(stringWriter);
    handler.setResult(streamResult);
    reader.parse(new InputSource(new ByteArrayInputStream(outputStream.toByteArray())));
    Assert.assertEquals(xml2, stringWriter.getBuffer().toString());
  }

}