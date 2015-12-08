package org.openexi.proc.common;

/**
 * EXIOptions provides accessors for values associated with
 * EXI options in the EXI header of an EXI stream.
 */
public final class EXIOptions {
  /**
   * Use strict interpretation of XML Schema. Value is 1.
   * @y.exclude
   */
  public static final int ADD_LESSCOMMON               = 0x0001;
  /**
   * Process undefined elements and attributes. Value is 2.
   * @y.exclude
   */
  public static final int ADD_UNCOMMON                 = 0x0002;
  /**
   * An alignment has been set. Value is 4. 
   * @y.exclude
   */
  public static final int ADD_ALIGNMENT                = 0x0004;
  /**
   * Preservation options have been set. Value is 8.
   * @y.exclude
   */
  public static final int ADD_PRESERVE                 = 0x0008;
  /**
   * Header options document has the "common" element.
   * @y.exclude
   */
  public static final int ADD_COMMON                   = 0x0010;
  /**
   * A number has been set for the maximum length of entries
   * in the String Table. Value is 32.
   * @y.exclude
   */
  public static final int ADD_VALUE_MAX_LENGTH         = 0x0020;
  /**
   * A number has been set for the maximum number of entries
   * in the String Table. Value is 64.
   * @y.exclude
   */
  public static final int ADD_VALUE_PARTITION_CAPACITY = 0x0040;
  /**
   * The stream is an XML Fragment. Value is 128. 
   * @y.exclude
   */
  public static final int ADD_FRAGMENT                 = 0x0080;
  /**
   * The stream has an associated Datatype Representation Map. Value is 256.
   * @y.exclude
   */
  public static final int ADD_DTRM                     = 0x0100;

  /**
   * Default number of entities that will be read and processed as a group.
   * Default block size is 1,000,000 items.
   * @y.exclude
   */
  public static final int BLOCKSIZE_DEFAULT = 1000000;
  /**
   * Default maximum string length is unbounded. Value is -1.
   * @y.exclude
   */
  public static final int VALUE_MAX_LENGTH_UNBOUNDED = -1;
  /**
   * Default maximum number of partitions (entries in the String Table)
   * is unbounded. Value is -1.
   * @y.exclude
   */
  public static final int VALUE_PARTITION_CAPACITY_UNBOUNDED = -1;
  
  private AlignmentType m_alignmentType;
  
  private boolean m_isFragment;
  
  private boolean m_isStrict;
  
  private boolean m_preserveComments;
  private boolean m_preservePIs;
  private boolean m_preserveDTD;
  private boolean m_preserveNS;
  private boolean m_infuseSC;

  private SchemaId m_schemaId;
  
  private int m_blockSize;

  private int m_valueMaxLength;
  private int m_valuePartitionCapacity;
  
  private boolean m_preserveLexicalValues;
  
  private int m_n_datatypeRepresentationMapBindings;
  private QName[] m_datatypeRepresentationMap;
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public EXIOptions() {
    init();
    m_datatypeRepresentationMap = new QName[16];
    for (int i = 0; i < m_datatypeRepresentationMap.length; i++) {
      m_datatypeRepresentationMap[i] = new QName();
    }
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public void init() {
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
  /**
   * Get the bit alignment setting.
   * @return {@link org.openexi.proc.common.AlignmentType}
   */
  public AlignmentType getAlignmentType() {
    return m_alignmentType;
  }
  /**
   * An XML fragment is a non-compliant XML document with multiple root
   * elements. 
   * @return <i>true</i> if the stream is an XML fragment.
   */
  public boolean isFragment() {
    return m_isFragment;
  }
  /**
   * The Strict option applies to streams that have an associated XML Schema
   * and the data in the XML stream is 100% compliant with the schema.
   * @return <i>true</i> if using strict interpretation of an associated XML Schema.
   */
  public boolean isStrict() {
    return m_isStrict;
  }
  /**
   * Returns whether comments are conserved in the EXI Stream. 
   * @return <i>true</i> if comments are preserved.
   */
  public boolean getPreserveComments() {
    return m_preserveComments;
  }
  /**
   * Returns whether processing instructions are conserved in the EXI Stream. 
   * @return <i>true</i> if processing instructions are preserved.
   */  
  public boolean getPreservePIs() {
    return m_preservePIs;
  }
  /**
   * Returns whether the document type definition is conserved in the EXI Stream. 
   * @return <i>true</i> if the document type definition is preserved.
   */  
  public boolean getPreserveDTD() {
    return m_preserveDTD;
  }
  /**
   * Returns whether the namespaces are preserved in the EXI stream.
   * @return <i>true</i> if namespaces are preserved.
   */
  public boolean getPreserveNS() {
    return m_preserveNS;
  }
  /**
   * Self-contained option not supported in this release.
   * @y.exclude
   * @return
   */
  public boolean getInfuseSC() {
    return m_infuseSC;
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public SchemaId getSchemaId() {
    return m_schemaId;
  }
  
  /**
   * Returns the number of element and attribute values that are read and processed
   * as a group.
   * @return the current block size. Default is 1,000,000.
   */
  public int getBlockSize() {
    return m_blockSize;
  }
  /**
   * Returns the maximum length in characters of strings that will be included
   * in the String Table.
   * @return the maximum length of values added to the String Table. Default is unbounded (-1).
   */
  public int getValueMaxLength() {
    return m_valueMaxLength;
  }
  
  /**
   * Returns the maximum number of entries in the String Table.
   * @return the maximum number of partitions (entries) in the String Table. Default is unbounded (-1).
   */
  public int getValuePartitionCapacity() {
    return m_valuePartitionCapacity;
  }
  /**
   * Returns whether lexical values (literal strings) are preserved rather 
   * than the logical values of elements and attributes.
   * @return <i>true</i> if lexical values are preserved.
   */
  public boolean getPreserveLexicalValues() {
    return m_preserveLexicalValues;
  }
  /**
   * Returns the number of Datatype Representation Map QName pairs.
   * @return the number of DTRM bindings.
   */
  public int getDatatypeRepresentationMapBindingsCount() {
    return m_n_datatypeRepresentationMapBindings;
  }
  /**
   * Returns an array of qualified names that map XMLSchema datatypes to 
   * non-standard equivalents in EXI.
   * @return an array of qualified names comprising a DTRM.
   */
  public QName[] getDatatypeRepresentationMap() {
    return m_datatypeRepresentationMap;
  }

  /**
   * Set the bit alignment for the EXI stream. Default is <i>bit-packed</i>.
   * @param alignmentType {@link org.openexi.proc.common.AlignmentType}
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setAlignmentType(AlignmentType alignmentType) 
    throws EXIOptionsException {
    if (m_infuseSC) {
      if (m_alignmentType == AlignmentType.compress)
        throw new EXIOptionsException("selfContained option and compression option cannot be used together.");
      else if (m_alignmentType == AlignmentType.preCompress)
        throw new EXIOptionsException("selfContained option and pre-compression option cannot be used together.");
    }
    m_alignmentType = alignmentType;
  }
  /**
   * Set to true if the stream is a non-compliant XML document with multiple
   * root elements. Default is <i>false</i>. 
   * @param isFragment <i>true</i> if the XML stream is a fragment.
   * @y.exclude
   */
  public void setFragment(boolean isFragment) {
    m_isFragment = isFragment;
  }
  /**
   * Set to true if the EXI stream is 100% compliant with the accompanying
   * XSD (XML Schema Document).
   * @param isStrict <i>true</i> if the EXI stream is 100% compliant with its XSD
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setStrict(boolean isStrict) 
    throws EXIOptionsException {
    if (m_preserveComments)
      throw new EXIOptionsException("Preserve.comments option and strict option cannot be used together.");
    else if (m_preservePIs)
      throw new EXIOptionsException("Preserve.pis option and strict option cannot be used together.");
    else if (m_preserveDTD)
      throw new EXIOptionsException("Preserve.dtd option and strict option cannot be used together.");
    else if (m_preserveNS)
      throw new EXIOptionsException("Preserve.prefixes option and strict option cannot be used together.");
    else if (m_infuseSC)
      throw new EXIOptionsException("selfContained option and strict option cannot be used together.");
    m_isStrict = isStrict;
  }
  
  /**
   * Set to <i>true</i> to preserve comments in the EXI stream.
   * @param preserveComments <i>true</i> to preserve comments in the EXI stream.
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setPreserveComments(boolean preserveComments) 
    throws EXIOptionsException {
    if (m_isStrict && preserveComments) {
      throw new EXIOptionsException("Preserve.comments option and strict option cannot be used together.");
    }
    m_preserveComments = preserveComments;
  }
  /**
   * Set to <i>true</i> to preserve processing instructions.
   * @param preservePIs <i>true</i> to preserve processing instructions.
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setPreservePIs(boolean preservePIs) 
    throws EXIOptionsException {
    if (m_isStrict && preservePIs) {
      throw new EXIOptionsException("Preserve.pis option and strict option cannot be used together.");
    }
    m_preservePIs = preservePIs;
  }
  /**
   * Set to <i>true</i> to preserve the Document Type Definition.
   * @param preserveDTD <i>true</i> to preserve the Document Type Definition.
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setPreserveDTD(boolean preserveDTD) 
    throws EXIOptionsException {
    if (m_isStrict && preserveDTD) {
      throw new EXIOptionsException("Preserve.dtd option and strict option cannot be used together.");
    }
    m_preserveDTD = preserveDTD;
  }

  /**
   * Set to <i>true</i> to preserve namespaces in the EXI stream.
   * @param preserveNS <i>true</i> to preserve namespaces.
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setPreserveNS(boolean preserveNS) 
    throws EXIOptionsException {
    if (m_isStrict && preserveNS) {
      throw new EXIOptionsException("Preserve.prefixes option and strict option cannot be used together.");
    }
    m_preserveNS = preserveNS;
  }
  
  /**
   * The self-contained option is not supported in this release.
   * @param infuseSC
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setInfuseSC(boolean infuseSC) 
    throws EXIOptionsException {
    if (m_infuseSC != infuseSC) {
      if (infuseSC) {
        if (m_alignmentType == AlignmentType.compress)
          throw new EXIOptionsException("selfContained option and compression option cannot be used together.");
        else if (m_alignmentType == AlignmentType.preCompress)
          throw new EXIOptionsException("selfContained option and pre-compression option cannot be used together.");
        else if (m_isStrict)
          throw new EXIOptionsException("selfContained option and strict option cannot be used together.");
      }
      m_infuseSC = infuseSC;
    }
  }
  /**
   * Set the ID for the XML Schema Document used with the EXI stream.
   * @param schemaId
   * @y.exclude
   */
  public void setSchemaId(SchemaId schemaId) {
    m_schemaId = schemaId;
  }
  
  /**
   * Set the number of elements and attributes that will be read and processed
   * as a single group.
   * @param blockSize number of items processed as a block. Default is 1,000,000. 
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setBlockSize(int blockSize) throws EXIOptionsException {
    if (blockSize <= 0)
      throw new EXIOptionsException("blockSize option value cannot be a negative number.");
    m_blockSize = blockSize;
  }
  
  /**
   * Set the maximum length for a value that will be included in the String
   * Table.
   * @param valueMaxLength maximum length for entries in the String Table. Default is -1 (unbounded).
   * @y.exclude
   */
  public void setValueMaxLength(int valueMaxLength) {
    m_valueMaxLength = valueMaxLength;
  }
  /**
   * Set the maximum number of entries in the String Table. 
   * @param valuePartitionCapacity maximum number of entries in the String Table. Default is -1 (unbounded).
   * @y.exclude
   */
  public void setValuePartitionCapacity(int valuePartitionCapacity) {
    m_valuePartitionCapacity = valuePartitionCapacity;
  }
  /**
   * Set whether lexical values (original string values) are preserved
   * in the EXI stream.
   * @param preserveLexicalValues <i>true</i> if lexical values are preserved.
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setPreserveLexicalValues(boolean preserveLexicalValues) 
    throws EXIOptionsException {
    m_preserveLexicalValues = preserveLexicalValues;
  }
  /**
   * Set a datatype representation map (DTRM). The DTRM allows you to remap
   * XMLSchema datatypes to EXI datatypes other than their default equivalents.
   * The map is created using a sequence of Qualified Name pairs that identify
   * a datatype definition in the XMLSchema namespace followed by the new 
   * corresponding datatype mapping in the EXI namespace.
   * <br /><br />
   * For example, the following lines map the boolean datatype from XMLSchema 
   * to the integer datatype in EXI.
   * <pre>
   *   QName q1 = new QName("xsd:boolean","http://www.w3.org/2001/XMLSchema");
   *   QName q2 = new QName("exi:integer","http://www.w3.org/2009/exi");
   *   QName[] dtrm = new QName[2];
   *   dtrm = {q1, q2}; // Each mapping requires 2 qualified names.
   *   decoderInstance.setDatatypeRepresentationMap(dtrm, 1); // The array, and the number of pairs (1).
   * </pre>
   * 
   * @param dtrm a sequence of pairs of datatype QName and datatype representation QName
   * @param n_bindings the number of QName pairs
   * @y.exclude
   */
  public void setDatatypeRepresentationMap(QName[] datatypeRepresentationMap, int n_bindings) 
    throws EXIOptionsException {
    final int n_qnames = 2 * n_bindings;
    int i;
    for (i = 0; i < n_bindings; i++) {
      final int ind = i << 1;
      if (datatypeRepresentationMap[ind] == null || datatypeRepresentationMap[ind + 1] == null)
        throw new EXIOptionsException("A qname in datatypeRepresentationMap cannot be null.");
    }
    if (m_datatypeRepresentationMap.length < n_qnames) {
      final QName[] _datatypeRepresentationMap;
      _datatypeRepresentationMap = new QName[n_qnames];
      for (i = 0; i < m_datatypeRepresentationMap.length; i++) {
        _datatypeRepresentationMap[i] = m_datatypeRepresentationMap[i];
      }
      for (; i < _datatypeRepresentationMap.length; i++) {
        _datatypeRepresentationMap[i] = new QName();
      }
      m_datatypeRepresentationMap = _datatypeRepresentationMap;
    }
    for (i = 0; i < 2 * n_bindings; i++) {
      final QName qname = datatypeRepresentationMap[i];
      m_datatypeRepresentationMap[i].setValue(qname.namespaceName, qname.localName, qname.prefix, qname.qName);
    }
    m_n_datatypeRepresentationMapBindings = n_bindings;
  }
  /**
   * Add an entry to the datatype representation map. 
   * @param typeName EXIEvent with the URI and local name of the XSD datatype.
   * @param codecName EXIEvent with the URI and local name of corresponding EXI datatype.
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void appendDatatypeRepresentationMap(EventDescription typeName, EventDescription codecName)
    throws EXIOptionsException {
    if (typeName == null || codecName == null) {
      throw new EXIOptionsException("A qname in datatypeRepresentationMap cannot be null.");
    }
    final int n_bindings = m_n_datatypeRepresentationMapBindings + 1;
    final int n_qnames = 2 * n_bindings;
    int i;
    if (m_datatypeRepresentationMap.length < n_qnames) {
      final QName[] _datatypeRepresentationMap;
      _datatypeRepresentationMap = new QName[n_qnames];
      for (i = 0; i < m_datatypeRepresentationMap.length; i++) {
        _datatypeRepresentationMap[i] = m_datatypeRepresentationMap[i];
      }
      for (; i < _datatypeRepresentationMap.length; i++) {
        _datatypeRepresentationMap[i] = new QName();
      }
      m_datatypeRepresentationMap = _datatypeRepresentationMap;
    }
    int ind = 2 * m_n_datatypeRepresentationMapBindings;
    m_datatypeRepresentationMap[ind++].setValue(typeName.getURI(), typeName.getName(), null, null);
    m_datatypeRepresentationMap[ind++].setValue(codecName.getURI(), codecName.getName(), null, null);
    assert ind == n_qnames;
    m_n_datatypeRepresentationMapBindings = n_bindings;
  }
  
  /**
   * Transforms this EXIOptions instance into a GrammarOptions instance.
   * @return a GrammarOptions object with the same settings.
   * @y.exclude
   */
  public short toGrammarOptions() {
    short grammarOptions = GrammarOptions.DEFAULT_OPTIONS;
    if (m_isStrict) {
      return GrammarOptions.STRICT_OPTIONS;
    }
    else {
      if (m_preserveComments)
        grammarOptions = GrammarOptions.addCM(grammarOptions);
      if (m_preservePIs)
        grammarOptions = GrammarOptions.addPI(grammarOptions);
      if (m_preserveDTD)
        grammarOptions = GrammarOptions.addDTD(grammarOptions);
      if (m_preserveNS)
        grammarOptions = GrammarOptions.addNS(grammarOptions);
      if (m_infuseSC)
        grammarOptions = GrammarOptions.addSC(grammarOptions);
    }
    return grammarOptions;
  }
  
  /**
   * Parses and applies individual GrammarOptions settings. The grammarOptions
   * parameter is a short integer that is the sum of bit switches set for each
   * available option. 
   * @param grammarOptions a short integer that encapsulates grammar options.
   * @throws EXIOptionsException
   * @y.exclude
   */
  public void setGrammarOptions(short grammarOptions) throws EXIOptionsException {
    if (m_isStrict = (grammarOptions == GrammarOptions.STRICT_OPTIONS)) {
      m_preserveComments = false;
      m_preservePIs = false;
      m_preserveDTD = false;
      m_preserveNS = false;
      m_infuseSC = false;
    }
    else {
      m_preserveComments = GrammarOptions.hasCM(grammarOptions);
      m_preservePIs = GrammarOptions.hasPI(grammarOptions);
      m_preserveDTD = GrammarOptions.hasDTD(grammarOptions); 
      m_preserveNS = GrammarOptions.hasNS(grammarOptions); 
      setInfuseSC(GrammarOptions.hasSC(grammarOptions));
    }
  }
  /**
   * Not for public use.
   * @return
   * @y.exclude
   */
  public int getOutline(boolean outputSchemaId) {
    int outline = 0;
    
    boolean hasAlignment = false;
    if (m_alignmentType == AlignmentType.byteAligned ||
        m_alignmentType == AlignmentType.preCompress) {
      hasAlignment = true;
    }
    final boolean hasValuePartitionCapacity;
    hasValuePartitionCapacity = m_valuePartitionCapacity != VALUE_PARTITION_CAPACITY_UNBOUNDED;

    final boolean hasValueMaxLength;
    hasValueMaxLength = m_valueMaxLength != VALUE_MAX_LENGTH_UNBOUNDED;
    
    final boolean hasDTRM;
    hasDTRM = m_n_datatypeRepresentationMapBindings != 0;

    boolean hasUncommon = false;
    if (hasAlignment)
      hasUncommon = true;
    else if (m_infuseSC)
      hasUncommon = true;
    else if (hasValueMaxLength)
      hasUncommon = true;
    else if (hasValuePartitionCapacity)
      hasUncommon = true;
    else if (hasDTRM)
      hasUncommon = true;

    boolean hasPreserve = false;
    if (m_preserveComments || m_preservePIs || m_preserveDTD || m_preserveNS || m_preserveLexicalValues) {
      hasPreserve = true;
    }

    boolean hasBlockSize = false;
    if (m_blockSize != BLOCKSIZE_DEFAULT)
      hasBlockSize = true;
    
    boolean hasLessCommon = false;
    if (hasUncommon || hasPreserve || hasBlockSize) {
      hasLessCommon = true;
    }

    boolean hasCommon = false;
    if (m_alignmentType == AlignmentType.compress) {
      hasCommon = true;
    }
    else if (m_isFragment) {
      hasCommon = true;
    }
    else if (outputSchemaId) {
      hasCommon = true;
    }

    if (hasLessCommon)
      outline |= ADD_LESSCOMMON;
    if (hasUncommon) {
      outline |= ADD_UNCOMMON;
      if (hasAlignment)
        outline |= ADD_ALIGNMENT;
      if (hasValueMaxLength)
        outline |= ADD_VALUE_MAX_LENGTH;
      if (hasValuePartitionCapacity)
        outline |= ADD_VALUE_PARTITION_CAPACITY;
      if (hasDTRM)
        outline |= ADD_DTRM;
    }
    if (hasPreserve)
      outline |= ADD_PRESERVE;
    if (hasCommon) {
      outline |= ADD_COMMON;
      if (m_isFragment)
        outline |= ADD_FRAGMENT;
    }
    
    return outline;
  }
  
}
