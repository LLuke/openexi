package org.openexi.tryout;

import org.openexi.util.XMLResourceBundle;

public class SchemaCompilerXMsg extends XMLResourceBundle {

  //static final int MF_STATUS_MESSAGE_POSITION       = 1;
  static final int MF_STATUS_WARNING_POSITION       = 2;
  static final int MF_STATUS_ERROR_POSITION         = 3;
  static final int MF_STATUS_FATAL_ERROR_POSITION   = 4;

  static final int MF_STATUS_ERROR_POSITION_NOT_AVAIL = 5;
  static final int MF_STATUS_NOT_A_VALID_XML = 6;
  static final int MF_STATUS_NOT_A_VALID_URI = 7;
  static final int MF_STATUS_IS_VALID_SCHEMA = 8;
  static final int MF_STATUS_IS_VALID_INSTANCE = 9;
  static final int MF_STATUS_NUM_OF_ERRORS = 10;
  static final int MF_STATUS_IO_ERROR_FILE_READ = 11;
  static final int MF_STATUS_IO_SUCCESS_FILE_SAVED = 12;
  static final int MF_STATUS_CONFIRMATION_FILE_OVERWRITE = 13;
  
  static final int MF_MESSAGE_CONVERSION_SUMMARY = 14;

}
