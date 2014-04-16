package org.openexi.fujitsu.scomp;

/**
 * The errors and/or warnings found by EXISchemaFactory during schema-processing are
 * reported by this interface. Users of EXISchemaFactory needs to provide an
 * implementation of this interface to receive errors from EXISchemaFactory.
 */
public interface EXISchemaFactoryErrorHandler {
  /**
   * Report an warning found during schema-processing.
   * @param exc warning found
   * @throws EXISchemaFactoryException at the discretion of the application
   */
  public void warning(EXISchemaFactoryException exc)
    throws EXISchemaFactoryException;
  
  /**
   * Report an error found during schema-processing. Note that errors are
   * recoverable only as far as schema processor is concerned. They may be
   * fatal at the application level.
   * @param exc error found
   * @throws EXISchemaFactoryException at the discretion of the application
   */
  public void error(EXISchemaFactoryException exc)
      throws EXISchemaFactoryException;

  /**
   * Report a fatal error found during schema-processing.
   * @param exc fatal error found
   * @throws EXISchemaFactoryException at the discretion of the application
   */
  public void fatalError(EXISchemaFactoryException exc)
      throws EXISchemaFactoryException;
}