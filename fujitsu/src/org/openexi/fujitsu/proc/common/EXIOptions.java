package org.openexi.fujitsu.proc.common;


public final class EXIOptions {
  
  public static final int ADD_LESSCOMMON               = 0x0001;
  public static final int ADD_UNCOMMON                 = 0x0002;
  public static final int ADD_ALIGNMENT                = 0x0004;
  public static final int ADD_PRESERVE                 = 0x0008;
  public static final int ADD_COMMON                   = 0x0010;
  public static final int ADD_VALUE_MAX_LENGTH         = 0x0020;
  public static final int ADD_VALUE_PARTITION_CAPACITY = 0x0040;
  public static final int ADD_FRAGMENT                 = 0x0080;
  public static final int ADD_DTRM                     = 0x0100;

  public static final int BLOCKSIZE_DEFAULT = 1000000;
  public static final int VALUE_MAX_LENGTH_UNBOUNDED = -1;
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
  
  public EXIOptions() {
    init();
    m_datatypeRepresentationMap = new QName[16];
    for (int i = 0; i < m_datatypeRepresentationMap.length; i++) {
      m_datatypeRepresentationMap[i] = new QName();
    }
  }
  
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
  
  public AlignmentType getAlignmentType() {
    return m_alignmentType;
  }
  
  public boolean isFragment() {
    return m_isFragment;
  }
  
  public boolean isStrict() {
    return m_isStrict;
  }
  
  public boolean getPreserveComments() {
    return m_preserveComments;
  }
  
  public boolean getPreservePIs() {
    return m_preservePIs;
  }
  
  public boolean getPreserveDTD() {
    return m_preserveDTD;
  }
  
  public boolean getPreserveNS() {
    return m_preserveNS;
  }

  public boolean getInfuseSC() {
    return m_infuseSC;
  }
  
  public SchemaId getSchemaId() {
    return m_schemaId;
  }
  
  public int getBlockSize() {
    return m_blockSize;
  }

  public int getValueMaxLength() {
    return m_valueMaxLength;
  }
  
  public int getValuePartitionCapacity() {
    return m_valuePartitionCapacity;
  }
  
  public boolean getPreserveLexicalValues() {
    return m_preserveLexicalValues;
  }
  
  public int getDatatypeRepresentationMapBindingsCount() {
    return m_n_datatypeRepresentationMapBindings;
  }
  
  public QName[] getDatatypeRepresentationMap() {
    return m_datatypeRepresentationMap;
  }

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

  public void setFragment(boolean isFragment) {
    m_isFragment = isFragment;
  }

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
  
  public void setPreserveComments(boolean preserveComments) 
    throws EXIOptionsException {
    if (m_isStrict && preserveComments) {
      throw new EXIOptionsException("Preserve.comments option and strict option cannot be used together.");
    }
    m_preserveComments = preserveComments;
  }

  public void setPreservePIs(boolean preservePIs) 
    throws EXIOptionsException {
    if (m_isStrict && preservePIs) {
      throw new EXIOptionsException("Preserve.pis option and strict option cannot be used together.");
    }
    m_preservePIs = preservePIs;
  }

  public void setPreserveDTD(boolean preserveDTD) 
    throws EXIOptionsException {
    if (m_isStrict && preserveDTD) {
      throw new EXIOptionsException("Preserve.dtd option and strict option cannot be used together.");
    }
    m_preserveDTD = preserveDTD;
  }

  public void setPreserveNS(boolean preserveNS) 
    throws EXIOptionsException {
    if (m_isStrict && preserveNS) {
      throw new EXIOptionsException("Preserve.prefixes option and strict option cannot be used together.");
    }
    m_preserveNS = preserveNS;
  }

  void setInfuseSC(boolean infuseSC) 
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

  public void setSchemaId(SchemaId schemaId) {
    m_schemaId = schemaId;
  }
  
  public void setBlockSize(int blockSize) throws EXIOptionsException {
    if (blockSize <= 0)
      throw new EXIOptionsException("blockSize option value cannot be a negative number.");
    m_blockSize = blockSize;
  }
  
  public void setValueMaxLength(int valueMaxLength) {
    m_valueMaxLength = valueMaxLength;
  }

  public void setValuePartitionCapacity(int valuePartitionCapacity) {
    m_valuePartitionCapacity = valuePartitionCapacity;
  }

  public void setPreserveLexicalValues(boolean preserveLexicalValues) 
    throws EXIOptionsException {
    m_preserveLexicalValues = preserveLexicalValues;
  }

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
  
  public void appendDatatypeRepresentationMap(EXIEvent typeName, EXIEvent codecName)
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
   * Turn this EXI Options into a GrammarOptions instance.
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

  public int getOutline() {
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
    else if (m_schemaId != null) {
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
