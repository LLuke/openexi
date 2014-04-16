package org.openexi.sax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;

import junit.framework.Assert;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EmptySchema;
import org.openexi.schema.TestBase;
import org.xml.sax.InputSource;

public class ValuePartitionCapacityTest extends TestBase {

  public ValuePartitionCapacityTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testGlobalPartition() throws Exception {

    String[] values = { "val1", "val2", "val3", "val2", "val1" };

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.DEFAULT_OPTIONS);

    Transmogrifier encoder = new Transmogrifier();
    EXIDecoder decoder = new EXIDecoder();
    Scanner scanner;
    InputSource inputSource;
    
    encoder.setAlignmentType(AlignmentType.bitPacked);
    encoder.setValuePartitionCapacity(2);
    encoder.setOutputOptions(HeaderOptionsOutputType.lessSchemaId);

    encoder.setGrammarCache(grammarCache);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);
    
    URL url = resolveSystemIdAsURL("/interop/datatypes/string/indexed-05.xml");
    inputSource = new InputSource(url.toString());
    inputSource.setByteStream(url.openStream());

    byte[] bts;
    int n_texts;
    
    encoder.encode(inputSource);
    
    bts = baos.toByteArray();

    decoder.setGrammarCache(grammarCache);
    decoder.setInputStream(new ByteArrayInputStream(bts));
    scanner = decoder.processHeader();
    Assert.assertEquals(2, scanner.getHeaderOptions().getValuePartitionCapacity());
    
    ArrayList<EventDescription> exiEventList = new ArrayList<EventDescription>();
    
    EventDescription exiEvent;
    String stringValue= null;
    n_texts = 0;
    while ((exiEvent = scanner.nextEvent()) != null) {
      if (exiEvent.getEventKind() == EventDescription.EVENT_CH) {
        stringValue = exiEvent.getCharacters().makeString();
        Assert.assertEquals(values[n_texts], stringValue);
        ++n_texts;
      }
      exiEventList.add(exiEvent);
    }
  }

}
