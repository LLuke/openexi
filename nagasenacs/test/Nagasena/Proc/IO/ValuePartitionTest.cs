using System;
using NUnit.Framework;

using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;
using StringTable = Nagasena.Proc.Common.StringTable;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  [TestFixture]
  public class ValuePartitionTest : Nagasena.LocaleLauncher {

    private Characters createCharacters(string value) {
      char[] characters = value.ToCharArray();
      return new Characters(characters, 0, characters.Length, false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    [Test]
    public virtual void testValuePartition() {

      StringTable stringTable;
      StringTable.GlobalValuePartition globalPartition;

      stringTable = Scriber.createStringTable(new GrammarCache((EXISchema)null));
      Assert.AreEqual(EXIOptions.VALUE_PARTITION_CAPACITY_UNBOUNDED, stringTable.ValuePartitionCapacity);

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

      Assert.AreEqual(-1, stringTable.getCompactIdOfURI("urn:foo"));
      int fooId = stringTable.addURI("urn:foo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
      int foo_A = stringTable.getLocalNamePartition(fooId).addName("A", (IGrammar)null);
      int foo_B = stringTable.getLocalNamePartition(fooId).addName("B", (IGrammar)null);

      Assert.AreEqual(-1, stringTable.getCompactIdOfURI("urn:goo"));
      int gooId = stringTable.addURI("urn:goo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
      int goo_A = stringTable.getLocalNamePartition(gooId).addName("A", (IGrammar)null);

      entry = globalPartition.getEntry(characterSequence_a);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_a, foo_A, fooId);
      Assert.AreEqual(0, globalPartition.width);
      Assert.AreEqual(1, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_a);
      Assert.AreEqual(0, entry.number);
      Assert.AreEqual("a", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(0, localPartition.width);
      Assert.AreEqual(1, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(0, localEntry.number);
      Assert.AreEqual("a", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_b);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_b, foo_B, fooId);
      Assert.AreEqual(1, globalPartition.width);
      Assert.AreEqual(2, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_b);
      Assert.AreEqual(1, entry.number);
      Assert.AreEqual("b", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(0, localPartition.width);
      Assert.AreEqual(1, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(0, localEntry.number);
      Assert.AreEqual("b", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_c);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_c, foo_A, fooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(3, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_c);
      Assert.AreEqual(2, entry.number);
      Assert.AreEqual("c", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(1, localPartition.width);
      Assert.AreEqual(2, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(1, localEntry.number);
      Assert.AreEqual("c", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_d);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_d, goo_A, gooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(4, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_d);
      Assert.AreEqual(3, entry.number);
      Assert.AreEqual("d", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(0, localPartition.width);
      Assert.AreEqual(1, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(0, localEntry.number);
      Assert.AreEqual("d", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_e);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_e, foo_A, fooId);
      Assert.AreEqual(3, globalPartition.width);
      Assert.AreEqual(5, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_e);
      Assert.AreEqual(4, entry.number);
      Assert.AreEqual("e", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(2, localPartition.width);
      Assert.AreEqual(3, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(2, localEntry.number);
      Assert.AreEqual("e", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_f);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_f, foo_A, fooId);
      Assert.AreEqual(3, globalPartition.width);
      Assert.AreEqual(6, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_f);
      Assert.AreEqual(5, entry.number);
      Assert.AreEqual("f", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(2, localPartition.width);
      Assert.AreEqual(4, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(3, localEntry.number);
      Assert.AreEqual("f", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_g);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_g, foo_A, fooId);
      Assert.AreEqual(3, globalPartition.width);
      Assert.AreEqual(7, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_g);
      Assert.AreEqual(6, entry.number);
      Assert.AreEqual("g", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(3, localPartition.width);
      Assert.AreEqual(5, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(4, localEntry.number);
      Assert.AreEqual("g", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_h);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_h, goo_A, gooId);
      Assert.AreEqual(3, globalPartition.width);
      Assert.AreEqual(8, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_h);
      Assert.AreEqual(7, entry.number);
      Assert.AreEqual("h", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(1, localPartition.width);
      Assert.AreEqual(2, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(1, localEntry.number);
      Assert.AreEqual("h", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_i);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_i, goo_A, gooId);
      Assert.AreEqual(4, globalPartition.width);
      Assert.AreEqual(9, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_i);
      Assert.AreEqual(8, entry.number);
      Assert.AreEqual("i", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(2, localPartition.width);
      Assert.AreEqual(3, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(2, localEntry.number);
      Assert.AreEqual("i", localEntry.value.makeString());

      globalPartition.reset();
      Assert.AreEqual(0, globalPartition.width);
      Assert.AreEqual(0, globalPartition.globalID);
      Assert.IsNull(globalPartition.getEntry(characterSequence_a));
    }

    /// <summary>
    /// Set valuePartitionCapacity to 3 (three).
    /// </summary>
    [Test]
    public virtual void testValuePartitionCapacity_01() {

      StringTable stringTable;
      StringTable.GlobalValuePartition globalPartition;

      stringTable = Scriber.createStringTable(new GrammarCache((EXISchema)null));
      stringTable.ValuePartitionCapacity = 3;

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

      Assert.AreEqual(-1, stringTable.getCompactIdOfURI("urn:foo"));
      int fooId = stringTable.addURI("urn:foo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
      int foo_A = stringTable.getLocalNamePartition(fooId).addName("A", (IGrammar)null);
      int foo_B = stringTable.getLocalNamePartition(fooId).addName("B", (IGrammar)null);

      Assert.AreEqual(-1, stringTable.getCompactIdOfURI("urn:goo"));
      int gooId = stringTable.addURI("urn:goo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
      int goo_A = stringTable.getLocalNamePartition(gooId).addName("A", (IGrammar)null);

      Assert.AreEqual(-1, stringTable.getCompactIdOfURI("urn:hoo"));
      int hooId = stringTable.addURI("urn:hoo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
      int hoo_A = stringTable.getLocalNamePartition(hooId).addName("A", (IGrammar)null);

      entry = globalPartition.getEntry(characterSequence_a);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_a, foo_A, fooId);
      Assert.AreEqual(0, globalPartition.width);
      Assert.AreEqual(1, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_a);
      Assert.AreEqual(0, entry.number);
      Assert.AreEqual("a", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(0, localPartition.width);
      Assert.AreEqual(1, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(0, localEntry.number);
      Assert.AreEqual("a", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_b);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_b, foo_B, fooId);
      Assert.AreEqual(1, globalPartition.width);
      Assert.AreEqual(2, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_b);
      Assert.AreEqual(1, entry.number);
      Assert.AreEqual("b", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(0, localPartition.width);
      Assert.AreEqual(1, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(0, localEntry.number);
      Assert.AreEqual("b", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_c);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_c, foo_A, fooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(0, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_c);
      Assert.AreEqual(2, entry.number);
      Assert.AreEqual("c", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(1, localPartition.width);
      Assert.AreEqual(2, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(1, localEntry.number);
      Assert.AreEqual("c", localEntry.value.makeString());

      entry = globalPartition.getEntry(characterSequence_a);
      Assert.AreEqual(0, entry.number);
      localPartitionSaved = entry.localPartition;
      localNumberSaved = entry.localEntry.number;

      entry = globalPartition.getEntry(characterSequence_d);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_d, goo_A, gooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(1, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_d);
      Assert.AreEqual(0, entry.number);
      Assert.AreEqual("d", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(0, localPartition.width);
      Assert.AreEqual(1, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(0, localEntry.number);
      Assert.AreEqual("d", localEntry.value.makeString());

      Assert.IsNull(globalPartition.getEntry(characterSequence_a));
      Assert.IsNull(localPartitionSaved.valueEntries[localNumberSaved]);

      entry = globalPartition.getEntry(characterSequence_b);
      Assert.AreEqual(1, entry.number);
      localPartitionSaved = entry.localPartition;
      localNumberSaved = entry.localEntry.number;

      entry = globalPartition.getEntry(characterSequence_e);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_e, foo_A, fooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(2, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_e);
      Assert.AreEqual(1, entry.number);
      Assert.AreEqual("e", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(2, localPartition.width);
      Assert.AreEqual(3, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(2, localEntry.number);
      Assert.AreEqual("e", localEntry.value.makeString());

      Assert.IsNull(globalPartition.getEntry(characterSequence_b));
      Assert.IsNull(localPartitionSaved.valueEntries[localNumberSaved]);

      entry = globalPartition.getEntry(characterSequence_c);
      Assert.AreEqual(2, entry.number);
      localPartitionSaved = entry.localPartition;
      localNumberSaved = entry.localEntry.number;

      entry = globalPartition.getEntry(characterSequence_f);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_f, foo_A, fooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(0, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_f);
      Assert.AreEqual(2, entry.number);
      Assert.AreEqual("f", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(2, localPartition.width);
      Assert.AreEqual(4, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(3, localEntry.number);
      Assert.AreEqual("f", localEntry.value.makeString());

      Assert.IsNull(globalPartition.getEntry(characterSequence_c));
      Assert.IsNull(localPartitionSaved.valueEntries[localNumberSaved]);

      entry = globalPartition.getEntry(characterSequence_d);
      Assert.AreEqual(0, entry.number);
      localPartitionSaved = entry.localPartition;
      localNumberSaved = entry.localEntry.number;

      entry = globalPartition.getEntry(characterSequence_g);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_g, foo_A, fooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(1, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_g);
      Assert.AreEqual(0, entry.number);
      Assert.AreEqual("g", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(3, localPartition.width);
      Assert.AreEqual(5, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(4, localEntry.number);
      Assert.AreEqual("g", localEntry.value.makeString());

      Assert.IsNull(globalPartition.getEntry(characterSequence_d));
      Assert.IsNull(localPartitionSaved.valueEntries[localNumberSaved]);

      entry = globalPartition.getEntry(characterSequence_e);
      Assert.AreEqual(1, entry.number);
      localPartitionSaved = entry.localPartition;
      localNumberSaved = entry.localEntry.number;

      entry = globalPartition.getEntry(characterSequence_h);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_h, goo_A, gooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(2, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_h);
      Assert.AreEqual(1, entry.number);
      Assert.AreEqual("h", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(1, localPartition.width);
      Assert.AreEqual(2, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(1, localEntry.number);
      Assert.AreEqual("h", localEntry.value.makeString());

      Assert.IsNull(globalPartition.getEntry(characterSequence_e));
      Assert.IsNull(localPartitionSaved.valueEntries[localNumberSaved]);

      entry = globalPartition.getEntry(characterSequence_f);
      Assert.AreEqual(2, entry.number);
      localPartitionSaved = entry.localPartition;
      localNumberSaved = entry.localEntry.number;

      entry = globalPartition.getEntry(characterSequence_i);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_i, goo_A, gooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(0, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_i);
      Assert.AreEqual(2, entry.number);
      Assert.AreEqual("i", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(2, localPartition.width);
      Assert.AreEqual(3, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(2, localEntry.number);
      Assert.AreEqual("i", localEntry.value.makeString());

      Assert.IsNull(globalPartition.getEntry(characterSequence_f));
      Assert.IsNull(localPartitionSaved.valueEntries[localNumberSaved]);

      entry = globalPartition.getEntry(characterSequence_g);
      Assert.AreEqual(0, entry.number);
      localPartitionSaved = entry.localPartition;
      localNumberSaved = entry.localEntry.number;

      entry = globalPartition.getEntry(characterSequence_j);
      Assert.IsNull(entry);
      globalPartition.addValue(characterSequence_j, hoo_A, hooId);
      Assert.AreEqual(2, globalPartition.width);
      Assert.AreEqual(1, globalPartition.globalID);
      entry = globalPartition.getEntry(characterSequence_j);
      Assert.AreEqual(0, entry.number);
      Assert.AreEqual("j", entry.value.makeString());
      localPartition = entry.localPartition;
      Assert.AreEqual(0, localPartition.width);
      Assert.AreEqual(1, localPartition.n_strings);
      localEntry = entry.localEntry;
      Assert.AreEqual(0, localEntry.number);
      Assert.AreEqual("j", localEntry.value.makeString());

      Assert.IsNull(globalPartition.getEntry(characterSequence_g));
      Assert.IsNull(localPartitionSaved.valueEntries[localNumberSaved]);

      globalPartition.reset();
      Assert.AreEqual(0, globalPartition.width);
      Assert.AreEqual(0, globalPartition.globalID);
      Assert.IsNull(globalPartition.getEntry(characterSequence_a));
    }

    /// <summary>
    /// Set valuePartitionCapacity to zero.
    /// </summary>
    [Test]
    public virtual void testValuePartitionCapacity_02() {

      StringTable stringTable;
      StringTable.GlobalValuePartition globalPartition;

      stringTable = Scriber.createStringTable(new GrammarCache((EXISchema)null));
      stringTable.ValuePartitionCapacity = 0;

      globalPartition = stringTable.globalValuePartition;

      Characters characterSequence_a = createCharacters("a");

      Assert.AreEqual(-1, stringTable.getCompactIdOfURI("urn:foo"));
      int fooId = stringTable.addURI("urn:foo", (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
      int foo_A = stringTable.getLocalNamePartition(fooId).addName("A", (IGrammar)null);

      Assert.IsNull(globalPartition.getEntry(characterSequence_a));
      globalPartition.addValue(characterSequence_a, foo_A, fooId);
      Assert.AreEqual(0, globalPartition.width);
      Assert.AreEqual(0, globalPartition.globalID);
      Assert.IsNull(globalPartition.getEntry(characterSequence_a));
    }

  }

}