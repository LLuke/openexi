package com.sumerogi.proc.common;

import com.sumerogi.proc.common.StringTable;
import com.sumerogi.schema.Characters;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ValuePartitionTest extends TestCase {

  private Characters createCharacters(String value) {
    final char[] characters = value.toCharArray();
    return new Characters(characters, 0, characters.length, false);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testValuePartition() throws Exception {
    
    StringTable stringTable;
    StringTable.GlobalValuePartition globalPartition;
    
    stringTable = new StringTable(StringTable.Usage.encoding); 
    
    globalPartition = stringTable.globalValuePartition;
    
    StringTable.GlobalEntry entry;
    StringTable.LocalValuePartition localPartition;
    StringTable.NumberedCharacters localEntry;
    
    Characters characterSequence_a = createCharacters("a");
    Characters characterSequence_b = createCharacters("b");
    Characters characterSequence_c = createCharacters("c");
    Characters characterSequence_d = createCharacters("d");
    Characters characterSequence_e = createCharacters("e");
    Characters characterSequence_f = createCharacters("f");
    Characters characterSequence_g = createCharacters("g");
    Characters characterSequence_h = createCharacters("h");
    Characters characterSequence_i = createCharacters("i");
    
    final int name_A = stringTable.addName("A");
    final int name_B = stringTable.addName("B");

    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_a, name_A);
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("a", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("a", localEntry.value.makeString());
    
    entry = globalPartition.getEntry(characterSequence_b);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_b, name_B);
    Assert.assertEquals(1, globalPartition.width);
    Assert.assertEquals(2, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_b);
    Assert.assertEquals(1, entry.number);
    Assert.assertEquals("b", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("b", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_c, name_A);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(3, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertEquals(2, entry.number);
    Assert.assertEquals("c", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(1, localPartition.width);
    Assert.assertEquals(2, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(1, localEntry.number);
    Assert.assertEquals("c", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_d, name_A);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(4, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertEquals(3, entry.number);
    Assert.assertEquals("d", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("d", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_e, name_A);
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(5, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertEquals(4, entry.number);
    Assert.assertEquals("e", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(4, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(3, localEntry.number);
    Assert.assertEquals("e", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_f, name_A);
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(6, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertEquals(5, entry.number);
    Assert.assertEquals("f", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(3, localPartition.width);
    Assert.assertEquals(5, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(4, localEntry.number);
    Assert.assertEquals("f", localEntry.value.makeString());
    
    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_g, name_A);
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(7, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertEquals(6, entry.number);
    Assert.assertEquals("g", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(3, localPartition.width);
    Assert.assertEquals(6, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(5, localEntry.number);
    Assert.assertEquals("g", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_h, name_A);
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(8, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertEquals(7, entry.number);
    Assert.assertEquals("h", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(3, localPartition.width);
    Assert.assertEquals(7, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(6, localEntry.number);
    Assert.assertEquals("h", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_i, name_A);
    Assert.assertEquals(4, globalPartition.width);
    Assert.assertEquals(9, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertEquals(8, entry.number);
    Assert.assertEquals("i", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(3, localPartition.width);
    Assert.assertEquals(8, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(7, localEntry.number);
    Assert.assertEquals("i", localEntry.value.makeString());
    
    globalPartition.reset();
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
  }

}
