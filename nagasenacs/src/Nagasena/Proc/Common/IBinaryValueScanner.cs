namespace Nagasena.Proc.Common {

  /// <exclude/>
  public interface IBinaryValueScanner {

    BinaryDataSource scan(long n_remainingBytes, BinaryDataSource binaryDataSource);

  }

}