package org.openexi.proc.io;

import java.util.HashMap;

import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.StringTableConst;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.schema.EXISchema;

public final class StringTable {
  
  private final URIPartition m_uriPartition;
  private final LocalNamePartition[] m_initialLocalNamePartitions;
  
  private final HashMap<String,LocalNamePartition> m_localNamePartitions;
  private final HashMap<String,PrefixPartition> m_prefixPartitions;
  private final GlobalPartition m_globalPartition;

  private final static String[] EMPTY = {}; 
  private final static String[] DEFAULT_PREFIXES = { "" }; 
  private final static String[] XML_PREFIXES = { "xml" }; 
  private final static String[] XSI_PREFIXES = { "xsi" }; 
  
  private final PrefixPartition m_defaultNamespacePrefixes;
  private final PrefixPartition m_xmlNamespacePrefixes;
  private final PrefixPartition m_xsiNamespacePrefixes;

  private final boolean m_hasSchema;
  
  private int m_valuePartitionCapacity;
  
  public StringTable(EXISchema schemaCorpus) {
    m_hasSchema = schemaCorpus != null;
    m_localNamePartitions = new HashMap<String,LocalNamePartition>();
    m_prefixPartitions = new HashMap<String,PrefixPartition>();
    m_defaultNamespacePrefixes = new PrefixPartition(DEFAULT_PREFIXES);
    m_xmlNamespacePrefixes = new PrefixPartition(XML_PREFIXES);
    m_xsiNamespacePrefixes = new PrefixPartition(XSI_PREFIXES);
    m_globalPartition = new GlobalPartition();
    m_valuePartitionCapacity = EXIOptions.VALUE_PARTITION_CAPACITY_UNBOUNDED;
    if (m_hasSchema) {
      final String[] uris = schemaCorpus.getUris();
      m_uriPartition = new URIPartition(uris);
      final int[][] orderedLocalNames = schemaCorpus.getLocalNames();
      final String[] names = schemaCorpus.getNames();
      m_initialLocalNamePartitions = new LocalNamePartition[orderedLocalNames.length];
      for (int i = 0; i < orderedLocalNames.length; i++) {
        final String[] localNames = new String[orderedLocalNames[i].length];
        for (int j = 0; j < localNames.length; j++) {
          localNames[j] = names[orderedLocalNames[i][j]];
        }
        m_initialLocalNamePartitions[i] = new LocalNamePartition(uris[i], localNames);
      }
    }
    else {
      m_uriPartition = new URIPartition();
      m_initialLocalNamePartitions = new LocalNamePartition[2];
      m_initialLocalNamePartitions[0] = new LocalNamePartition(XmlUriConst.W3C_XML_1998_URI, StringTableConst.XML_LOCALNAMES);
      m_initialLocalNamePartitions[1] = new LocalNamePartition(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, StringTableConst.XSI_LOCALNAMES);
    }
    init();
  }
  
  int getValuePartitionCapacity() {
    return m_valuePartitionCapacity;
  }
  
  public void setValuePartitionCapacity(int valuePartitionCapacity) {
    m_valuePartitionCapacity = valuePartitionCapacity;
  }

  public void clear() {
    m_uriPartition.clear();
    m_localNamePartitions.clear();
    final int len = m_initialLocalNamePartitions.length;
    for (int i = 0; i < len; i++) {
      LocalNamePartition  partition = m_initialLocalNamePartitions[i];
      partition.clear();
    }    
    m_prefixPartitions.clear();
    m_defaultNamespacePrefixes.clear();
    m_xmlNamespacePrefixes.clear();
    m_xsiNamespacePrefixes.clear();
    m_globalPartition.clear();
    init();
  }
  
  private void init() {
    final int len = m_initialLocalNamePartitions.length;
    for (int i = 0; i < len; i++) {
      LocalNamePartition  partition = m_initialLocalNamePartitions[i];
      m_localNamePartitions.put(partition.uri, partition);
    }
    m_prefixPartitions.put("", m_defaultNamespacePrefixes);
    m_prefixPartitions.put(XmlUriConst.W3C_XML_1998_URI, m_xmlNamespacePrefixes);
    m_prefixPartitions.put(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, m_xsiNamespacePrefixes);
  }
  
  public URIPartition getURIPartition() {
    return m_uriPartition;
  }
  
  public LocalNamePartition getLocalNamePartition(String uri) {
    LocalNamePartition partition;
    if ((partition = m_localNamePartitions.get(uri)) != null)
      return partition;
    else {
      partition = new LocalNamePartition(uri);
      m_localNamePartitions.put(uri, partition);
      return partition;
    }
  }
  
  public PrefixPartition getPrefixPartition(final String uri) {
    PrefixPartition partition;
    if ((partition = m_prefixPartitions.get(uri)) != null)
      return partition;
    else {
      partition = new PrefixPartition();
      m_prefixPartitions.put(uri, partition);
      return partition;
    }
  }

  public GlobalPartition getGlobalPartition() {
    return m_globalPartition;
  }

  static abstract class SkeletalPartition {
    
    protected NumberedString[] m_stringList;
    
    public int n_strings;
    public int width;

    protected SkeletalPartition() {
    }
    
    public void clear() {
      init();
    }
    
    protected void init() {
      m_stringList = new NumberedString[1];
      n_strings = 0;
      width = 0;
    }

    public String getString(final int i) {
      return m_stringList[i].value;
    }
    
    public abstract int addString(String str);
  }

  static abstract class Partition extends SkeletalPartition {
    
    protected final HashMap<String,NumberedString> m_stringMap;
    
    protected Partition() {
      m_stringMap = new HashMap<String,NumberedString>();
    }
    
    @Override
    public void clear() {
      m_stringMap.clear();
      super.clear();
    }
    
    public int getCompactId(final String str) {
      NumberedString item;
      if ((item = m_stringMap.get(str)) != null) {
        return item.number;
      }
      return -1;
    }

    public int internString(final String str) {
      int number;
      if ((number = getCompactId(str)) != -1) {
        return number;
      }
      return addString(str);
    }
  }
  
  public static final class LocalNamePartition extends Partition {
    
    final String uri;
    private String[] m_initialNames;
    
    private LocalNamePartition(String uri) {
      this(uri, EMPTY);
    }
    
    private LocalNamePartition(String uri, String[] names) {
      this.uri = uri;
      m_initialNames = names;
      init();
    }
    
    @Override
    protected void init() {
      super.init();

      int i;
      final int len;
      for (i = 0, len = m_initialNames.length; i < len; i++) {
        addString(m_initialNames[i]);
      }
    }
    
    /**
     * Add a name which has *not* been in the local-name partition.
     */
    @Override
    public int addString(final String name) {
      final NumberedString item;
      final int number = n_strings;
      item = new NumberedString(name, number);
      final int length = m_stringList.length;
      if (n_strings == length) {
        int newLength =  2 * length;
        NumberedString[] stringList = new NumberedString[newLength];
        System.arraycopy(m_stringList, 0, stringList, 0, length);
        m_stringList = stringList;
        ++width;
      }
      m_stringList[n_strings++] = item;
      m_stringMap.put(name, item);
      return number;
    }
  }
  
  static private abstract class PrefixOrURIPartition extends Partition {
    
    public int forwardedWidth;
    
    @Override
    protected void init() {
      super.init();
      forwardedWidth = 0;
    }

    /**
     * Add a name which has *not* been in the partition.
     */
    @Override
    public int addString(final String name) {
      final NumberedString item;
      final int number = n_strings;
      item = new NumberedString(name, number);
      int length = m_stringList.length;
      if (n_strings == length) {
        int newLength =  2 * length;
        NumberedString[] uriList = new NumberedString[newLength];
        System.arraycopy(m_stringList, 0, uriList, 0, length);
        m_stringList = uriList;
        ++width;
        length = newLength;
      }
      m_stringList[n_strings++] = item;
      m_stringMap.put(name, item);
      if (n_strings == length)
        ++forwardedWidth;
      return number;
    }
  }
  
  public static final class PrefixPartition extends PrefixOrURIPartition {
    
    private String[] m_prefixes;
    
    private PrefixPartition() {
      this(EMPTY);
    }
    
    private PrefixPartition(final String[] prefixes) {
      m_prefixes = prefixes;
      init();
    }
    
    @Override
    protected void init() {
      super.init();

      int i;
      final int len;
      for (i = 0, len = m_prefixes.length; i < len; i++) {
        addString(m_prefixes[i]);
      }
    }
  }

  public static final class URIPartition extends PrefixOrURIPartition {
    
    private final boolean m_isSchemaInformed;
    private final String[] m_schemaUris;
    
    private URIPartition(String[] uris) {
      assert uris != null;
      m_isSchemaInformed = true;
      m_schemaUris = uris;
      init();
    }

    private URIPartition() {
      m_isSchemaInformed = false;
      m_schemaUris = (String[])null;
      init();
    }

    @Override
    protected void init() {
      super.init();
      if (m_isSchemaInformed) {
        final int len = m_schemaUris.length;
        for (int i = 0; i < len; i++)
          addString(m_schemaUris[i]);
      }
      else {
        addString("");
        addString("http://www.w3.org/XML/1998/namespace");
        addString("http://www.w3.org/2001/XMLSchema-instance");
      }
    }
  }
  
  static class NumberedString {
    final String value;
    public final int number;
    NumberedString(final String value, final int number) {
      this.value = value;
      this.number = number;
    }
    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  static class NumberedCharacters {
    final CharacterSequence value;
    public final int number;
    NumberedCharacters(final CharacterSequence value, final int number) {
      this.value = value;
      this.number = number;
    }
    @Override
    public int hashCode() {
      return value.hashCode();
    }
  }

  public static final class LocalPartition {

    protected NumberedCharacters[] m_stringList;

    public int n_strings;
    public int width;

    final String name;
    final String uri;
    
    public void clear() {
      init();
    }
    
    void init() {
      m_stringList = new NumberedCharacters[1];
      n_strings = 0;
      width = 0;
    }

    public NumberedCharacters getString(final int i) {
      return m_stringList[i];
    }
    
    LocalPartition(final String name, final String uri) {
      this.name = name;
      this.uri = uri;
      init();
    }

    NumberedCharacters getEntry(final int i) {
      return m_stringList[i];
    }
    
    void releaseEntry(int i) {
      m_stringList[i] = null;
    }
    
    /**
     * Add a value which has *not* been in the local-value partition.
     */
    int addString(CharacterSequence value) {
      NumberedCharacters item;
      final int number = n_strings;
      item = new NumberedCharacters(value, number);
      final int length = m_stringList.length;
      if (n_strings == length) {
        int newLength =  2 * length;
        NumberedCharacters[] stringList = new NumberedCharacters[newLength];
        System.arraycopy(m_stringList, 0, stringList, 0, length);
        m_stringList = stringList;
        ++width;
      }
      m_stringList[n_strings++] = item;
      return number;
    }
  }

  public final class GlobalPartition {

    private GlobalEntry[] m_stringList;
    private final HashMap<CharacterSequence,GlobalEntry> m_stringMap;
    // Local-name to LocalValuePartition[]
    private final HashMap<String,LocalPartition[]> m_partitionMap;
    
    int globalID;
    public int width;

    GlobalPartition() {
      m_stringMap = new HashMap<CharacterSequence,GlobalEntry>();
      m_partitionMap = new HashMap<String,LocalPartition[]>();
      init();
    }
    
    public void clear() {
      m_stringMap.clear();
      m_partitionMap.clear();
      init();
    }
    
    private void init() {
      m_stringList = new GlobalEntry[1];
      globalID = 0;
      width = 0;
    }
    
    public CharacterSequence getString(final int i) {
      return m_stringList[i].value;
    }

    public GlobalEntry getEntry(final CharacterSequence characterSequence) {
      final GlobalEntry item;
      if ((item = m_stringMap.get(characterSequence)) != null) {
        return item;
      }
      return null;
    }
    
    public LocalPartition getLocalPartition(final String name, final String uri) {
      final LocalPartition[] candidates;
      if ((candidates = m_partitionMap.get(name)) != null) {
        int i;
        final int len;
        for (i = 0, len = candidates.length; i < len; i++) {
          final LocalPartition localValuePartition = candidates[i];
          assert name.equals(localValuePartition.name);
          if (uri.equals(localValuePartition.uri))
            return localValuePartition;
        }
      }
      return null;
    }

    public void addString(final CharacterSequence characterSequence, final String name, final String uri) {
      if (m_valuePartitionCapacity != 0) {
        LocalPartition localValuePartition = null;
        LocalPartition[] candidates;
        if ((candidates = m_partitionMap.get(name)) != null) {
          int i;
          final int len;
          for (i = 0, len = candidates.length; i < len; i++) {
            localValuePartition = candidates[i];
            assert name.equals(localValuePartition.name);
            if (uri.equals(localValuePartition.uri))
              break;
          }
          if (i == len) {
            LocalPartition[] _candidates;
            _candidates = new LocalPartition[candidates.length + 1];
            System.arraycopy(candidates, 0, _candidates, 0, candidates.length);
            localValuePartition = new LocalPartition(name, uri);
            _candidates[candidates.length] = localValuePartition;
            candidates = _candidates;
            m_partitionMap.put(name, candidates);
          }
        }
        else {
          localValuePartition = new LocalPartition(name, uri);
          candidates = new LocalPartition[1];
          candidates[0] = localValuePartition;
          m_partitionMap.put(name, candidates);
        }
  
        final int localNumber = localValuePartition.addString(characterSequence);
  
        GlobalEntry newItem = new GlobalEntry(characterSequence, globalID, localValuePartition, 
            localValuePartition.getEntry(localNumber));
        
        if (globalID == m_stringList.length) {
          final int newLength =  2 * globalID;
          GlobalEntry[] stringList = new GlobalEntry[newLength];
          System.arraycopy(m_stringList, 0, stringList, 0, globalID);
          m_stringList = stringList;
          ++width;
        }
        final GlobalEntry item;
        if ((item = m_stringList[globalID]) != null) {
          item.localPartition.releaseEntry(item.localEntry.number);
          m_stringMap.remove(item.value);
        }
        m_stringList[globalID] = newItem;
        m_stringMap.put(characterSequence, newItem);
        if (++globalID == m_valuePartitionCapacity)
          globalID = 0;
      }
    }
  }
  
  public static final class GlobalEntry {
    final CharacterSequence value;
    public final int number;
    public final LocalPartition localPartition;
    public final NumberedCharacters localEntry;
    GlobalEntry(final CharacterSequence value, final int number, final LocalPartition localPartition, 
        final NumberedCharacters localEntry) {
      this.value = value;
      this.number = number;
      this.localPartition = localPartition;
      this.localEntry = localEntry;
    }
  }
  
}
