using System;
using System.Diagnostics;
using System.Collections.Generic;

using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;

namespace Nagasena.Proc.Common {

  public sealed class StringTable {

    public readonly GlobalValuePartition globalValuePartition;

    private readonly LinkedList<Channel> channels;
    private int n_channels;

    public enum Usage {
      decoding,
      encoding
    }

    private static readonly string[] EMPTY = new string[] {};
    private static readonly string[] DEFAULT_PREFIXES = new string[] { "" };
    private static readonly string[] XML_PREFIXES = new string[] { "xml" };
    private static readonly string[] XSI_PREFIXES = new string[] { "xsi" };

    private readonly IGrammarCache m_grammarCache;
    private readonly bool m_hasSchema;

    private int m_valuePartitionCapacity;

    private readonly bool m_useMap;

    ///////////////////////////////////////////////////////////////////////////
    // URIPartition fields
    ///////////////////////////////////////////////////////////////////////////

    private URIEntry[] m_uriList;

    public int n_uris = 0;
    public int uriWidth = 0;
    public int uriForwardedWidth = 0;
    private int m_uriMilestone = 1;

    private readonly int m_start_n_uris;
    private readonly int m_start_uriWidth;
    private readonly int m_start_uriForwardedWidth;
    private readonly int m_start_uriMilestone;

    private readonly IDictionary<string, URIEntry> m_uriMap;
    private readonly LocalNamePartition[] m_initialLocalNamePartitions;
    private readonly PrefixPartition[] m_initialPrefixPartitions;

    ///////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////

    public StringTable(IGrammarCache grammarCache, Usage usage) {
      m_grammarCache = grammarCache;
      EXISchema schema = grammarCache != null ? grammarCache.EXISchema : null;
      m_useMap = usage == Usage.encoding;
      m_hasSchema = schema != null;
      globalValuePartition = new GlobalValuePartition(this);
      m_valuePartitionCapacity = EXIOptions.VALUE_PARTITION_CAPACITY_UNBOUNDED;
      channels = new LinkedList<Channel>();
      n_channels = 0;
      LocalNamePartition[] initialLocalNamePartitions;
      PrefixPartition[] initialPrefixPartitions;
      if (m_hasSchema) {
        int[][] localNames = schema.LocalNames;
        initialLocalNamePartitions = new LocalNamePartition[localNames.Length];
        for (int i = 0; i < localNames.Length; i++) {
          LocalNamePartition localNamePartition = new LocalNamePartition(this, schema.uris[i], schema.localNames[i], schema);
          localNamePartition.uriId = i;
          initialLocalNamePartitions[i] = localNamePartition;
        }
      }
      else {
        initialLocalNamePartitions = new LocalNamePartition[3];
        LocalNamePartition localNamePartition;
        localNamePartition = new LocalNamePartition(this, "", new string[] {}, (EXISchema)null);
        localNamePartition.uriId = 0;
        initialLocalNamePartitions[0] = localNamePartition;
        localNamePartition = new LocalNamePartition(this, XmlUriConst.W3C_XML_1998_URI, EXISchemaConst.XML_LOCALNAMES, (EXISchema)null);
        localNamePartition.uriId = XmlUriConst.W3C_XML_1998_URI_ID;
        initialLocalNamePartitions[1] = localNamePartition;
        localNamePartition = new LocalNamePartition(this, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, EXISchemaConst.XSI_LOCALNAMES, (EXISchema)null);
        localNamePartition.uriId = XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID;
        initialLocalNamePartitions[2] = localNamePartition;
      }
      initialPrefixPartitions = new PrefixPartition[3];
      initialPrefixPartitions[0] = new PrefixPartition(this, DEFAULT_PREFIXES);
      initialPrefixPartitions[1] = new PrefixPartition(this, XML_PREFIXES);
      initialPrefixPartitions[2] = new PrefixPartition(this, XSI_PREFIXES);

      // Initialize URIPartition
      m_uriMap = m_useMap ? new Dictionary<string, URIEntry>() : null;
      Debug.Assert(initialPrefixPartitions.Length == 3 && initialPrefixPartitions.Length <= initialLocalNamePartitions.Length);
      m_initialLocalNamePartitions = initialLocalNamePartitions;
      m_initialPrefixPartitions = initialPrefixPartitions;
      m_uriList = new URIEntry[32];
      for (int i = 0; i < m_initialLocalNamePartitions.Length; i++) {
        LocalNamePartition localNamePartition = m_initialLocalNamePartitions[i];
        PrefixPartition prefixPartition = i < 3 ? initialPrefixPartitions[i] : null;
        addURI(localNamePartition.uri, localNamePartition, prefixPartition);
      }
      m_start_n_uris = n_uris;
      m_start_uriWidth = uriWidth;
      m_start_uriForwardedWidth = uriForwardedWidth;
      m_start_uriMilestone = m_uriMilestone;
    }

    public void reset() {
      // Reset URIPartition
      int n_initialLocalNamePartitions = m_initialLocalNamePartitions.Length;
      int n_initialPrefixPartitions = m_initialPrefixPartitions.Length;
      if (m_useMap) {
        m_uriMap.Clear();
        for (int i = 0; i < n_initialLocalNamePartitions; i++) {
          LocalNamePartition localNamePartition = m_initialLocalNamePartitions[i];
          localNamePartition.reset();
          m_uriMap[localNamePartition.uri] = m_uriList[i];
        }
        for (int i = 0; i < n_initialPrefixPartitions; i++) {
          m_initialPrefixPartitions[i].reset();
        }
        for (int i = n_initialPrefixPartitions; i < n_initialLocalNamePartitions; i++) {
          PrefixPartition prefixPartition;
          if ((prefixPartition = m_uriList[i].prefixPartition) != null) {
            prefixPartition.reset();
          }
        }
      }
      else {
        for (int i = 0; i < n_initialLocalNamePartitions; i++) {
          m_initialLocalNamePartitions[i].reset();
        }
        for (int i = 0; i < n_initialPrefixPartitions; i++) {
          m_initialPrefixPartitions[i].reset();
        }
        for (int i = n_initialPrefixPartitions; i < n_initialLocalNamePartitions; i++) {
          PrefixPartition prefixPartition;
          if ((prefixPartition = m_uriList[i].prefixPartition) != null) {
            prefixPartition.reset();
          }
        }
      }
      n_uris = m_start_n_uris;
      uriWidth = m_start_uriWidth;
      uriForwardedWidth = m_start_uriForwardedWidth;
      m_uriMilestone = m_start_uriMilestone;

      // Reset Global value partition
      globalValuePartition.reset();

      // Reset channels
      if (n_channels != 0) {
        IEnumerator<Channel> iterChannels = channels.GetEnumerator();
        while (iterChannels.MoveNext()) {
          iterChannels.Current.blockNum = -1;
        }
      }
    }

    public int ValuePartitionCapacity {
      get {
        return m_valuePartitionCapacity;
      }
      set {
        m_valuePartitionCapacity = value;
      }
    }


    ///////////////////////////////////////////////////////////////////////////
    // URIPartition methods
    ///////////////////////////////////////////////////////////////////////////

    public string getURI(int i) {
      return m_uriList[i].value;
    }

    public int getCompactIdOfURI(string uri) {
      URIEntry item;
      if (m_uriMap.TryGetValue(uri, out item)) {
        return item.number;
      }
      return -1;
    }

    public int internURI(string uri) {
      int number;
      if ((number = getCompactIdOfURI(uri)) != -1) {
        return number;
      }
      return addURI(uri, (LocalNamePartition)null, (PrefixPartition)null);
    }

    /// <summary>
    /// Add a name which has *not* been in the partition.
    /// </summary>
    public int addURI(string uri, LocalNamePartition localNamePartition, PrefixPartition prefixPartition) {
      URIEntry uriEntry;
      int number = n_uris;
      int length;
      if ((length = m_uriList.Length) == n_uris) {
        uriEntry = null; // need a new entry
        int newLength = 2 * length;
        URIEntry[] uriList = new URIEntry[newLength];
        Array.Copy(m_uriList, 0, uriList, 0, length);
        m_uriList = uriList;
      }
      else if ((uriEntry = m_uriList[number]) != null) {
        Debug.Assert(localNamePartition == null);
        localNamePartition = uriEntry.localNamePartition;
        localNamePartition.reset();
        localNamePartition.uri = uri;
      }
      if (localNamePartition == null) {
        localNamePartition = new LocalNamePartition(this, uri);
      }
      localNamePartition.uriId = number;

      if (uriEntry == null) {
        uriEntry = new URIEntry(uri, number, localNamePartition, prefixPartition);
      }
      else {
        uriEntry.value = uri;
        uriEntry.number = number;
        uriEntry.localNamePartition = localNamePartition;
        uriEntry.prefixPartition = prefixPartition;
      }

      if (m_useMap) {
        m_uriMap[uri] = uriEntry;
      }
      m_uriList[n_uris] = uriEntry;
      if (n_uris++ == m_uriMilestone) {
        ++uriWidth;
        m_uriMilestone <<= 1;
      }
      if (n_uris == m_uriMilestone) {
        ++uriForwardedWidth;
      }
      return number;
    }

    public LocalNamePartition getLocalNamePartition(int i) {
      return m_uriList[i].localNamePartition;
    }

    public PrefixPartition getPrefixPartition(int i) {
      URIEntry item = m_uriList[i];
      PrefixPartition prefixPartition;
      if ((prefixPartition = item.prefixPartition) != null) {
        return prefixPartition;
      }
      else {
        return item.prefixPartition = new PrefixPartition(this);
      }
    }

    public sealed class LocalNameEntry {
      public string localName;
      internal int number;
      internal LocalValuePartition localValuePartition;
      public IGrammar grammar;
      internal bool isGrammarBuiltin;
      public Channel channel;
      /// <summary>
      /// Constructs a LocalNameEntry. </summary>
      /// <param name="grammar"> Either an EXIGrammar or null </param>
      internal LocalNameEntry(string localName, int number, IGrammar grammar) {
        this.localName = localName;
        this.number = number;
        this.localValuePartition = null;
        isGrammarBuiltin = (this.grammar = grammar) == null;
        channel = null;
      }
      public override int GetHashCode() {
        return localName.GetHashCode();
      }
    }

    public sealed class LocalNamePartition {
      private readonly StringTable outerInstance;


      public LocalNameEntry[] localNameEntries;

      public int n_strings = 0;
      public int width = 0;
      internal int m_milestone = 1;

      internal readonly int m_start_n_strings;
      internal readonly int m_start_width;
      internal readonly int m_start_milestone;

      internal readonly Dictionary<string, LocalNameEntry> m_nameMap;

      internal string uri;
      internal int uriId;

      internal readonly string[] m_initialNames;

      // Registry of touched (i.e. modified) LocalValuePartitions
      internal int m_n_localValuePartitionsTouched;
      internal LocalValuePartition[] m_localValuePartitionsTouched;

      // Registry of touched (i.e. modified) BuiltinElementGrammars
      internal int m_n_grammarsTouched;
      internal IGrammar[] m_grammarsTouched;

      internal LocalNamePartition(StringTable outerInstance, string uri) : this(outerInstance, uri, EMPTY, (EXISchema)null) {
        this.outerInstance = outerInstance;
      }

      internal LocalNamePartition(StringTable outerInstance, string uri, string[] names, EXISchema schema) {
        this.outerInstance = outerInstance;
        this.uri = uri;
        uriId = -1;
        if (outerInstance.m_useMap) {
          m_nameMap = new Dictionary<string, LocalNameEntry>();
          m_initialNames = names;
        }
        else {
          m_nameMap = null;
          m_initialNames = null;
        }
        localNameEntries = new LocalNameEntry[256];
        int i;
        int len;
        for (i = 0, len = names.Length; i < len; i++) {
          string name = names[i];
          IGrammar grammar;
          if (schema != null) {
            int elem;
            if ((elem = schema.getGlobalElemOfSchema(uri, name)) != EXISchema.NIL_NODE) {
              grammar = outerInstance.m_grammarCache.getElementGrammarUse(elem);
            }
            else {
              grammar = null;
            }
          }
          else {
            grammar = null;
          }
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
        if (outerInstance.m_useMap) {
          m_nameMap.Clear();
          for (int i = 0; i < m_initialNames.Length; i++) {
            m_nameMap[m_initialNames[i]] = localNameEntries[i];
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
        Debug.Assert(localNameEntries[i].channel == null);
        localNameEntries[i].channel = channel;
        outerInstance.channels.AddLast(channel);
        ++outerInstance.n_channels;
      }

      public int getCompactId(string name) {
        LocalNameEntry item;
        if ((item = m_nameMap[name]) != null) {
          return item.number;
        }
        return -1;
      }

      public int internName(string name) {
        int number;
        if ((number = getCompactId(name)) != -1) {
          return number;
        }
        return addName(name, (IGrammar)null);
      }

      /// <summary>
      /// Add a name which has *not* been in the local-name partition. </summary>
      /// <returns> localName ID within the partition </returns>
      public int addName(string name, IGrammar grammar) {
        LocalNameEntry localNameEntry;
        int number = n_strings;
        int length;
        if ((length = localNameEntries.Length) == n_strings) {
          localNameEntry = null; // need a new entry
          int newLength = 2 * length;
          LocalNameEntry[] stringList = new LocalNameEntry[newLength];
          Array.Copy(localNameEntries, 0, stringList, 0, length);
          localNameEntries = stringList;
        }
        else if ((localNameEntry = localNameEntries[number]) != null) {
          localNameEntry.localName = name;
          localNameEntry.number = number;
          LocalValuePartition localValuePartition;
          if ((localValuePartition = localNameEntry.localValuePartition) != null) {
            localValuePartition.reset();
          }
          Debug.Assert(grammar == null);
          if ((grammar = localNameEntry.grammar) != null) {
            grammar.reset();
          }
        }

        if (localNameEntry == null) {
          localNameEntry = new LocalNameEntry(name, number, grammar);
        }

        if (n_strings == m_milestone) {
          ++width;
          m_milestone <<= 1;
        }
        localNameEntries[n_strings++] = localNameEntry;
        if (outerInstance.m_useMap) {
          m_nameMap[name] = localNameEntry;
        }
        return number;
      }

      public LocalValuePartition getLocalValuePartition(int i) {
        LocalNameEntry item = localNameEntries[i];
        LocalValuePartition localValuePartition;
        if ((localValuePartition = item.localValuePartition) != null) {
          return localValuePartition;
        }
        else {
          return item.localValuePartition = new LocalValuePartition(this);
        }
      }

      public void addTouchedValuePartitions(LocalValuePartition localValuePartition) {
        if (m_n_localValuePartitionsTouched == m_localValuePartitionsTouched.Length) {
          int nlen = m_n_localValuePartitionsTouched << 1;
          LocalValuePartition[] _localValuePartitions = new LocalValuePartition[nlen];
          Array.Copy(m_localValuePartitionsTouched, 0, _localValuePartitions, 0, m_n_localValuePartitionsTouched);
          m_localValuePartitionsTouched = _localValuePartitions;
        }
        m_localValuePartitionsTouched[m_n_localValuePartitionsTouched++] = localValuePartition;
      }

      public void addTouchedBuiltinElementGrammars(IGrammar builtinElementGrammar) {
        if (m_n_grammarsTouched == m_grammarsTouched.Length) {
          int nlen = m_n_grammarsTouched << 1;
          IGrammar[] _grammarsTouched = new IGrammar[nlen];
          Array.Copy(m_grammarsTouched, 0, _grammarsTouched, 0, m_n_grammarsTouched);
          m_grammarsTouched = _grammarsTouched;
        }
        m_grammarsTouched[m_n_grammarsTouched++] = builtinElementGrammar;
      }

    }

    public sealed class PrefixEntry {
      public readonly string value;
      internal readonly int number;
      internal PrefixEntry(string value, int number) {
        this.value = value;
        this.number = number;
      }
      public override int GetHashCode() {
        return value.GetHashCode();
      }
    }

    public sealed class PrefixPartition {
      private readonly StringTable outerInstance;


      public PrefixEntry[] prefixEntries;

      public int n_strings = 0;
      public int width = 0;
      public int forwardedWidth = 0;
      internal int m_milestone = 1;

      internal readonly int m_start_n_strings;
      internal readonly int m_start_width;
      internal readonly int m_start_forwardedWidth;
      internal readonly int m_start_milestone;

      internal readonly Dictionary<string, PrefixEntry> m_prefixMap;

      internal readonly string[] m_prefixes;

      internal PrefixPartition(StringTable outerInstance) : this(outerInstance, EMPTY) {
        this.outerInstance = outerInstance;
      }

      internal PrefixPartition(StringTable outerInstance, string[] prefixes) {
        this.outerInstance = outerInstance;
        m_prefixMap = outerInstance.m_useMap ? new Dictionary<string, PrefixEntry>() : null;
        m_prefixes = prefixes;

        prefixEntries = new PrefixEntry[1];

        int i;
        int len;
        for (i = 0, len = m_prefixes.Length; i < len; i++) {
          addPrefix(m_prefixes[i]);
        }
        m_start_n_strings = n_strings;
        m_start_width = width;
        m_start_forwardedWidth = forwardedWidth;
        m_start_milestone = m_milestone;
      }

      public void reset() {
        if (outerInstance.m_useMap) {
          m_prefixMap.Clear();
          int i;
          int len;
          for (i = 0, len = m_prefixes.Length; i < len; i++) {
            m_prefixMap[m_prefixes[i]] = prefixEntries[i];
          }
        }
        n_strings = m_start_n_strings;
        width = m_start_width;
        forwardedWidth = m_start_forwardedWidth;
        m_milestone = m_start_milestone;
      }

      public int getCompactId(string prefix) {
        PrefixEntry item;
        if (m_prefixMap.TryGetValue(prefix, out item)) {
          return item.number;
        }
        return -1;
      }

      public int internPrefix(string prefix) {
        int number;
        if ((number = getCompactId(prefix)) != -1) {
          return number;
        }
        return addPrefix(prefix);
      }

      /// <summary>
      /// Add a name which has *not* been in the partition.
      /// </summary>
      public int addPrefix(string prefix) {
        PrefixEntry item;
        int number = n_strings;
        item = new PrefixEntry(prefix, number);
        int length = prefixEntries.Length;
        if (n_strings == length) {
          int newLength = 2 * length;
          PrefixEntry[] uriList = new PrefixEntry[newLength];
          Array.Copy(prefixEntries, 0, uriList, 0, length);
          prefixEntries = uriList;
          length = newLength;
        }
        if (outerInstance.m_useMap) {
          m_prefixMap[prefix] = item;
        }
        prefixEntries[n_strings] = item;
        if (n_strings++ == m_milestone) {
          ++width;
          m_milestone <<= 1;
        }
        if (n_strings == m_milestone) {
          ++forwardedWidth;
        }
        return number;
      }
    }

    private sealed class URIEntry {
      internal string value;
      internal int number;
      internal LocalNamePartition localNamePartition;
      internal PrefixPartition prefixPartition;

      internal URIEntry(string value, int number, LocalNamePartition localNamePartition, PrefixPartition prefixPartition) {
        this.value = value;
        this.number = number;
        this.localNamePartition = localNamePartition;
        this.prefixPartition = prefixPartition;
      }

      public override int GetHashCode() {
        return value.GetHashCode();
      }
    }

    public class NumberedCharacters {
      public readonly Characters value;
      public readonly int number;
      internal NumberedCharacters(Characters value, int number) {
        this.value = value;
        this.number = number;
      }
      public override int GetHashCode() {
        return value.GetHashCode();
      }
    }

    public sealed class LocalValuePartition {

      public NumberedCharacters[] valueEntries;

      public int n_strings = 0;
      public int width = 0;
      internal int m_milestone = 1;

      internal readonly LocalNamePartition m_localNamePartition;

      internal LocalValuePartition(LocalNamePartition localNamePartition) {
        valueEntries = new NumberedCharacters[1];
        m_localNamePartition = localNamePartition;
      }

      internal void reset() {
        n_strings = 0;
        width = 0;
        m_milestone = 1;
      }

      internal void releaseEntry(int i) {
        valueEntries[i] = null;
      }

      /// <summary>
      /// Add a value which has *not* been in the local-value partition.
      /// </summary>
      internal int addString(Characters value) {
        NumberedCharacters item;
        int number = n_strings;
        item = new NumberedCharacters(value, number);
        int length = valueEntries.Length;
        if (n_strings == length) {
          int newLength = 2 * length;
          NumberedCharacters[] stringList = new NumberedCharacters[newLength];
          Array.Copy(valueEntries, 0, stringList, 0, length);
          valueEntries = stringList;
        }
        if (n_strings == m_milestone) {
          ++width;
          m_milestone <<= 1;
        }
        valueEntries[n_strings] = item;
        if (n_strings++ == 0) {
          m_localNamePartition.addTouchedValuePartitions(this);
        }
        return number;
      }
    }

    public sealed class GlobalValuePartition {
      private readonly StringTable outerInstance;


      public GlobalEntry[] valueEntries;
      internal readonly Dictionary<Characters, GlobalEntry> m_stringMap;

      public int globalID = 0;
      public int width = 0;
      internal int m_milestone = 1;
      internal bool m_wrapped = false;

      internal GlobalValuePartition(StringTable outerInstance) {
        this.outerInstance = outerInstance;
        m_stringMap = outerInstance.m_useMap ? new Dictionary<Characters, GlobalEntry>() : null;
        valueEntries = new GlobalEntry[1];
      }

      public void reset() {
        if (outerInstance.m_useMap) {
          m_stringMap.Clear();
        }
        init();
      }

      internal void init() {
        globalID = 0;
        width = 0;
        m_milestone = 1;
        m_wrapped = false;
      }

      public GlobalEntry getEntry(Characters characterSequence) {
        GlobalEntry item;
        if (m_stringMap.TryGetValue(characterSequence, out item)) {
          return item;
        }
        return null;
      }

      public LocalValuePartition getLocalPartition(int name, int uri) {
        LocalNamePartition localNamePartition = outerInstance.getLocalNamePartition(uri);
        return localNamePartition.getLocalValuePartition(name);
      }

      public void addValue(Characters characterSequence, int name, int uri) {
        if (characterSequence.isVolatile) {
        characterSequence.turnPermanent();
        }
        if (outerInstance.m_valuePartitionCapacity != 0) {
          LocalValuePartition localValuePartition;
          localValuePartition = outerInstance.getLocalNamePartition(uri).getLocalValuePartition(name);
          int localNumber = localValuePartition.addString(characterSequence);

          GlobalEntry newItem = new GlobalEntry(characterSequence, globalID, localValuePartition, localValuePartition.valueEntries[localNumber]);

          if (globalID == valueEntries.Length) {
            int newLength = 2 * globalID;
            GlobalEntry[] stringList = new GlobalEntry[newLength];
            Array.Copy(valueEntries, 0, stringList, 0, globalID);
            valueEntries = stringList;
          }
          if (globalID == m_milestone) {
            ++width;
            m_milestone <<= 1;
          }
          GlobalEntry item;
          if ((item = valueEntries[globalID]) != null) {
            if (m_wrapped) {
              item.localPartition.releaseEntry(item.localEntry.number);
            }
            if (outerInstance.m_useMap) {
              m_stringMap.Remove(item.value);
            }
          }
          valueEntries[globalID] = newItem;
          if (outerInstance.m_useMap) {
            m_stringMap[characterSequence] = newItem;
          }
          if (++globalID == outerInstance.m_valuePartitionCapacity) {
            globalID = 0;
            m_wrapped = true;
          }
        }
      }
    }

    public sealed class GlobalEntry {
      public readonly Characters value;
      public readonly int number;
      public readonly LocalValuePartition localPartition;
      public readonly NumberedCharacters localEntry;
      internal GlobalEntry(Characters value, int number, LocalValuePartition localPartition, NumberedCharacters localEntry) {
        this.value = value;
        this.number = number;
        this.localPartition = localPartition;
        this.localEntry = localEntry;
      }
    }

  }

}