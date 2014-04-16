package org.openexi.scomp;

/**
 * This interface reports exceptions from EXISchemaFactory during schema 
 * processing. Users of EXISchemaFactory need to provide an
 * implementation of this interface to receive errors from EXISchemaFactory.
 * @author Dennis Dawson
 */
public interface EXISchemaFactoryErrorHandler {
  /**
   * Report a warning found during schema processing.
   * @param exc warning found
   * @throws EXISchemaFactoryException at the discretion of the application
   */
  public void warning(EXISchemaFactoryException exc)
    throws EXISchemaFactoryException;
  
  /**
   * Report an error found during schema processing. Note that errors are
   * recoverable only as far as the schema processor is concerned. They might
   * be fatal at the application level.
   * @param exc error found
   * @throws EXISchemaFactoryException at the discretion of the application
   */
  public void error(EXISchemaFactoryException exc)
      throws EXISchemaFactoryException;

  /**
   * Report a fatal error found during schema processing.
   * @param exc fatal error found
   * @throws EXISchemaFactoryException at the discretion of the application
   */
  public void fatalError(EXISchemaFactoryException exc)
      throws EXISchemaFactoryException;
}