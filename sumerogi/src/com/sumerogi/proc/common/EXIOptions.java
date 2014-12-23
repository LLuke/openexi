package com.sumerogi.proc.common;

/**
 * EXIOptions provides accessors for values associated with
 * EXI options in the EXI header of an EXI stream.
 */
public final class EXIOptions {

  /**
   * Default number of entities that will be read and processed as a group.
   * Default block size is 1,000,000 items.
   * @y.exclude
   */
  public static final int BLOCKSIZE_DEFAULT = 1000000;
  
  private AlignmentType m_alignmentType;
  
  private int m_blockSize;

  /**
   * Not for public use.
   * @y.exclude
   */
  public EXIOptions() {
    init();
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public void init() {
    m_alignmentType = AlignmentType.bitPacked;
    m_blockSize = BLOCKSIZE_DEFAULT;
  }
  /**
   * Get the bit alignment setting.
   * @return {@link org.openexi.proc.common.AlignmentType}
   */
  public AlignmentType getAlignmentType() {
    return m_alignmentType;
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
   * Set the bit alignment for the EXI stream. Default is <i>bit-packed</i>.
   * @param alignmentType {@link org.openexi.proc.common.AlignmentType}
   * @y.exclude
   */
  public void setAlignmentType(AlignmentType alignmentType) {
    m_alignmentType = alignmentType;
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
  
}
