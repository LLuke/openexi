namespace Nagasena.Proc {

  /// <summary>
  /// This enumeration provides three possible settings for header options output.
  /// <br /><br />
  /// <ul>
  /// <li><i>none</i> &ndash; Header options are not included in the header.
  /// The receiver of the document must have precise knowledge of the 
  /// settings used to encode the document.<br /><br />
  /// </li>
  /// <li>
  /// <i>lessSchemaId</i> &ndash; Header options are present. Every
  /// setting used is written out in the header options, except SchemaID.
  /// The receiver of the document must know which schema is used to 
  /// encode the document.<br /><br />
  /// </li>
  /// <li>
  /// <i>all</i> &ndash; All header options are present. Every setting
  /// used is written to the header options, including SchemaID.
  /// </li>
  /// </ul>
  /// </summary>
  public enum HeaderOptionsOutputType {

    none,
    lessSchemaId,
    all

  }

}