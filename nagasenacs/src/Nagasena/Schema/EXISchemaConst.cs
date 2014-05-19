namespace Nagasena.Schema {

  /// <summary>
  /// EXISchemaConst lists the constant serial numbers given to the built-in schema
  /// types in the compiled schema.
  /// Note that it is the serial numbers that are constant. Node numbers are
  /// usually different.
  /// </summary>
  /// <exclude/>
  public sealed class EXISchemaConst {
    public const sbyte UNTYPED = -1;
    public const sbyte ANY_TYPE = 0;
    public const sbyte ANY_SIMPLE_TYPE = 1;
    public const sbyte STRING_TYPE = 2;
    public const sbyte BOOLEAN_TYPE = 3;
    public const sbyte DECIMAL_TYPE = 4;
    public const sbyte FLOAT_TYPE = 5;
    public const sbyte DOUBLE_TYPE = 6;
    public const sbyte DURATION_TYPE = 7;
    public const sbyte DATETIME_TYPE = 8;
    public const sbyte TIME_TYPE = 9;
    public const sbyte DATE_TYPE = 10;
    public const sbyte G_YEARMONTH_TYPE = 11;
    public const sbyte G_YEAR_TYPE = 12;
    public const sbyte G_MONTHDAY_TYPE = 13;
    public const sbyte G_DAY_TYPE = 14;
    public const sbyte G_MONTH_TYPE = 15;
    public const sbyte HEXBINARY_TYPE = 16;
    public const sbyte BASE64BINARY_TYPE = 17;
    public const sbyte ANYURI_TYPE = 18;
    public const sbyte QNAME_TYPE = 19;
    public const sbyte NOTATION_TYPE = 20;
    internal const sbyte N_PRIMITIVE_TYPES = 19;
    public const sbyte INTEGER_TYPE = 21;
    public const sbyte N_PRIMITIVE_TYPES_PLUS_INTEGER = 22;

    public const sbyte NON_NEGATIVE_INTEGER_TYPE = 22;
    public const sbyte UNSIGNED_LONG_TYPE = 23;
    public const sbyte POSITIVE_INTEGER_TYPE = 24;
    public const sbyte NON_POSITIVE_INTEGER_TYPE = 25;
    public const sbyte NEGATIVE_INTEGER_TYPE = 26;
    public const sbyte INT_TYPE = 27;
    public const sbyte SHORT_TYPE = 28;
    public const sbyte BYTE_TYPE = 29;
    public const sbyte UNSIGNED_SHORT_TYPE = 30;
    public const sbyte UNSIGNED_BYTE_TYPE = 31;
    public const sbyte LONG_TYPE = 32;
    public const sbyte UNSIGNED_INT_TYPE = 33;

    public const sbyte NORMALIZED_STRING_TYPE = 34;
    public const sbyte TOKEN_TYPE = 35;
    public const sbyte LANGUAGE_TYPE = 36;
    public const sbyte NAME_TYPE = 37;
    public const sbyte NCNAME_TYPE = 38;
    public const sbyte NMTOKEN_TYPE = 39;
    public const sbyte ENTITY_TYPE = 40;
    public const sbyte IDREF_TYPE = 41;
    public const sbyte ID_TYPE = 42;

    public const sbyte ENTITIES_TYPE = 43;
    public const sbyte IDREFS_TYPE = 44;
    public const sbyte NMTOKENS_TYPE = 45;

    public const sbyte N_BUILTIN_TYPES = 46;

    /// <summary>
    /// Initial Entries in "http://www.w3.org/XML/1998/namespace" localName partition.
    /// </summary>
    public static readonly string[] XML_LOCALNAMES = new string[] { "base", "id", "lang", "space" };

    /// <summary>
    /// Initial Entries in "http://www.w3.org/2001/XMLSchema-instance" localName partition.
    /// </summary>
    public static readonly string[] XSI_LOCALNAMES = new string[2];

    public const int XSI_LOCALNAME_NIL_ID = 0;
    public const int XSI_LOCALNAME_TYPE_ID = 1;
    static EXISchemaConst() {
      XSI_LOCALNAMES[XSI_LOCALNAME_NIL_ID] = "nil";
      XSI_LOCALNAMES[XSI_LOCALNAME_TYPE_ID] = "type";
    }

    /// <summary>
    /// Initial Entries in "http://www.w3.org/2001/XMLSchema" localName partition.
    /// </summary>
    public static readonly string[] XSD_LOCALNAMES = { "ENTITIES", "ENTITY", "ID", "IDREF", 
      "IDREFS", "NCName", "NMTOKEN", "NMTOKENS", "NOTATION", "Name", "QName", "anySimpleType", 
      "anyType", "anyURI", "base64Binary", "boolean", "byte", "date", "dateTime", "decimal", 
      "double", "duration", "float", "gDay", "gMonth", "gMonthDay", "gYear", "gYearMonth", 
      "hexBinary", "int", "integer", "language", "long", "negativeInteger", "nonNegativeInteger", 
      "nonPositiveInteger", "normalizedString", "positiveInteger", "short", "string", "time", 
      "token", "unsignedByte", "unsignedInt", "unsignedLong", "unsignedShort" };

    public static readonly int N_BUILTIN_LOCAL_NAMES = 1 + XML_LOCALNAMES.Length + XSI_LOCALNAMES.Length + XSD_LOCALNAMES.Length;

  }

}