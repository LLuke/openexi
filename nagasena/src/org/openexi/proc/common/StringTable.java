package org.openexi.proc.common;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;

import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;

public final class StringTable {
  
  public final GlobalValuePartition globalValuePartition;
  
  private final LinkedList<Channel> channels;
  private int n_channels;

  public enum Usage {
    decoding,
    encoding
  };
  
  private final static String[] EMPTY = {}; 
  private final static String[] DEFAULT_PREFIXES = { "" }; 
  private final static String[] XML_PREFIXES = { "xml" }; 
  private final static String[] XSI_PREFIXES = { "xsi" }; 
  
  private final IGrammarCache m_grammarCache;
  private final boolean m_hasSchema;
  
  private int m_valuePartitionCapacity;
  
  private final boolean m_useMap;
  
  ///////////////////////////////////////////////////////////////////////////
  // URIPartition fields
  ///////////////////////////////////////////////////////////////////////////

  private URIEntry[] m_uriList;
  
  public int n_uris = 0;
  public int uriForwardedWidth = 0;
  
  private final int m_start_n_uris;
  private final int m_start_uriForwardedWidth;
  
  private final Map<String,URIEntry> m_uriMap;
  private final LocalNamePartition[] m_initialLocalNamePartitions;
  private final PrefixPartition[] m_initialPrefixPartitions;

  ///////////////////////////////////////////////////////////////////////////
  // Constructor
  ///////////////////////////////////////////////////////////////////////////

  public StringTable(IGrammarCache grammarCache, Usage usage) {
    m_grammarCache = grammarCache;
    final EXISchema schema = grammarCache != null ? grammarCache.getEXISchema() : null;
    m_useMap = usage == Usage.encoding;
    m_hasSchema = schema != null;
    globalValuePartition = new GlobalValuePartition();
    m_valuePartitionCapacity = EXIOptions.VALUE_PARTITION_CAPACITY_UNBOUNDED;
    channels = new LinkedList<Channel>();
    n_channels = 0;
    LocalNamePartition[] initialLocalNamePartitions;
    PrefixPartition[] initialPrefixPartitions;
    if (m_hasSchema) {
      final int[][] localNames = schema.getLocalNames();
      initialLocalNamePartitions = new LocalNamePartition[localNames.length];
      for (int i = 0; i < localNames.length; i++) {
        final LocalNamePartition localNamePartition = new LocalNamePartition(schema.uris[i], schema.localNames[i], schema);
        localNamePartition.uriId = i;
        initialLocalNamePartitions[i] = localNamePartition;
      }
    }
    else {
      initialLocalNamePartitions = new LocalNamePartition[3];
      LocalNamePartition localNamePartition;
      localNamePartition = new LocalNamePartition("", new String[] {}, (EXISchema)null); 
      localNamePartition.uriId = 0;
      initialLocalNamePartitions[0] = localNamePartition; 
      localNamePartition = new LocalNamePartition(XmlUriConst.W3C_XML_1998_URI, EXISchemaConst.XML_LOCALNAMES, (EXISchema)null);
      localNamePartition.uriId = XmlUriConst.W3C_XML_1998_URI_ID;
      initialLocalNamePartitions[1] = localNamePartition;
      localNamePartition = new LocalNamePartition(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, EXISchemaConst.XSI_LOCALNAMES, (EXISchema)null);
      localNamePartition.uriId = XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID;
      initialLocalNamePartitions[2] = localNamePartition;
    }
    initialPrefixPartitions = new PrefixPartition[3];
    initialPrefixPartitions[0] = new PrefixPartition(DEFAULT_PREFIXES);
    initialPrefixPartitions[1] = new PrefixPartition(XML_PREFIXES);
    initialPrefixPartitions[2] = new PrefixPartition(XSI_PREFIXES);
    
    // Initialize URIPartition
    m_uriMap = m_useMap ? new HashMap<String,URIEntry>() : null;
    assert initialPrefixPartitions.length == 3 && initialPrefixPartitions.length <= initialLocalNamePartitions.length;
    m_initialLocalNamePartitions = initialLocalNamePartitions;
    m_initialPrefixPartitions = initialPrefixPartitions;
    m_uriList = new URIEntry[32];
    for (int i = 0; i < m_initialLocalNamePartitions.length; i++) {
      final LocalNamePartition localNamePartition = m_initialLocalNamePartitions[i];
      final PrefixPartition prefixPartition = i < 3 ? initialPrefixPartitions[i] : null;
      addURI(localNamePartition.uri, localNamePartition, prefixPartition);
    }
    m_start_n_uris = n_uris;
    m_start_uriForwardedWidth = uriForwardedWidth;
  }

  public void reset() {
  	// Reset URIPartition
    final int n_initialLocalNamePartitions = m_initialLocalNamePartitions.length;
    final int n_initialPrefixPartitions = m_initialPrefixPartitions.length;
    if (m_useMap) {
      m_uriMap.clear();
      for (int i = 0; i < n_initialLocalNamePartitions; i++) {
        final LocalNamePartition localNamePartition = m_initialLocalNamePartitions[i]; 
        localNamePartition.reset();
        m_uriMap.put(localNamePartition.uri, m_uriList[i]);
      }
      for (int i = 0; i < n_initialPrefixPartitions; i++) {
        m_initialPrefixPartitions[i].reset();
      }
      for (int i = n_initialPrefixPartitions; i < n_initialLocalNamePartitions; i++) {
        final PrefixPartition prefixPartition;
        if ((prefixPartition  = m_uriList[i].prefixPartition) != null)
          prefixPartition.reset();
      }
    }
    else {
      for (int i = 0; i < n_initialLocalNamePartitions; i++)
        m_initialLocalNamePartitions[i].reset();
      for (int i = 0; i < n_initialPrefixPartitions; i++) {
        m_initialPrefixPartitions[i].reset();
      }
      for (int i = n_initialPrefixPartitions; i < n_initialLocalNamePartitions; i++) {
        final PrefixPartition prefixPartition;
        if ((prefixPartition  = m_uriList[i].prefixPartition) != null)
          prefixPartition.reset();
      }
    }
    n_uris = m_start_n_uris;
    uriForwardedWidth = m_start_uriForwardedWidth;
    
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
  
  public int getValuePartitionCapacity() {
    return m_valuePartitionCapacity;
  }
  
  public void setValuePartitionCapacity(int valuePartitionCapacity) {
    m_valuePartitionCapacity = valuePartitionCapacity;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // URIPartition methods
  ///////////////////////////////////////////////////////////////////////////

  public String getURI(final int i) {
    return m_uriList[i].value;
  }

  public int getCompactIdOfURI(String uri) {
    final URIEntry item;
    if ((item = m_uriMap.get(uri)) != null) {
      return item.number;
    }
    return -1;
  }

  public int internURI(String uri) {
    final int number;
    if ((number = getCompactIdOfURI(uri)) != -1) {
      return number;
    }
    return addURI(uri, (LocalNamePartition)null, (PrefixPartition)null);
  }

  /**
   * Add a name which has *not* been in the partition.
   */
  public int addURI(String uri, LocalNamePartition localNamePartition, PrefixPartition prefixPartition) {
    URIEntry uriEntry;
    final int number = n_uris;
    final int length;
    if ((length = m_uriList.length) == n_uris) {
      uriEntry = null; // need a new entry
      final int newLength =  2 * length;
      final URIEntry[] uriList = new URIEntry[newLength];
      System.arraycopy(m_uriList, 0, uriList, 0, length);
      m_uriList = uriList;
    }
    else if ((uriEntry = m_uriList[number]) != null) {
      assert localNamePartition == null;
      localNamePartition = uriEntry.localNamePartition;
      localNamePartition.reset();
      localNamePartition.uri = uri;
    }
    if (localNamePartition == null)
      localNamePartition = new LocalNamePartition(uri);
    localNamePartition.uriId = number;
    
    if (uriEntry == null)
      uriEntry = new URIEntry(uri, number, localNamePartition, prefixPartition);
    else {
      uriEntry.value = uri;
      uriEntry.number = number;
      uriEntry.localNamePartition = localNamePartition;
      uriEntry.prefixPartition = prefixPartition;
    }
      
    if (m_useMap)
      m_uriMap.put(uri, uriEntry);
    m_uriList[n_uris] = uriEntry;
    if ((n_uris++ & n_uris) == 0) // i.e. n_uris (after increment) is a power of 2
      ++uriForwardedWidth;
      
    return number;
  }

  public LocalNamePartition getLocalNamePartition(int i) {
    return m_uriList[i].localNamePartition;
  }

  public PrefixPartition getPrefixPartition(int i) {
    final URIEntry item = m_uriList[i];
    final PrefixPartition prefixPartition;
    if ((prefixPartition = item.prefixPartition) != null)
      return prefixPartition;
    else
      return item.prefixPartition = new PrefixPartition();
  }
  
  public static final class LocalNameEntry {
    public String localName;
    int number;
    LocalValuePartition localValuePartition;
    public IGrammar grammar;
    public Channel channel;
    /**
     * Constructs a LocalNameEntry.   
     * @param grammar Either an EXIGrammar or null
     */
    LocalNameEntry(String localName, int number, IGrammar grammar) {
      this.localName = localName;
      this.number = number;
      this.localValuePartition = null;
      this.grammar = grammar; 
      channel = null;
    }
    @Override
    public final int hashCode() {
      return localName.hashCode();
    }
  }

  public final class LocalNamePartition {

    public LocalNameEntry[] localNameEntries;
    
    public int n_strings = 0;
    public int width = 0;
    private int m_milestone = 1;

    private final int m_start_n_strings;
    private final int m_start_width;
    private final int m_start_milestone;

    private final HashMap<String,LocalNameEntry> m_nameMap;
    
    String uri;
    int uriId;
    
    private final String[] m_initialNames;

    // Registry of touched (i.e. modified) LocalValuePartitions
    private int m_n_localValuePartitionsTouched;
    private LocalValuePartition[] m_localValuePartitionsTouched;

    // Registry of touched (i.e. modified) BuiltinElementGrammars
    private int m_n_grammarsTouched;
    private IGrammar[] m_grammarsTouched;

    LocalNamePartition(String uri) {
      this(uri, EMPTY, (EXISchema)null);
    }

    LocalNamePartition(String uri, String[] names, EXISchema schema) {
      this.uri = uri;
      uriId = -1;
      if (m_useMap) {
        m_nameMap = new HashMap<String,LocalNameEntry>();
        m_initialNames = names;
      }
      else {
        m_nameMap = null;
        m_initialNames = null;
      }
      localNameEntries = new LocalNameEntry[256];
      int i;
      final int len;
      for (i = 0, len = names.length; i < len; i++) {
        final String name = names[i];
        final IGrammar grammar;
        if (schema != null) {
          final int elem;
          if ((elem = schema.getGlobalElemOfSchema(uri, name)) != EXISchema.NIL_NODE)
            grammar = m_grammarCache.getElementGrammarUse(elem);
          else
            grammar = null;
        }
        else
          grammar = null;
        addName(name, grammar);
      }
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
        for (int i = 0; i < m_initialNames.length; i++) {
          m_nameMap.put(m_initialNames[i], localNameEntries[i]);
        }
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
    }
    
    public void setGrammar(int i, IGrammar grammar) {
      localNameEntries[i].grammar = grammar;
    }

    public void setChannel(int i, Channel channel) {
      assert localNameEntries[i].channel == null;
      localNameEntries[i].channel = channel;
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
      return addName(name, (IGrammar)null);
    }

    /**
     * Add a name which has *not* been in the local-name partition.
     * @return localName ID within the partition
     */
    public int addName(String name, IGrammar grammar) {
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
        assert grammar == null;
        if ((grammar = localNameEntry.grammar) != null)
          grammar.reset();
      }
      
      if (localNameEntry == null)
        localNameEntry = new LocalNameEntry(name, number, grammar);
      
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
      final LocalNameEntry item = localNameEntries[i];
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
    
    public void addTouchedBuiltinElementGrammars(IGrammar builtinElementGrammar) {
      if (m_n_grammarsTouched == m_grammarsTouched.length) {
        final int nlen = m_n_grammarsTouched << 1;
        final IGrammar[] _grammarsTouched = new IGrammar[nlen];
        System.arraycopy(m_grammarsTouched, 0, _grammarsTouched, 0, m_n_grammarsTouched);
        m_grammarsTouched = _grammarsTouched;
      }
      m_grammarsTouched[m_n_grammarsTouched++] = builtinElementGrammar;
    }

  }
  
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

  public final class PrefixPartition {

    public PrefixEntry[] prefixEntries;
    
    public int n_strings = 0;
    public int width = 0;
    public int forwardedWidth = 0;
    private int m_milestone = 1;

    private final int m_start_n_strings;
    private final int m_start_width;
    private final int m_start_forwardedWidth;
    private final int m_start_milestone;

    private final HashMap<String,PrefixEntry> m_prefixMap;

    private final String[] m_prefixes;

    private PrefixPartition() {
      this(EMPTY);
    }
    
    private PrefixPartition(final String[] prefixes) {
      m_prefixMap = m_useMap ? new HashMap<String,PrefixEntry>() : null;
      m_prefixes = prefixes;
      
      prefixEntries = new PrefixEntry[1];

      int i;
      final int len;
      for (i = 0, len = m_prefixes.length; i < len; i++) {
        addPrefix(m_prefixes[i]);
      }
      m_start_n_strings = n_strings;
      m_start_width = width;
      m_start_forwardedWidth = forwardedWidth;
      m_start_milestone = m_milestone;
    }

    public void reset() {
      if (m_useMap) {
        m_prefixMap.clear();
        int i;
        final int len;
        for (i = 0, len = m_prefixes.length; i < len; i++) {
          m_prefixMap.put(m_prefixes[i], prefixEntries[i]);
        }
      }
      n_strings = m_start_n_strings;
      width = m_start_width;
      forwardedWidth = m_start_forwardedWidth;
      m_milestone = m_start_milestone;
    }
    
    public int getCompactId(final String prefix) {
      PrefixEntry item;
      if ((item = m_prefixMap.get(prefix)) != null) {
        return item.number;
      }
      return -1;
    }

    public int internPrefix(final String prefix) {
      int number;
      if ((number = getCompactId(prefix)) != -1) {
        return number;
      }
      return addPrefix(prefix);
    }

    /**
     * Add a name which has *not* been in the partition.
     */
    public int addPrefix(final String prefix) {
      final PrefixEntry item;
      final int number = n_strings;
      item = new PrefixEntry(prefix, number);
      int length = prefixEntries.length;
      if (n_strings == length) {
        int newLength =  2 * length;
        PrefixEntry[] uriList = new PrefixEntry[newLength];
        System.arraycopy(prefixEntries, 0, uriList, 0, length);
        prefixEntries = uriList;
        length = newLength;
      }
      if (m_useMap)
        m_prefixMap.put(prefix, item);
      prefixEntries[n_strings] = item;
      if (n_strings++ == m_milestone) {
        ++width;
        m_milestone <<= 1;
      }
      if (n_strings == m_milestone)
        ++forwardedWidth;
      return number;
    }
  }

  private static final class URIEntry {
    String value;
    int number;
    LocalNamePartition localNamePartition;
    PrefixPartition prefixPartition;
    
    URIEntry(String value, int number, LocalNamePartition localNamePartition, PrefixPartition prefixPartition) {
      this.value = value;
      this.number = number;
      this.localNamePartition = localNamePartition;
      this.prefixPartition = prefixPartition;
    }
    
    @Override
    public final int hashCode() {
      return value.hashCode();
    }
  }

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
    
    private final LocalNamePartition m_localNamePartition;

    LocalValuePartition(LocalNamePartition localNamePartition) {
      valueEntries = new NumberedCharacters[1];
      m_localNamePartition = localNamePartition;
    }

    void reset() {
      n_strings = 0;
      width = 0;
      m_milestone = 1;
    }

    void releaseEntry(int i) {
      valueEntries[i] = null;
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
        m_localNamePartition.addTouchedValuePartitions(this);
      return number;
    }
  }

  public final class GlobalValuePartition {

    public GlobalEntry[] valueEntries;
    private final HashMap<Characters,GlobalEntry> m_stringMap;
    
    public int globalID = 0;
    public int width = 0;
    private int m_milestone = 1;
    private boolean m_wrapped = false;

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
      m_wrapped = false;
    }
    
    public GlobalEntry getEntry(final Characters characterSequence) {
      final GlobalEntry item;
      if ((item = m_stringMap.get(characterSequence)) != null) {
        return item;
      }
      return null;
    }
    
    public LocalValuePartition getLocalPartition(final int name, final int uri) {
      final LocalNamePartition localNamePartition = getLocalNamePartition(uri);
      return localNamePartition.getLocalValuePartition(name);
    }

    public void addValue(final Characters characterSequence, final int name, final int uri) {
      if (characterSequence.isVolatile)
    	characterSequence.turnPermanent();
      if (m_valuePartitionCapacity != 0) {
        final LocalValuePartition localValuePartition; 
        localValuePartition = getLocalNamePartition(uri).getLocalValuePartition(name);
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
          if (m_wrapped)
            item.localPartition.releaseEntry(item.localEntry.number);
          if (m_useMap)
            m_stringMap.remove(item.value);
        }
        valueEntries[globalID] = newItem;
        if (m_useMap)
          m_stringMap.put(characterSequence, newItem);
        if (++globalID == m_valuePartitionCapacity) {
          globalID = 0;
          m_wrapped = true;
        }
      }
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
