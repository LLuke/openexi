package com.sumerogi.proc.common;

import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;

import com.sumerogi.schema.Characters;

public final class StringTable {
  
  public final GlobalValuePartition globalValuePartition;
  
  private final LinkedList<Channel> channels;
  private int n_channels;

  public enum Usage {
    decoding,
    encoding
  };
  
  private final boolean m_useMap;
  
  ///////////////////////////////////////////////////////////////////////////
  // URIPartition fields
  ///////////////////////////////////////////////////////////////////////////

  final public LocalNameEntry documentLocalNameEntry;
  public LocalNameEntry[] localNameEntries;
  
  public int n_strings = 0;
  public int width = 0;
  private int m_milestone = 1;

  private final int m_start_n_strings;
  private final int m_start_width;
  private final int m_start_milestone;

  private final HashMap<String,LocalNameEntry> m_nameMap;
  
  // Registry of touched (i.e. modified) LocalValuePartitions
  private int m_n_localValuePartitionsTouched;
  private LocalValuePartition[] m_localValuePartitionsTouched;

  // Registry of touched (i.e. modified) BuiltinElementGrammars
  private int m_n_grammarsTouched;
  private IGrammar[] m_grammarsTouched;
  
  public static final int NAME_DOCUMENT = Integer.MIN_VALUE;
  public static final int NAME_NONE = -1;

  ///////////////////////////////////////////////////////////////////////////
  // Constructor
  ///////////////////////////////////////////////////////////////////////////

  public StringTable(Usage usage) {
    m_useMap = usage == Usage.encoding;
    globalValuePartition = new GlobalValuePartition();
    channels = new LinkedList<Channel>();
    n_channels = 0;

    if (m_useMap) {
      m_nameMap = new HashMap<String,LocalNameEntry>();
    }
    else {
      m_nameMap = null;
    }
    documentLocalNameEntry = new LocalNameEntry("", NAME_DOCUMENT);
    localNameEntries = new LocalNameEntry[256];
    m_start_n_strings = n_strings;
    m_start_width = width;
    m_start_milestone = m_milestone;
    
    m_localValuePartitionsTouched = new LocalValuePartition[32];
    m_n_localValuePartitionsTouched = 0;
    m_grammarsTouched = new IGrammar[32];
    m_n_grammarsTouched = 0;
  }

  public void reset() {
    if (m_useMap) {
      m_nameMap.clear();
    }
    for (int i = 0; i < m_n_localValuePartitionsTouched; i++) {
      m_localValuePartitionsTouched[i].reset();
    }
    m_n_localValuePartitionsTouched = 0;
    for (int i = 0; i < m_n_grammarsTouched; i++) {
      m_grammarsTouched[i].reset();
    }
    m_n_grammarsTouched = 0;
    
    n_strings = m_start_n_strings;
    width = m_start_width;
    m_milestone = m_start_milestone;
    
  	// Reset Global value partition
    globalValuePartition.reset();

    // Reset channels
    if (n_channels != 0) {
      final Iterator<Channel> iterChannels = channels.iterator();
      while (iterChannels.hasNext()) {
        iterChannels.next().blockNum = -1;
      }
    }
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // URIPartition methods
  ///////////////////////////////////////////////////////////////////////////

  public static final class LocalNameEntry {
    public String localName;
    int number;
    LocalValuePartition localValuePartition;
    public IGrammar[] objectGrammars;
    public IGrammar[] arrayGrammars;
    public Channel channel;
    /**
     * Constructs a LocalNameEntry.   
     * @param grammar Either an EXIGrammar or null
     */
    LocalNameEntry(String localName, int number) {
      this.localName = localName;
      this.number = number;
      this.localValuePartition = null;
      this.objectGrammars = new IGrammar[16]; 
      this.arrayGrammars = new IGrammar[16]; 
      channel = null;
    }
    @Override
    public final int hashCode() {
      return localName.hashCode();
    }
  }

  public void setObjectGrammar(int i, int distance, IGrammar grammar) {
    final LocalNameEntry localNameEntry = i == NAME_DOCUMENT ?  
        documentLocalNameEntry : localNameEntries[i];
    localNameEntry.objectGrammars[distance] = grammar;
  }

  public void setArrayGrammar(int i, int distance, IGrammar grammar) {
    final LocalNameEntry localNameEntry = i == NAME_DOCUMENT ?  
        documentLocalNameEntry : localNameEntries[i];
    localNameEntry.arrayGrammars[distance] = grammar;
  }
  
  public Channel getChannel(int i) {
    final LocalNameEntry localNameEntry = i == NAME_DOCUMENT ?  
        documentLocalNameEntry : localNameEntries[i];
    return localNameEntry.channel;
  }

  public void setChannel(int i, Channel channel) {
    final LocalNameEntry localNameEntry = i == NAME_DOCUMENT ?  
        documentLocalNameEntry : localNameEntries[i];
    assert localNameEntry.channel == null;
    localNameEntry.channel = channel;
    channels.add(channel);
    ++n_channels;
  }

  public int getCompactId(String name) {
    final LocalNameEntry item;
    if ((item = m_nameMap.get(name)) != null) {
      return item.number;
    }
    return -1;
  }

  public int internName(String name) {
    final int number;
    if ((number = getCompactId(name)) != -1) {
      return number;
    }
    return addName(name);
  }

  /**
   * Add a name which has *not* been in the local-name partition.
   * @return localName ID within the partition
   */
  public int addName(String name) {
    LocalNameEntry localNameEntry;
    final int number = n_strings;
    final int length;
    if ((length = localNameEntries.length) == n_strings) {
      localNameEntry = null; // need a new entry
      final int newLength =  2 * length;
      final LocalNameEntry[] stringList = new LocalNameEntry[newLength];
      System.arraycopy(localNameEntries, 0, stringList, 0, length);
      localNameEntries = stringList;
    }
    else if ((localNameEntry = localNameEntries[number]) != null) {
      localNameEntry.localName = name;
      localNameEntry.number = number;
      final LocalValuePartition localValuePartition;
      if ((localValuePartition = localNameEntry.localValuePartition) != null) {
        localValuePartition.reset();
      }
      IGrammar[] grammars;
      grammars = localNameEntry.objectGrammars;
      for (int i = 0; i < grammars.length; i++) {
        final IGrammar grammar = grammars[i];
        if (grammar != null)
          grammars[i].reset();
      }
      grammars = localNameEntry.arrayGrammars;
      for (int i = 0; i < grammars.length; i++) {
        final IGrammar grammar = grammars[i];
        if (grammar != null)
          grammars[i].reset();
      }
    }
    
    if (localNameEntry == null)
      localNameEntry = new LocalNameEntry(name, number);
    
    if (n_strings == m_milestone) {
      ++width;
      m_milestone <<= 1;
    }
    localNameEntries[n_strings++] = localNameEntry;
    if (m_useMap)
      m_nameMap.put(name, localNameEntry);
    return number;
  }
  
  public LocalValuePartition getLocalValuePartition(int i) {
    final LocalNameEntry item = i == NAME_DOCUMENT ? 
        documentLocalNameEntry : localNameEntries[i];
    final LocalValuePartition localValuePartition;
    if ((localValuePartition = item.localValuePartition) != null)
      return localValuePartition;
    else
      return item.localValuePartition = new LocalValuePartition(this);
  }
  
  public void addTouchedValuePartitions(LocalValuePartition localValuePartition) {
    if (m_n_localValuePartitionsTouched == m_localValuePartitionsTouched.length) {
      final int nlen = m_n_localValuePartitionsTouched << 1;
      final LocalValuePartition[] _localValuePartitions = new LocalValuePartition[nlen];
      System.arraycopy(m_localValuePartitionsTouched, 0, _localValuePartitions, 0, m_n_localValuePartitionsTouched);
      m_localValuePartitionsTouched = _localValuePartitions;
    }
    m_localValuePartitionsTouched[m_n_localValuePartitionsTouched++] = localValuePartition;
  }
  
  public void addTouchedBuiltinGrammars(IGrammar builtinGrammar) {
    if (m_n_grammarsTouched == m_grammarsTouched.length) {
      final int nlen = m_n_grammarsTouched << 1;
      final IGrammar[] _grammarsTouched = new IGrammar[nlen];
      System.arraycopy(m_grammarsTouched, 0, _grammarsTouched, 0, m_n_grammarsTouched);
      m_grammarsTouched = _grammarsTouched;
    }
    m_grammarsTouched[m_n_grammarsTouched++] = builtinGrammar;
  }
  
//  public final class LocalNamePartition {

//    public LocalNameEntry[] localNameEntries;
//    
//    public int n_strings = 0;
//    public int width = 0;
//    private int m_milestone = 1;
//
//    private final int m_start_n_strings;
//    private final int m_start_width;
//    private final int m_start_milestone;
//
//    private final HashMap<String,LocalNameEntry> m_nameMap;
//    
//    // Registry of touched (i.e. modified) LocalValuePartitions
//    private int m_n_localValuePartitionsTouched;
//    private LocalValuePartition[] m_localValuePartitionsTouched;
//
//    // Registry of touched (i.e. modified) BuiltinElementGrammars
//    private int m_n_grammarsTouched;
//    private IGrammar[] m_grammarsTouched;

//    LocalNamePartition() {
//      if (m_useMap) {
//        m_nameMap = new HashMap<String,LocalNameEntry>();
//      }
//      else {
//        m_nameMap = null;
//      }
//      localNameEntries = new LocalNameEntry[256];
//      m_start_n_strings = n_strings;
//      m_start_width = width;
//      m_start_milestone = m_milestone;
//      
//      m_localValuePartitionsTouched = new LocalValuePartition[32];
//      m_n_localValuePartitionsTouched = 0;
//      m_grammarsTouched = new IGrammar[32];
//      m_n_grammarsTouched = 0;
//    }

//    public void reset() {
//      if (m_useMap) {
//        m_nameMap.clear();
//      }
//      for (int i = 0; i < m_n_localValuePartitionsTouched; i++) {
//        m_localValuePartitionsTouched[i].reset();
//      }
//      m_n_localValuePartitionsTouched = 0;
//      for (int i = 0; i < m_n_grammarsTouched; i++) {
//        m_grammarsTouched[i].reset();
//      }
//      m_n_grammarsTouched = 0;
//      
//      n_strings = m_start_n_strings;
//      width = m_start_width;
//      m_milestone = m_start_milestone;
//    }
    
//    public void setGrammar(int i, IGrammar grammar) {
//      localNameEntries[i].grammar = grammar;
//    }
//
//    public void setChannel(int i, Channel channel) {
//      assert localNameEntries[i].channel == null;
//      localNameEntries[i].channel = channel;
//      channels.add(channel);
//      ++n_channels;
//    }
//
//    public int getCompactId(String name) {
//      final LocalNameEntry item;
//      if ((item = m_nameMap.get(name)) != null) {
//        return item.number;
//      }
//      return -1;
//    }
//
//    public int internName(String name) {
//      final int number;
//      if ((number = getCompactId(name)) != -1) {
//        return number;
//      }
//      return addName(name, (IGrammar)null);
//    }
//
//    /**
//     * Add a name which has *not* been in the local-name partition.
//     * @return localName ID within the partition
//     */
//    public int addName(String name, IGrammar grammar) {
//      LocalNameEntry localNameEntry;
//      final int number = n_strings;
//      final int length;
//      if ((length = localNameEntries.length) == n_strings) {
//        localNameEntry = null; // need a new entry
//        final int newLength =  2 * length;
//        final LocalNameEntry[] stringList = new LocalNameEntry[newLength];
//        System.arraycopy(localNameEntries, 0, stringList, 0, length);
//        localNameEntries = stringList;
//      }
//      else if ((localNameEntry = localNameEntries[number]) != null) {
//        localNameEntry.localName = name;
//        localNameEntry.number = number;
//        final LocalValuePartition localValuePartition;
//        if ((localValuePartition = localNameEntry.localValuePartition) != null) {
//          localValuePartition.reset();
//        }
//        assert grammar == null;
//        if ((grammar = localNameEntry.grammar) != null)
//          grammar.reset();
//      }
//      
//      if (localNameEntry == null)
//        localNameEntry = new LocalNameEntry(name, number, grammar);
//      
//      if (n_strings == m_milestone) {
//        ++width;
//        m_milestone <<= 1;
//      }
//      localNameEntries[n_strings++] = localNameEntry;
//      if (m_useMap)
//        m_nameMap.put(name, localNameEntry);
//      return number;
//    }
//    
//    public LocalValuePartition getLocalValuePartition(int i) {
//      final LocalNameEntry item = localNameEntries[i];
//      final LocalValuePartition localValuePartition;
//      if ((localValuePartition = item.localValuePartition) != null)
//        return localValuePartition;
//      else
//        return item.localValuePartition = new LocalValuePartition(this);
//    }
//    
//    public void addTouchedValuePartitions(LocalValuePartition localValuePartition) {
//      if (m_n_localValuePartitionsTouched == m_localValuePartitionsTouched.length) {
//        final int nlen = m_n_localValuePartitionsTouched << 1;
//        final LocalValuePartition[] _localValuePartitions = new LocalValuePartition[nlen];
//        System.arraycopy(m_localValuePartitionsTouched, 0, _localValuePartitions, 0, m_n_localValuePartitionsTouched);
//        m_localValuePartitionsTouched = _localValuePartitions;
//      }
//      m_localValuePartitionsTouched[m_n_localValuePartitionsTouched++] = localValuePartition;
//    }
//    
//    public void addTouchedBuiltinElementGrammars(IGrammar builtinElementGrammar) {
//      if (m_n_grammarsTouched == m_grammarsTouched.length) {
//        final int nlen = m_n_grammarsTouched << 1;
//        final IGrammar[] _grammarsTouched = new IGrammar[nlen];
//        System.arraycopy(m_grammarsTouched, 0, _grammarsTouched, 0, m_n_grammarsTouched);
//        m_grammarsTouched = _grammarsTouched;
//      }
//      m_grammarsTouched[m_n_grammarsTouched++] = builtinElementGrammar;
//    }

//  }
  
  public static final class PrefixEntry {
    public final String value;
    final int number;
    PrefixEntry(String value, int number) {
      this.value = value;
      this.number = number;
    }
    @Override
    public final int hashCode() {
      return value.hashCode();
    }
  }

//  private static final class URIEntry {
//    LocalNamePartition localNamePartition;
//    
//    URIEntry(LocalNamePartition localNamePartition) {
//      this.localNamePartition = localNamePartition;
//    }
//  }

  public static class NumberedCharacters {
    public final Characters value;
    public final int number;
    NumberedCharacters(final Characters value, final int number) {
      this.value = value;
      this.number = number;
    }
    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  public static final class LocalValuePartition {

    public NumberedCharacters[] valueEntries;

    public int n_strings = 0;
    public int width = 0;
    private int m_milestone = 1;
    
    private final StringTable m_stringTable;

//    LocalValuePartition(LocalNamePartition localNamePartition) {
    LocalValuePartition(StringTable stringTable) {
      valueEntries = new NumberedCharacters[1];
      m_stringTable = stringTable;
    }

    void reset() {
      n_strings = 0;
      width = 0;
      m_milestone = 1;
    }

    /**
     * Add a value which has *not* been in the local-value partition.
     */
    int addString(Characters value) {
      NumberedCharacters item;
      final int number = n_strings;
      item = new NumberedCharacters(value, number);
      final int length = valueEntries.length;
      if (n_strings == length) {
        int newLength =  2 * length;
        NumberedCharacters[] stringList = new NumberedCharacters[newLength];
        System.arraycopy(valueEntries, 0, stringList, 0, length);
        valueEntries = stringList;
      }
      if (n_strings == m_milestone) {
        ++width;
        m_milestone <<= 1;
      }
      valueEntries[n_strings] = item;
      if (n_strings++ == 0)
        m_stringTable.addTouchedValuePartitions(this);
      return number;
    }
  }

  public final class GlobalValuePartition {

    public GlobalEntry[] valueEntries;
    private final HashMap<Characters,GlobalEntry> m_stringMap;
    
    public int globalID = 0;
    public int width = 0;
    private int m_milestone = 1;

    GlobalValuePartition() {
      m_stringMap = m_useMap ? new HashMap<Characters,GlobalEntry>() : null;
      valueEntries = new GlobalEntry[1];
    }
    
    public void reset() {
      if (m_useMap)
        m_stringMap.clear();
      init();
    }
    
    private void init() {
      globalID = 0;
      width = 0;
      m_milestone = 1;
    }
    
    public GlobalEntry getEntry(final Characters characterSequence) {
      final GlobalEntry item;
      if ((item = m_stringMap.get(characterSequence)) != null) {
        return item;
      }
      return null;
    }
    
    public LocalValuePartition getLocalPartition(final int name) {
      return getLocalValuePartition(name);
    }

    public void addValue(final Characters characterSequence, final int name) {
      if (characterSequence.isVolatile)
    	characterSequence.turnPermanent();
      final LocalValuePartition localValuePartition; 
      localValuePartition = getLocalValuePartition(name);
      final int localNumber = localValuePartition.addString(characterSequence);

      GlobalEntry newItem = new GlobalEntry(characterSequence, globalID, localValuePartition, 
          localValuePartition.valueEntries[localNumber]);
      
      if (globalID == valueEntries.length) {
        final int newLength =  2 * globalID;
        GlobalEntry[] stringList = new GlobalEntry[newLength];
        System.arraycopy(valueEntries, 0, stringList, 0, globalID);
        valueEntries = stringList;
      }
      if (globalID == m_milestone) {
        ++width;
        m_milestone <<= 1;
      }
      final GlobalEntry item;
      if ((item = valueEntries[globalID]) != null) {
        if (m_useMap)
          m_stringMap.remove(item.value);
      }
      valueEntries[globalID] = newItem;
      if (m_useMap)
        m_stringMap.put(characterSequence, newItem);
      ++globalID;
    }
  }
  
  public static final class GlobalEntry {
    public final Characters value;
    public final int number;
    public final LocalValuePartition localPartition;
    public final NumberedCharacters localEntry;
    GlobalEntry(final Characters value, final int number, final LocalValuePartition localPartition, 
        final NumberedCharacters localEntry) {
      this.value = value;
      this.number = number;
      this.localPartition = localPartition;
      this.localEntry = localEntry;
    }
  }
  
}
