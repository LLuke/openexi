package org.openexi.fujitsu.proc.io;

import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EXIOptions;
import org.openexi.fujitsu.schema.EXISchema;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ValuePartitionTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private CharacterSequence createCharacterSequence(String value) {
    final char[] characters = value.toCharArray();
    return new Characters(characters, 0, characters.length);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testValuePartition() throws Exception {
    
    StringTable stringTable;
    StringTable.GlobalPartition globalPartition;
    
    stringTable = new StringTable((EXISchema)null);
    Assert.assertEquals(EXIOptions.VALUE_PARTITION_CAPACITY_UNBOUNDED, stringTable.getValuePartitionCapacity());
    
    globalPartition = stringTable.getGlobalPartition();
    
    StringTable.GlobalEntry entry;
    StringTable.LocalPartition localPartition;
    StringTable.NumberedCharacters localEntry;
    
    CharacterSequence characterSequence_a = createCharacterSequence("a");
    CharacterSequence characterSequence_b = createCharacterSequence("b");
    CharacterSequence characterSequence_c = createCharacterSequence("c");
    CharacterSequence characterSequence_d = createCharacterSequence("d");
    CharacterSequence characterSequence_e = createCharacterSequence("e");
    CharacterSequence characterSequence_f = createCharacterSequence("f");
    CharacterSequence characterSequence_g = createCharacterSequence("g");
    CharacterSequence characterSequence_h = createCharacterSequence("h");
    CharacterSequence characterSequence_i = createCharacterSequence("i");
    
    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_a, "A", "urn:foo");
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("a", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("a", localEntry.value.makeString());
    
    entry = globalPartition.getEntry(characterSequence_b);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_b, "B", "urn:foo");
    Assert.assertEquals(1, globalPartition.width);
    Assert.assertEquals(2, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_b);
    Assert.assertEquals(1, entry.number);
    Assert.assertEquals("b", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("B", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("b", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_c, "A", "urn:foo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(3, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertEquals(2, entry.number);
    Assert.assertEquals("c", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(1, localPartition.width);
    Assert.assertEquals(2, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(1, localEntry.number);
    Assert.assertEquals("c", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_d, "A", "urn:goo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(4, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertEquals(3, entry.number);
    Assert.assertEquals("d", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:goo", localPartition.uri);
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("d", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_e, "A", "urn:foo");
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(5, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertEquals(4, entry.number);
    Assert.assertEquals("e", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("e", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_f, "A", "urn:foo");
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(6, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertEquals(5, entry.number);
    Assert.assertEquals("f", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(4, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(3, localEntry.number);
    Assert.assertEquals("f", localEntry.value.makeString());
    
    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_g, "A", "urn:foo");
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(7, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertEquals(6, entry.number);
    Assert.assertEquals("g", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(3, localPartition.width);
    Assert.assertEquals(5, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(4, localEntry.number);
    Assert.assertEquals("g", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_h, "A", "urn:goo");
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(8, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertEquals(7, entry.number);
    Assert.assertEquals("h", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:goo", localPartition.uri);
    Assert.assertEquals(1, localPartition.width);
    Assert.assertEquals(2, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(1, localEntry.number);
    Assert.assertEquals("h", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_i, "A", "urn:goo");
    Assert.assertEquals(4, globalPartition.width);
    Assert.assertEquals(9, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertEquals(8, entry.number);
    Assert.assertEquals("i", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:goo", localPartition.uri);
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("i", localEntry.value.makeString());
    
    globalPartition.clear();
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
  }

  /**
   * Set valuePartitionCapacity to 3 (three).
   */
  public void testValuePartitionCapacity_01() throws Exception {
    
    StringTable stringTable;
    StringTable.GlobalPartition globalPartition;
    
    stringTable = new StringTable((EXISchema)null);
    stringTable.setValuePartitionCapacity(3);
    
    globalPartition = stringTable.getGlobalPartition();
    
    StringTable.GlobalEntry entry;
    StringTable.LocalPartition localPartition;
    StringTable.NumberedCharacters localEntry;
    
    StringTable.LocalPartition localPartitionSaved;
    int localNumberSaved;

    CharacterSequence characterSequence_a = createCharacterSequence("a");
    CharacterSequence characterSequence_b = createCharacterSequence("b");
    CharacterSequence characterSequence_c = createCharacterSequence("c");
    CharacterSequence characterSequence_d = createCharacterSequence("d");
    CharacterSequence characterSequence_e = createCharacterSequence("e");
    CharacterSequence characterSequence_f = createCharacterSequence("f");
    CharacterSequence characterSequence_g = createCharacterSequence("g");
    CharacterSequence characterSequence_h = createCharacterSequence("h");
    CharacterSequence characterSequence_i = createCharacterSequence("i");
    CharacterSequence characterSequence_j = createCharacterSequence("j");

    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_a, "A", "urn:foo");
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("a", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("a", localEntry.value.makeString());
    
    entry = globalPartition.getEntry(characterSequence_b);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_b, "B", "urn:foo");
    Assert.assertEquals(1, globalPartition.width);
    Assert.assertEquals(2, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_b);
    Assert.assertEquals(1, entry.number);
    Assert.assertEquals("b", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("B", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("b", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_c, "A", "urn:foo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertEquals(2, entry.number);
    Assert.assertEquals("c", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(1, localPartition.width);
    Assert.assertEquals(2, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(1, localEntry.number);
    Assert.assertEquals("c", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertEquals(0, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_d, "A", "urn:goo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("d", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:goo", localPartition.uri);
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("d", localEntry.value.makeString());
    
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
    Assert.assertNull(localPartitionSaved.getEntry(localNumberSaved));

    entry = globalPartition.getEntry(characterSequence_b);
    Assert.assertEquals(1, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_e, "A", "urn:foo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(2, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertEquals(1, entry.number);
    Assert.assertEquals("e", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("e", localEntry.value.makeString());

    Assert.assertNull(globalPartition.getEntry(characterSequence_b));
    Assert.assertNull(localPartitionSaved.getEntry(localNumberSaved));

    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertEquals(2, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_f, "A", "urn:foo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertEquals(2, entry.number);
    Assert.assertEquals("f", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(4, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(3, localEntry.number);
    Assert.assertEquals("f", localEntry.value.makeString());
    
    Assert.assertNull(globalPartition.getEntry(characterSequence_c));
    Assert.assertNull(localPartitionSaved.getEntry(localNumberSaved));

    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertEquals(0, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_g, "A", "urn:foo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("g", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:foo", localPartition.uri);
    Assert.assertEquals(3, localPartition.width);
    Assert.assertEquals(5, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(4, localEntry.number);
    Assert.assertEquals("g", localEntry.value.makeString());

    Assert.assertNull(globalPartition.getEntry(characterSequence_d));
    Assert.assertNull(localPartitionSaved.getEntry(localNumberSaved));

    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertEquals(1, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_h, "A", "urn:goo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(2, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertEquals(1, entry.number);
    Assert.assertEquals("h", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:goo", localPartition.uri);
    Assert.assertEquals(1, localPartition.width);
    Assert.assertEquals(2, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(1, localEntry.number);
    Assert.assertEquals("h", localEntry.value.makeString());

    Assert.assertNull(globalPartition.getEntry(characterSequence_e));
    Assert.assertNull(localPartitionSaved.getEntry(localNumberSaved));

    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertEquals(2, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_i, "A", "urn:goo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertEquals(2, entry.number);
    Assert.assertEquals("i", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:goo", localPartition.uri);
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("i", localEntry.value.makeString());
    
    Assert.assertNull(globalPartition.getEntry(characterSequence_f));
    Assert.assertNull(localPartitionSaved.getEntry(localNumberSaved));

    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertEquals(0, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_j);
    Assert.assertNull(entry);
    globalPartition.addString(characterSequence_j, "A", "urn:hoo");
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_j);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("j", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals("A", localPartition.name);
    Assert.assertEquals("urn:hoo", localPartition.uri);
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("j", localEntry.value.makeString());

    Assert.assertNull(globalPartition.getEntry(characterSequence_g));
    Assert.assertNull(localPartitionSaved.getEntry(localNumberSaved));

    globalPartition.clear();
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
  }

  /**
   * Set valuePartitionCapacity to zero.
   */
  public void testValuePartitionCapacity_02() throws Exception {
    
    StringTable stringTable;
    StringTable.GlobalPartition globalPartition;
    
    stringTable = new StringTable((EXISchema)null);
    stringTable.setValuePartitionCapacity(0);
    
    globalPartition = stringTable.getGlobalPartition();
    
    CharacterSequence characterSequence_a = createCharacterSequence("a");

    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
    globalPartition.addString(characterSequence_a, "A", "urn:foo");
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
  }

}
