package com.sumerogi.proc.common;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NamePartitionTest extends TestCase {
  
  public NamePartitionTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testXmlNames() throws Exception {
    
    StringTable localNamePartition;
    
    localNamePartition = new StringTable(StringTable.Usage.decoding); 
    
    Assert.assertEquals(0, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.width);

    localNamePartition.addName("a");
    Assert.assertEquals(1, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.width);
    localNamePartition.addName("b");
    Assert.assertEquals(2, localNamePartition.n_strings);
    Assert.assertEquals(1, localNamePartition.width);
    localNamePartition.addName("c");
    Assert.assertEquals(3, localNamePartition.n_strings);
    Assert.assertEquals(2, localNamePartition.width);
    localNamePartition.addName("d");
    Assert.assertEquals(4, localNamePartition.n_strings);
    Assert.assertEquals(2, localNamePartition.width);
    localNamePartition.addName("e");
    Assert.assertEquals(5, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    
    localNamePartition.reset();

    localNamePartition.addName("a");
    Assert.assertEquals(1, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.width);
    localNamePartition.addName("b");
    Assert.assertEquals(2, localNamePartition.n_strings);
    Assert.assertEquals(1, localNamePartition.width);
    localNamePartition.addName("c");
    Assert.assertEquals(3, localNamePartition.n_strings);
    Assert.assertEquals(2, localNamePartition.width);
    localNamePartition.addName("d");
    Assert.assertEquals(4, localNamePartition.n_strings);
    Assert.assertEquals(2, localNamePartition.width);
    localNamePartition.addName("e");
    Assert.assertEquals(5, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
  }

}
