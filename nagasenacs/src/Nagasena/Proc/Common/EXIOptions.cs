using System.Diagnostics;

namespace Nagasena.Proc.Common {

  /// <summary>
  /// EXIOptions provides accessors for values associated with
  /// EXI options in the EXI header of an EXI stream.
  /// </summary>
  public sealed class EXIOptions {
    /// <summary>
    /// Use strict interpretation of XML Schema. Value is 1.
    /// @y.exclude
    /// </summary>
    public const int ADD_LESSCOMMON = 0x0001;
    /// <summary>
    /// Process undefined elements and attributes. Value is 2.
    /// @y.exclude
    /// </summary>
    public const int ADD_UNCOMMON = 0x0002;
    /// <summary>
    /// An alignment has been set. Value is 4. 
    /// @y.exclude
    /// </summary>
    public const int ADD_ALIGNMENT = 0x0004;
    /// <summary>
    /// Preservation options have been set. Value is 8.
    /// @y.exclude
    /// </summary>
    public const int ADD_PRESERVE = 0x0008;
    /// <summary>
    /// Header options document has the "common" element.
    /// @y.exclude
    /// </summary>
    public const int ADD_COMMON = 0x0010;
    /// <summary>
    /// A number has been set for the maximum length of entries
    /// in the String Table. Value is 32.
    /// @y.exclude
    /// </summary>
    public const int ADD_VALUE_MAX_LENGTH = 0x0020;
    /// <summary>
    /// A number has been set for the maximum number of entries
    /// in the String Table. Value is 64.
    /// @y.exclude
    /// </summary>
    public const int ADD_VALUE_PARTITION_CAPACITY = 0x0040;
    /// <summary>
    /// The stream is an XML Fragment. Value is 128. 
    /// @y.exclude
    /// </summary>
    public const int ADD_FRAGMENT = 0x0080;
    /// <summary>
    /// The stream has an associated Datatype Representation Map. Value is 256.
    /// @y.exclude
    /// </summary>
    public const int ADD_DTRM = 0x0100;

    /// <summary>
    /// Default number of entities that will be read and processed as a group.
    /// Default block size is 1,000,000 items.
    /// @y.exclude
    /// </summary>
    public const int BLOCKSIZE_DEFAULT = 1000000;
    /// <summary>
    /// Default maximum string length is unbounded. Value is -1.
    /// @y.exclude
    /// </summary>
    public const int VALUE_MAX_LENGTH_UNBOUNDED = -1;
    /// <summary>
    /// Default maximum number of partitions (entries in the String Table)
    /// is unbounded. Value is -1.
    /// @y.exclude
    /// </summary>
    public const int VALUE_PARTITION_CAPACITY_UNBOUNDED = -1;

    private AlignmentType m_alignmentType;

    private bool m_isFragment;

    private bool m_isStrict;

    private bool m_preserveComments;
    private bool m_preservePIs;
    private bool m_preserveDTD;
    private bool m_preserveNS;
    private bool m_infuseSC;

    private SchemaId m_schemaId;

    private int m_blockSize;

    private int m_valueMaxLength;
    private int m_valuePartitionCapacity;

    private bool m_preserveLexicalValues;

    private int m_n_datatypeRepresentationMapBindings;
    private QName[] m_datatypeRepresentationMap;

    internal EXIOptions() {
      init();
      m_datatypeRepresentationMap = new QName[16];
      for (int i = 0; i < m_datatypeRepresentationMap.Length; i++) {
        m_datatypeRepresentationMap[i] = new QName();
      }
    }

    internal void init() {
      m_alignmentType = AlignmentType.bitPacked;
      m_isFragment = false;
      m_isStrict = false;
      m_preserveComments = false;
      m_preservePIs = false;
      m_preserveDTD = false;
      m_preserveNS = false;
      m_preserveLexicalValues = false;
      m_infuseSC = false;
      m_schemaId = null;
      m_blockSize = BLOCKSIZE_DEFAULT;
      m_valueMaxLength = VALUE_MAX_LENGTH_UNBOUNDED;
      m_valuePartitionCapacity = VALUE_PARTITION_CAPACITY_UNBOUNDED;
      m_n_datatypeRepresentationMapBindings = 0;
    }

    /// <summary>
    /// Get the bit alignment setting. </summary>
    /// <returns> <seealso cref="org.openexi.proc.common.AlignmentType"/> </returns>
    public AlignmentType AlignmentType {
      get {
        return m_alignmentType;
      }
      set {
        if (m_infuseSC) {
          if (m_alignmentType == AlignmentType.compress) {
            throw new EXIOptionsException("selfContained option and compression option cannot be used together.");
          }
          else if (m_alignmentType == AlignmentType.preCompress) {
            throw new EXIOptionsException("selfContained option and pre-compression option cannot be used together.");
          }
        }
        m_alignmentType = value;
      }
    }

    /// <summary>
    /// An XML fragment is a non-compliant XML document with multiple root
    /// elements. </summary>
    /// <returns> <i>true</i> if the stream is an XML fragment. </returns>
    public bool Fragment {
      get {
        return m_isFragment;
      }
      set {
        m_isFragment = value;
      }
    }

    /// <summary>
    /// The Strict option applies to streams that have an associated XML Schema
    /// and the data in the XML stream is 100% compliant with the schema. </summary>
    /// <returns> <i>true</i> if using strict interpretation of an associated XML Schema. </returns>
    public bool Strict {
      get {
        return m_isStrict;
      }
      set {
        if (m_preserveComments) {
          throw new EXIOptionsException("Preserve.comments option and strict option cannot be used together.");
        }
        else if (m_preservePIs) {
          throw new EXIOptionsException("Preserve.pis option and strict option cannot be used together.");
        }
        else if (m_preserveDTD) {
          throw new EXIOptionsException("Preserve.dtd option and strict option cannot be used together.");
        }
        else if (m_preserveNS) {
          throw new EXIOptionsException("Preserve.prefixes option and strict option cannot be used together.");
        }
        else if (m_infuseSC) {
          throw new EXIOptionsException("selfContained option and strict option cannot be used together.");
        }
        m_isStrict = value;
      }
    }

    /// <summary>
    /// Returns whether comments are conserved in the EXI Stream. </summary>
    /// <returns> <i>true</i> if comments are preserved. </returns>
    public bool PreserveComments {
      get {
        return m_preserveComments;
      }
      set {
        if (m_isStrict && value) {
          throw new EXIOptionsException("Preserve.comments option and strict option cannot be used together.");
        }
        m_preserveComments = value;
      }
    }

    /// <summary>
    /// Returns whether processing instructions are conserved in the EXI Stream. </summary>
    /// <returns> <i>true</i> if processing instructions are preserved. </returns>
    public bool PreservePIs {
      get {
        return m_preservePIs;
      }
      set {
        if (m_isStrict && value) {
          throw new EXIOptionsException("Preserve.pis option and strict option cannot be used together.");
        }
        m_preservePIs = value;
      }
    }

    /// <summary>
    /// Returns whether the document type definition is conserved in the EXI Stream. </summary>
    /// <returns> <i>true</i> if the document type definition is preserved. </returns>
    public bool PreserveDTD {
      get {
        return m_preserveDTD;
      }
      set {
        if (m_isStrict && value) {
          throw new EXIOptionsException("Preserve.dtd option and strict option cannot be used together.");
        }
        m_preserveDTD = value;
      }
    }

    /// <summary>
    /// Returns whether the namespaces are preserved in the EXI stream. </summary>
    /// <returns> <i>true</i> if namespaces are preserved. </returns>
    public bool PreserveNS {
      get {
        return m_preserveNS;
      }
      set {
        if (m_isStrict && value) {
          throw new EXIOptionsException("Preserve.prefixes option and strict option cannot be used together.");
        }
        m_preserveNS = value;
      }
    }

    /// <summary>
    /// Self-contained option not supported in this release.
    /// @return
    /// </summary>
    internal bool InfuseSC {
      get {
        return m_infuseSC;
      }
      set {
        if (m_infuseSC != value) {
          if (value) {
            if (m_alignmentType == AlignmentType.compress) {
              throw new EXIOptionsException("selfContained option and compression option cannot be used together.");
            }
            else if (m_alignmentType == AlignmentType.preCompress) {
              throw new EXIOptionsException("selfContained option and pre-compression option cannot be used together.");
            }
            else if (m_isStrict) {
              throw new EXIOptionsException("selfContained option and strict option cannot be used together.");
            }
          }
          m_infuseSC = value;
        }
      }
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public SchemaId SchemaId {
      get {
        return m_schemaId;
      }
      set {
        m_schemaId = value;
      }
    }

    /// <summary>
    /// Returns the number of element and attribute values that are read and processed
    /// as a group. </summary>
    /// <returns> the current block size. Default is 1,000,000. </returns>
    public int BlockSize {
      get {
        return m_blockSize;
      }
      set {
        if (value <= 0) {
          throw new EXIOptionsException("blockSize option value cannot be a negative number.");
        }
        m_blockSize = value;
      }
    }

    /// <summary>
    /// Returns the maximum length in characters of strings that will be included
    /// in the String Table. </summary>
    /// <returns> the maximum length of values added to the String Table. Default is unbounded (-1). </returns>
    public int ValueMaxLength {
      get {
        return m_valueMaxLength;
      }
      set {
        m_valueMaxLength = value;
      }
    }

    /// <summary>
    /// Returns the maximum number of entries in the String Table. </summary>
    /// <returns> the maximum number of partitions (entries) in the String Table. Default is unbounded (-1). </returns>
    public int ValuePartitionCapacity {
      get {
        return m_valuePartitionCapacity;
      }
      set {
        m_valuePartitionCapacity = value;
      }
    }

    /// <summary>
    /// Returns whether lexical values (literal strings) are preserved rather 
    /// than the logical values of elements and attributes. </summary>
    /// <returns> <i>true</i> if lexical values are preserved. </returns>
    public bool PreserveLexicalValues {
      get {
        return m_preserveLexicalValues;
      }
      set {
        m_preserveLexicalValues = value;
      }
    }

    /// <summary>
    /// Returns the number of Datatype Representation Map QName pairs. </summary>
    /// <returns> the number of DTRM bindings. </returns>
    public int DatatypeRepresentationMapBindingsCount {
      get {
        return m_n_datatypeRepresentationMapBindings;
      }
    }

    /// <summary>
    /// Returns an array of qualified names that map XMLSchema datatypes to 
    /// non-standard equivalents in EXI. </summary>
    /// <returns> an array of qualified names comprising a DTRM. </returns>
    public QName[] DatatypeRepresentationMap {
      get {
        return m_datatypeRepresentationMap;
      }
    }

    /// <summary>
    /// Set a datatype representation map (DTRM). The DTRM allows you to remap
    /// XMLSchema datatypes to EXI datatypes other than their default equivalents.
    /// The map is created using a sequence of Qualified Name pairs that identify
    /// a datatype definition in the XMLSchema namespace followed by the new 
    /// corresponding datatype mapping in the EXI namespace.
    /// <br /><br />
    /// For example, the following lines map the boolean datatype from XMLSchema 
    /// to the integer datatype in EXI.
    /// <pre>
    ///   QName q1 = new QName("xsd:boolean","http://www.w3.org/2001/XMLSchema");
    ///   QName q2 = new QName("exi:integer","http://www.w3.org/2009/exi");
    ///   QName[] dtrm = new QName[2];
    ///   dtrm = {q1, q2}; // Each mapping requires 2 qualified names.
    ///   decoderInstance.setDatatypeRepresentationMap(dtrm, 1); // The array, and the number of pairs (1).
    /// </pre>
    /// </summary>
    /// <param name="dtrm"> a sequence of pairs of datatype QName and datatype representation QName </param>
    /// <param name="n_bindings"> the number of QName pairs</param>
    internal void setDatatypeRepresentationMap(QName[] datatypeRepresentationMap, int n_bindings) {
      int n_qnames = 2 * n_bindings;
      int i;
      for (i = 0; i < n_bindings; i++) {
        int ind = i << 1;
        if (datatypeRepresentationMap[ind] == null || datatypeRepresentationMap[ind + 1] == null) {
          throw new EXIOptionsException("A qname in datatypeRepresentationMap cannot be null.");
        }
      }
      if (m_datatypeRepresentationMap.Length < n_qnames) {
        QName[] _datatypeRepresentationMap;
        _datatypeRepresentationMap = new QName[n_qnames];
        for (i = 0; i < m_datatypeRepresentationMap.Length; i++) {
          _datatypeRepresentationMap[i] = m_datatypeRepresentationMap[i];
        }
        for (; i < _datatypeRepresentationMap.Length; i++) {
          _datatypeRepresentationMap[i] = new QName();
        }
        m_datatypeRepresentationMap = _datatypeRepresentationMap;
      }
      for (i = 0; i < 2 * n_bindings; i++) {
        QName qname = datatypeRepresentationMap[i];
        m_datatypeRepresentationMap[i].setValue(qname.namespaceName, qname.localName, qname.prefix, qname.qName);
      }
      m_n_datatypeRepresentationMapBindings = n_bindings;
    }

    /// <summary>
    /// Add an entry to the datatype representation map. </summary>
    /// <param name="typeName"> EXIEvent with the URI and local name of the XSD datatype. </param>
    /// <param name="codecName"> EXIEvent with the URI and local name of corresponding EXI datatype. </param>
    /// <exception cref="EXIOptionsException">
    /// </exception>
    internal void appendDatatypeRepresentationMap(EventDescription typeName, EventDescription codecName) {
      if (typeName == null || codecName == null) {
        throw new EXIOptionsException("A qname in datatypeRepresentationMap cannot be null.");
      }
      int n_bindings = m_n_datatypeRepresentationMapBindings + 1;
      int n_qnames = 2 * n_bindings;
      int i;
      if (m_datatypeRepresentationMap.Length < n_qnames) {
        QName[] _datatypeRepresentationMap;
        _datatypeRepresentationMap = new QName[n_qnames];
        for (i = 0; i < m_datatypeRepresentationMap.Length; i++) {
          _datatypeRepresentationMap[i] = m_datatypeRepresentationMap[i];
        }
        for (; i < _datatypeRepresentationMap.Length; i++) {
          _datatypeRepresentationMap[i] = new QName();
        }
        m_datatypeRepresentationMap = _datatypeRepresentationMap;
      }
      int ind = 2 * m_n_datatypeRepresentationMapBindings;
      m_datatypeRepresentationMap[ind++].setValue(typeName.URI, typeName.Name, null, null);
      m_datatypeRepresentationMap[ind++].setValue(codecName.URI, codecName.Name, null, null);
      Debug.Assert(ind == n_qnames);
      m_n_datatypeRepresentationMapBindings = n_bindings;
    }

    /// <summary>
    /// Transforms this EXIOptions instance into a GrammarOptions instance. </summary>
    /// <returns> a GrammarOptions object with the same settings.
    /// @y.exclude </returns>
    public short toGrammarOptions() {
      short grammarOptions = Nagasena.Proc.Common.GrammarOptions.DEFAULT_OPTIONS;
      if (m_isStrict) {
        return Nagasena.Proc.Common.GrammarOptions.STRICT_OPTIONS;
      }
      else {
        if (m_preserveComments) {
          grammarOptions = Nagasena.Proc.Common.GrammarOptions.addCM(grammarOptions);
        }
        if (m_preservePIs) {
          grammarOptions = Nagasena.Proc.Common.GrammarOptions.addPI(grammarOptions);
        }
        if (m_preserveDTD) {
          grammarOptions = Nagasena.Proc.Common.GrammarOptions.addDTD(grammarOptions);
        }
        if (m_preserveNS) {
          grammarOptions = Nagasena.Proc.Common.GrammarOptions.addNS(grammarOptions);
        }
        if (m_infuseSC) {
          grammarOptions = Nagasena.Proc.Common.GrammarOptions.addSC(grammarOptions);
        }
      }
      return grammarOptions;
    }

    /// <summary>
    /// Parses and applies individual GrammarOptions settings. The grammarOptions
    /// parameter is a short integer that is the sum of bit switches set for each
    /// available option. </summary>
    /// <param name="grammarOptions"> a short integer that encapsulates grammar options. </param>
    /// <exception cref="EXIOptionsException">
    /// </exception>
    internal short GrammarOptions {
      set {
        if (m_isStrict = (value == Nagasena.Proc.Common.GrammarOptions.STRICT_OPTIONS)) {
          m_preserveComments = false;
          m_preservePIs = false;
          m_preserveDTD = false;
          m_preserveNS = false;
          m_infuseSC = false;
        }
        else {
          m_preserveComments = Nagasena.Proc.Common.GrammarOptions.hasCM(value);
          m_preservePIs = Nagasena.Proc.Common.GrammarOptions.hasPI(value);
          m_preserveDTD = Nagasena.Proc.Common.GrammarOptions.hasDTD(value);
          m_preserveNS = Nagasena.Proc.Common.GrammarOptions.hasNS(value);
          InfuseSC = Nagasena.Proc.Common.GrammarOptions.hasSC(value);
        }
      }
    }

    /// <summary>
    /// Not for public use.
    /// @return
    /// </summary>
    internal int getOutline(bool outputSchemaId) {
      int outline = 0;

      bool hasAlignment = false;
      if (m_alignmentType == AlignmentType.byteAligned || m_alignmentType == AlignmentType.preCompress) {
        hasAlignment = true;
      }
      bool hasValuePartitionCapacity;
      hasValuePartitionCapacity = m_valuePartitionCapacity != VALUE_PARTITION_CAPACITY_UNBOUNDED;

      bool hasValueMaxLength;
      hasValueMaxLength = m_valueMaxLength != VALUE_MAX_LENGTH_UNBOUNDED;

      bool hasDTRM;
      hasDTRM = m_n_datatypeRepresentationMapBindings != 0;

      bool hasUncommon = false;
      if (hasAlignment) {
        hasUncommon = true;
      }
      else if (m_infuseSC) {
        hasUncommon = true;
      }
      else if (hasValueMaxLength) {
        hasUncommon = true;
      }
      else if (hasValuePartitionCapacity) {
        hasUncommon = true;
      }
      else if (hasDTRM) {
        hasUncommon = true;
      }

      bool hasPreserve = false;
      if (m_preserveComments || m_preservePIs || m_preserveDTD || m_preserveNS || m_preserveLexicalValues) {
        hasPreserve = true;
      }

      bool hasBlockSize = false;
      if (m_blockSize != BLOCKSIZE_DEFAULT) {
        hasBlockSize = true;
      }

      bool hasLessCommon = false;
      if (hasUncommon || hasPreserve || hasBlockSize) {
        hasLessCommon = true;
      }

      bool hasCommon = false;
      if (m_alignmentType == AlignmentType.compress) {
        hasCommon = true;
      }
      else if (m_isFragment) {
        hasCommon = true;
      }
      else if (outputSchemaId && m_schemaId != null) {
        hasCommon = true;
      }

      if (hasLessCommon) {
        outline |= ADD_LESSCOMMON;
      }
      if (hasUncommon) {
        outline |= ADD_UNCOMMON;
        if (hasAlignment) {
          outline |= ADD_ALIGNMENT;
        }
        if (hasValueMaxLength) {
          outline |= ADD_VALUE_MAX_LENGTH;
        }
        if (hasValuePartitionCapacity) {
          outline |= ADD_VALUE_PARTITION_CAPACITY;
        }
        if (hasDTRM) {
          outline |= ADD_DTRM;
        }
      }
      if (hasPreserve) {
        outline |= ADD_PRESERVE;
      }
      if (hasCommon) {
        outline |= ADD_COMMON;
        if (m_isFragment) {
          outline |= ADD_FRAGMENT;
        }
      }

      return outline;
    }

  }

}