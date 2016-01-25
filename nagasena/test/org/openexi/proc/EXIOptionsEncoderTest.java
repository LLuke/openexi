package org.openexi.proc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.util.ExiUriConst;

public class EXIOptionsEncoderTest extends TestCase {

  public EXIOptionsEncoderTest(String name) {
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
   * compression with blockSize 1000001
   */
  public void testBlockSize_01() throws Exception {
    
    EXIOptionsEncoder optionsEncoder = new EXIOptionsEncoder();
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    EXIOptions options = new EXIOptions();
    options.setAlignmentType(AlignmentType.compress);
    options.setBlockSize(1000001);
    
    optionsEncoder.encode(options, false, false, baos).flush();
    
    baos.close();
    
    byte[] bts = baos.toByteArray();
    InputStream inputStream = new ByteArrayInputStream(bts);
    
    HashMap<String,Object> optionsMap = HeaderOptionsUtil.decode(inputStream);
    inputStream.close();
    
    Assert.assertTrue(optionsMap.containsKey("compression"));
    Assert.assertEquals("1000001", (String)optionsMap.get("blockSize"));
  }

  /**
   * compression with blockSize 1000000 (default value)
   */
  public void testBlockSize_02() throws Exception {
    
    EXIOptionsEncoder optionsEncoder = new EXIOptionsEncoder();
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    EXIOptions options = new EXIOptions();
    options.setAlignmentType(AlignmentType.compress);
    options.setBlockSize(1000000);
    
    optionsEncoder.encode(options, false, false, baos).flush();
    
    baos.close();
    
    byte[] bts = baos.toByteArray();
    InputStream inputStream = new ByteArrayInputStream(bts);
    
    HashMap<String,Object> optionsMap = HeaderOptionsUtil.decode(inputStream);
    inputStream.close();
    
    Assert.assertTrue(optionsMap.containsKey("compression"));
    Assert.assertFalse(optionsMap.containsKey("blockSize"));
  }

  /**
   * bitPacked with blockSize 1000001. 
   * blockSize does not get encoded.
   */
  public void testBlockSize_03() throws Exception {
    
    EXIOptionsEncoder optionsEncoder = new EXIOptionsEncoder();
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    EXIOptions options = new EXIOptions();
    options.setAlignmentType(AlignmentType.bitPacked);
    options.setBlockSize(1000001);
    
    optionsEncoder.encode(options, false, false, baos).flush();
    
    baos.close();
    
    byte[] bts = baos.toByteArray();
    InputStream inputStream = new ByteArrayInputStream(bts);
    
    HashMap<String,Object> optionsMap = HeaderOptionsUtil.decode(inputStream);
    inputStream.close();
    
    Assert.assertFalse(optionsMap.containsKey("blockSize"));
  }

  /**
   * Empty container elements are omitted. 
   */
  public void testEmptyContainers_01() throws Exception {
    
    EXIOptionsEncoder optionsEncoder = new EXIOptionsEncoder();
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    EXIOptions options = new EXIOptions();
    
    optionsEncoder.encode(options, false, false, baos).flush();
    
    baos.close();
    
    byte[] bts = baos.toByteArray();
    InputStream inputStream = new ByteArrayInputStream(bts);
    
    HashMap<String,Object> optionsMap = HeaderOptionsUtil.decode(inputStream);
    inputStream.close();
    
    Assert.assertFalse(optionsMap.containsKey("common"));
    Assert.assertFalse(optionsMap.containsKey("lesscommon"));
    Assert.assertFalse(optionsMap.containsKey("uncommon"));
  }
  
  /**
   * datatypeRepresentationMap does not get output in the header  when lexicalValues is present.
   */
  public void testDTRM_01() throws Exception {
    
    EXIOptionsEncoder optionsEncoder = new EXIOptionsEncoder();
    
    ByteArrayOutputStream baos;
    EXIOptions options;
    byte[] bts;
    InputStream inputStream;
    HashMap<String,Object> optionsMap;
    
    baos = new ByteArrayOutputStream();
    options = new EXIOptions();
    
    options.setPreserveLexicalValues(true);
    options.appendDatatypeRepresentationMap(XmlUriConst.W3C_2001_XMLSCHEMA_URI, "xsd:boolean" , ExiUriConst.W3C_2009_EXI_URI, "exi:integer");

    optionsEncoder.encode(options, false, false, baos).flush();
    
    baos.close();
    
    bts = baos.toByteArray();
    inputStream = new ByteArrayInputStream(bts);
    
    optionsMap = HeaderOptionsUtil.decode(inputStream);
    inputStream.close();

    Assert.assertTrue(optionsMap.containsKey("lexicalValues"));
    Assert.assertFalse(optionsMap.containsKey("datatypeRepresentationMap"));
    
    baos = new ByteArrayOutputStream();
    options = new EXIOptions();
    
    
    options.setPreserveLexicalValues(false);
    options.appendDatatypeRepresentationMap(XmlUriConst.W3C_2001_XMLSCHEMA_URI, "xsd:boolean" , ExiUriConst.W3C_2009_EXI_URI, "exi:integer");

    optionsEncoder.encode(options, false, false, baos).flush();
    
    baos.close();
    
    bts = baos.toByteArray();
    inputStream = new ByteArrayInputStream(bts);
    
    optionsMap = HeaderOptionsUtil.decode(inputStream);
    inputStream.close();
    
    // datatypeRepresentationMap gets output when lexicalValues is not present.
    Assert.assertFalse(optionsMap.containsKey("lexicalValues"));
    Assert.assertTrue(optionsMap.containsKey("datatypeRepresentationMap"));
  }

}
