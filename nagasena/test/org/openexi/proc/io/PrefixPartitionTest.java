package org.openexi.proc.io;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.EmptySchema;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;

public class PrefixPartitionTest extends TestCase {
  
  public PrefixPartitionTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_compilerErrors = new EXISchemaFactoryErrorMonitor();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    m_compilerErrors.clear();
  }

  private EXISchemaFactoryErrorMonitor m_compilerErrors;

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testDefaultPrefixes() throws Exception {
    
    StringTable stringTable;
    StringTable.PrefixPartition prefixPartition;
    
    stringTable = Scriber.createStringTable(null); 
    
    int uriId = stringTable.getCompactIdOfURI("");
    prefixPartition = stringTable.getPrefixPartition(uriId);
    
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(1, prefixPartition.forwardedWidth);

    Assert.assertEquals(1, prefixPartition.n_strings);
    Assert.assertEquals(0, prefixPartition.getCompactId(""));
    
    prefixPartition.addPrefix("a");
    Assert.assertEquals(2, prefixPartition.n_strings);
    Assert.assertEquals(1, prefixPartition.width);
    Assert.assertEquals(2, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("b");
    Assert.assertEquals(3, prefixPartition.n_strings);
    Assert.assertEquals(2, prefixPartition.width);
    Assert.assertEquals(2, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("c");
    Assert.assertEquals(4, prefixPartition.n_strings);
    Assert.assertEquals(2, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("d");
    Assert.assertEquals(5, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("e");
    Assert.assertEquals(6, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("f");
    Assert.assertEquals(7, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("g");
    Assert.assertEquals(8, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(4, prefixPartition.forwardedWidth);
    
    prefixPartition.reset();
    
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(1, prefixPartition.forwardedWidth);

    Assert.assertEquals(1, prefixPartition.n_strings);
    Assert.assertEquals(0, prefixPartition.getCompactId(""));
  }
  
  /**
   */
  public void testXmlPrefixes() throws Exception {
    
    StringTable stringTable;
    StringTable.PrefixPartition prefixPartition;
    
    stringTable = Scriber.createStringTable(null); 
    
    int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_XML_1998_URI);
    prefixPartition = stringTable.getPrefixPartition(uriId);
    
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(1, prefixPartition.forwardedWidth);

    Assert.assertEquals(1, prefixPartition.n_strings);
    Assert.assertEquals(0, prefixPartition.getCompactId("xml"));
    
    prefixPartition.addPrefix("a");
    Assert.assertEquals(2, prefixPartition.n_strings);
    Assert.assertEquals(1, prefixPartition.width);
    Assert.assertEquals(2, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("b");
    Assert.assertEquals(3, prefixPartition.n_strings);
    Assert.assertEquals(2, prefixPartition.width);
    Assert.assertEquals(2, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("c");
    Assert.assertEquals(4, prefixPartition.n_strings);
    Assert.assertEquals(2, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("d");
    Assert.assertEquals(5, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("e");
    Assert.assertEquals(6, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("f");
    Assert.assertEquals(7, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("g");
    Assert.assertEquals(8, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(4, prefixPartition.forwardedWidth);
    
    prefixPartition.reset();
    
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(1, prefixPartition.forwardedWidth);

    Assert.assertEquals(1, prefixPartition.n_strings);
    Assert.assertEquals(0, prefixPartition.getCompactId("xml"));
  }

  /**
   */
  public void testXsiPrefixes() throws Exception {
    
    StringTable stringTable;
    StringTable.PrefixPartition prefixPartition;
    
    stringTable = Scriber.createStringTable(null); 
    
    int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
    prefixPartition = stringTable.getPrefixPartition(uriId);
    
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(1, prefixPartition.forwardedWidth);

    Assert.assertEquals(1, prefixPartition.n_strings);
    Assert.assertEquals(0, prefixPartition.getCompactId("xsi"));
    
    prefixPartition.addPrefix("a");
    Assert.assertEquals(2, prefixPartition.n_strings);
    Assert.assertEquals(1, prefixPartition.width);
    Assert.assertEquals(2, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("b");
    Assert.assertEquals(3, prefixPartition.n_strings);
    Assert.assertEquals(2, prefixPartition.width);
    Assert.assertEquals(2, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("c");
    Assert.assertEquals(4, prefixPartition.n_strings);
    Assert.assertEquals(2, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("d");
    Assert.assertEquals(5, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("e");
    Assert.assertEquals(6, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("f");
    Assert.assertEquals(7, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("g");
    Assert.assertEquals(8, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(4, prefixPartition.forwardedWidth);
    
    prefixPartition.reset();
    
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(1, prefixPartition.forwardedWidth);

    Assert.assertEquals(1, prefixPartition.n_strings);
    Assert.assertEquals(0, prefixPartition.getCompactId("xsi"));
  }
  
  /**
   */
  public void testXsdPrefixes() throws Exception {
    
    StringTable stringTable;
    StringTable.PrefixPartition prefixPartition;
    
    stringTable = Scriber.createStringTable(new GrammarCache(EmptySchema.getEXISchema())); 
    
    int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    prefixPartition = stringTable.getPrefixPartition(uriId);

    // There are no prefixes initially associated with the XML Schema namespace
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(0, prefixPartition.forwardedWidth);
    Assert.assertEquals(0, prefixPartition.n_strings);
    
    prefixPartition.addPrefix("a");
    Assert.assertEquals(1, prefixPartition.n_strings);
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(1, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("b");
    Assert.assertEquals(2, prefixPartition.n_strings);
    Assert.assertEquals(1, prefixPartition.width);
    Assert.assertEquals(2, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("c");
    Assert.assertEquals(3, prefixPartition.n_strings);
    Assert.assertEquals(2, prefixPartition.width);
    Assert.assertEquals(2, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("d");
    Assert.assertEquals(4, prefixPartition.n_strings);
    Assert.assertEquals(2, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("e");
    Assert.assertEquals(5, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("f");
    Assert.assertEquals(6, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("g");
    Assert.assertEquals(7, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(3, prefixPartition.forwardedWidth);
    prefixPartition.addPrefix("h");
    Assert.assertEquals(8, prefixPartition.n_strings);
    Assert.assertEquals(3, prefixPartition.width);
    Assert.assertEquals(4, prefixPartition.forwardedWidth);
    
    prefixPartition.reset();
    
    Assert.assertEquals(0, prefixPartition.width);
    Assert.assertEquals(0, prefixPartition.forwardedWidth);
    Assert.assertEquals(0, prefixPartition.n_strings);
  }

}
