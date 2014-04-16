package org.openexi.proc.common;

import java.io.IOException;

public interface IBinaryValueScanner {

  public BinaryDataSource scan(long n_remainingBytes, BinaryDataSource binaryDataSource) throws IOException;
  
}
