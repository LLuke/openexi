package org.openexi.proc.io;

import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.IGrammar;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;

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
    
    stringTable = Scriber.createStringTable(new GrammarCache((EXISchema)null)); 
    Assert.assertEquals(EXIOptions.VALUE_PARTITION_CAPACITY_UNBOUNDED, stringTable.getValuePartitionCapacity());
    
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
    
    Assert.assertEquals(-1, stringTable.getCompactIdOfURI("urn:foo"));
    final int fooId = stringTable.addURI("urn:foo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
    final int foo_A = stringTable.getLocalNamePartition(fooId).addName("A", (IGrammar)null);
    final int foo_B = stringTable.getLocalNamePartition(fooId).addName("B", (IGrammar)null);

    Assert.assertEquals(-1, stringTable.getCompactIdOfURI("urn:goo"));
    final int gooId = stringTable.addURI("urn:goo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
    final int goo_A = stringTable.getLocalNamePartition(gooId).addName("A", (IGrammar)null);

    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_a, foo_A, fooId);
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
    globalPartition.addValue(characterSequence_b, foo_B, fooId);
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
    globalPartition.addValue(characterSequence_c, foo_A, fooId);
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
    globalPartition.addValue(characterSequence_d, goo_A, gooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(4, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertEquals(3, entry.number);
    Assert.assertEquals("d", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("d", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_e, foo_A, fooId);
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(5, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertEquals(4, entry.number);
    Assert.assertEquals("e", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("e", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_f, foo_A, fooId);
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(6, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertEquals(5, entry.number);
    Assert.assertEquals("f", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(4, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(3, localEntry.number);
    Assert.assertEquals("f", localEntry.value.makeString());
    
    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_g, foo_A, fooId);
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(7, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertEquals(6, entry.number);
    Assert.assertEquals("g", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(3, localPartition.width);
    Assert.assertEquals(5, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(4, localEntry.number);
    Assert.assertEquals("g", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_h, goo_A, gooId);
    Assert.assertEquals(3, globalPartition.width);
    Assert.assertEquals(8, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertEquals(7, entry.number);
    Assert.assertEquals("h", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(1, localPartition.width);
    Assert.assertEquals(2, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(1, localEntry.number);
    Assert.assertEquals("h", localEntry.value.makeString());

    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_i, goo_A, gooId);
    Assert.assertEquals(4, globalPartition.width);
    Assert.assertEquals(9, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertEquals(8, entry.number);
    Assert.assertEquals("i", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("i", localEntry.value.makeString());
    
    globalPartition.reset();
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
  }

  /**
   * Set valuePartitionCapacity to 3 (three).
   */
  public void testValuePartitionCapacity_01() throws Exception {
    
    StringTable stringTable;
    StringTable.GlobalValuePartition globalPartition;
    
    stringTable = Scriber.createStringTable(new GrammarCache((EXISchema)null)); 
    stringTable.setValuePartitionCapacity(3);
    
    globalPartition = stringTable.globalValuePartition;
    
    StringTable.GlobalEntry entry;
    StringTable.LocalValuePartition localPartition;
    StringTable.NumberedCharacters localEntry;
    
    StringTable.LocalValuePartition localPartitionSaved;
    int localNumberSaved;

    Characters characterSequence_a = createCharacters("a");
    Characters characterSequence_b = createCharacters("b");
    Characters characterSequence_c = createCharacters("c");
    Characters characterSequence_d = createCharacters("d");
    Characters characterSequence_e = createCharacters("e");
    Characters characterSequence_f = createCharacters("f");
    Characters characterSequence_g = createCharacters("g");
    Characters characterSequence_h = createCharacters("h");
    Characters characterSequence_i = createCharacters("i");
    Characters characterSequence_j = createCharacters("j");

    Assert.assertEquals(-1, stringTable.getCompactIdOfURI("urn:foo"));
    final int fooId = stringTable.addURI("urn:foo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
    final int foo_A = stringTable.getLocalNamePartition(fooId).addName("A", (IGrammar)null);
    final int foo_B = stringTable.getLocalNamePartition(fooId).addName("B", (IGrammar)null);

    Assert.assertEquals(-1, stringTable.getCompactIdOfURI("urn:goo"));
    final int gooId = stringTable.addURI("urn:goo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
    final int goo_A = stringTable.getLocalNamePartition(gooId).addName("A", (IGrammar)null);

    Assert.assertEquals(-1, stringTable.getCompactIdOfURI("urn:hoo"));
    final int hooId = stringTable.addURI("urn:hoo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
    final int hoo_A = stringTable.getLocalNamePartition(hooId).addName("A", (IGrammar)null);

    entry = globalPartition.getEntry(characterSequence_a);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_a, foo_A, fooId);
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
    globalPartition.addValue(characterSequence_b, foo_B, fooId);
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
    globalPartition.addValue(characterSequence_c, foo_A, fooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertEquals(2, entry.number);
    Assert.assertEquals("c", entry.value.makeString());
    localPartition = entry.localPartition;
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
    globalPartition.addValue(characterSequence_d, goo_A, gooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("d", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("d", localEntry.value.makeString());
    
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
    Assert.assertNull(localPartitionSaved.valueEntries[localNumberSaved]);

    entry = globalPartition.getEntry(characterSequence_b);
    Assert.assertEquals(1, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_e, foo_A, fooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(2, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertEquals(1, entry.number);
    Assert.assertEquals("e", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("e", localEntry.value.makeString());

    Assert.assertNull(globalPartition.getEntry(characterSequence_b));
    Assert.assertNull(localPartitionSaved.valueEntries[localNumberSaved]);

    entry = globalPartition.getEntry(characterSequence_c);
    Assert.assertEquals(2, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_f, foo_A, fooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertEquals(2, entry.number);
    Assert.assertEquals("f", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(4, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(3, localEntry.number);
    Assert.assertEquals("f", localEntry.value.makeString());
    
    Assert.assertNull(globalPartition.getEntry(characterSequence_c));
    Assert.assertNull(localPartitionSaved.valueEntries[localNumberSaved]);

    entry = globalPartition.getEntry(characterSequence_d);
    Assert.assertEquals(0, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_g, foo_A, fooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("g", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(3, localPartition.width);
    Assert.assertEquals(5, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(4, localEntry.number);
    Assert.assertEquals("g", localEntry.value.makeString());

    Assert.assertNull(globalPartition.getEntry(characterSequence_d));
    Assert.assertNull(localPartitionSaved.valueEntries[localNumberSaved]);

    entry = globalPartition.getEntry(characterSequence_e);
    Assert.assertEquals(1, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_h, goo_A, gooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(2, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_h);
    Assert.assertEquals(1, entry.number);
    Assert.assertEquals("h", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(1, localPartition.width);
    Assert.assertEquals(2, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(1, localEntry.number);
    Assert.assertEquals("h", localEntry.value.makeString());

    Assert.assertNull(globalPartition.getEntry(characterSequence_e));
    Assert.assertNull(localPartitionSaved.valueEntries[localNumberSaved]);

    entry = globalPartition.getEntry(characterSequence_f);
    Assert.assertEquals(2, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_i, goo_A, gooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_i);
    Assert.assertEquals(2, entry.number);
    Assert.assertEquals("i", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(2, localPartition.width);
    Assert.assertEquals(3, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(2, localEntry.number);
    Assert.assertEquals("i", localEntry.value.makeString());
    
    Assert.assertNull(globalPartition.getEntry(characterSequence_f));
    Assert.assertNull(localPartitionSaved.valueEntries[localNumberSaved]);

    entry = globalPartition.getEntry(characterSequence_g);
    Assert.assertEquals(0, entry.number);
    localPartitionSaved = entry.localPartition;
    localNumberSaved = entry.localEntry.number;

    entry = globalPartition.getEntry(characterSequence_j);
    Assert.assertNull(entry);
    globalPartition.addValue(characterSequence_j, hoo_A, hooId);
    Assert.assertEquals(2, globalPartition.width);
    Assert.assertEquals(1, globalPartition.globalID);
    entry = globalPartition.getEntry(characterSequence_j);
    Assert.assertEquals(0, entry.number);
    Assert.assertEquals("j", entry.value.makeString());
    localPartition = entry.localPartition;
    Assert.assertEquals(0, localPartition.width);
    Assert.assertEquals(1, localPartition.n_strings);
    localEntry = entry.localEntry;
    Assert.assertEquals(0, localEntry.number);
    Assert.assertEquals("j", localEntry.value.makeString());

    Assert.assertNull(globalPartition.getEntry(characterSequence_g));
    Assert.assertNull(localPartitionSaved.valueEntries[localNumberSaved]);

    globalPartition.reset();
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
  }

  /**
   * Set valuePartitionCapacity to zero.
   */
  public void testValuePartitionCapacity_02() throws Exception {
    
    StringTable stringTable;
    StringTable.GlobalValuePartition globalPartition;
    
    stringTable = Scriber.createStringTable(new GrammarCache((EXISchema)null)); 
    stringTable.setValuePartitionCapacity(0);
    
    globalPartition = stringTable.globalValuePartition;
    
    Characters characterSequence_a = createCharacters("a");

    Assert.assertEquals(-1, stringTable.getCompactIdOfURI("urn:foo"));
    final int fooId = stringTable.addURI("urn:foo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
    final int foo_A = stringTable.getLocalNamePartition(fooId).addName("A", (IGrammar)null);
    
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
    globalPartition.addValue(characterSequence_a, foo_A, fooId);
    Assert.assertEquals(0, globalPartition.width);
    Assert.assertEquals(0, globalPartition.globalID);
    Assert.assertNull(globalPartition.getEntry(characterSequence_a));
  }

}
