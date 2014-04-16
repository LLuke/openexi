namespace Nagasena.Proc.Common {

  public interface IBinaryValueScanner {

    BinaryDataSource scan(long n_remainingBytes, BinaryDataSource binaryDataSource);

  }

}