namespace Nagasena.Schema {

  /// <exclude/>
  public sealed class EXISchemaLayout {

    ///////////////////////////////////////////////////////////////////////////
    // INODE (common to ELEMENT_NODE and ATTRIBUTE_NODE)
    ///////////////////////////////////////////////////////////////////////////

    public const int INODE_NAME = 0;
    public const int INODE_URI = 1;
    public const int INODE_TYPE = 2;
    private const int SZ_INODE = 3;

    ///////////////////////////////////////////////////////////////////////////
    // ELEMENT_NODE, ATTRIBUTE_NODE
    ///////////////////////////////////////////////////////////////////////////

    public const int ELEM_NILLABLE = SZ_INODE;
    public static readonly int SZ_ELEM = SZ_INODE + 1;

    public const int SZ_ATTR = SZ_INODE;

    ///////////////////////////////////////////////////////////////////////////
    // TYPE (common to SIMPLE_TYPE_NODE and COMPLEX_TYPE_NODE)
    ///////////////////////////////////////////////////////////////////////////

    public const int TYPE_NAME = 0;
    public const int TYPE_URI = 1;

    // TYPE (common part)
    public const int TYPE_NUMBER = 2;
    public const int TYPE_TYPABLE = 3;
    public const int TYPE_GRAMMAR = 4;
    // Content datatype for complex type. Structure for simple type is described below.
    public const int TYPE_AUX = 5;
    public const int SZ_TYPE = 6;
    public const int SZ_COMPLEX_TYPE = SZ_TYPE;

    // TYPE_AUX structure (common to simple type and complex type)
    private const int TYPE_TYPE_OFFSET = 31;
    public static readonly int TYPE_TYPE_OFFSET_MASK = 0x01 << TYPE_TYPE_OFFSET;

    ///////////////////////////////////////////////////////////////////////////
    // SIMPLE_TYPE_NODE
    ///////////////////////////////////////////////////////////////////////////

    // 2 bits representing urtype, atomic, list or union
    private const int SIMPLE_TYPE_VARIETY_OFFSET = 0;
    internal static readonly int SIMPLE_TYPE_VARIETY_MASK = 0x0003 << SIMPLE_TYPE_VARIETY_OFFSET;
    private const int SIMPLE_TYPE_VARIETY_WIDTH = 2;
    // 1 bit indicating the presence of enumerated values
    private static readonly int SIMPLE_TYPE_HAS_ENUMERATED_VALUES_OFFSET = SIMPLE_TYPE_VARIETY_OFFSET + SIMPLE_TYPE_VARIETY_WIDTH;
    public static readonly int SIMPLE_TYPE_HAS_ENUMERATED_VALUES_MASK = 0x01 << SIMPLE_TYPE_HAS_ENUMERATED_VALUES_OFFSET;
    private const int SIMPLE_TYPE_HAS_ENUMERATED_VALUES_WIDTH = 1;
    // 2 bits representing whiteSpace for atomic simple types derived from string
    public static readonly int SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET = SIMPLE_TYPE_HAS_ENUMERATED_VALUES_OFFSET + SIMPLE_TYPE_HAS_ENUMERATED_VALUES_WIDTH;
    internal static readonly int SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_MASK = 0x03 << SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET;
    private const int SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_WIDTH = 2;
    // 8 bits representing the number of characters in restricted charset
    public static readonly int SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET = SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET + SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_WIDTH;
    internal static readonly int SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_MASK = 0xFF << SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET;
    /* private static final int SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_WIDTH = 8; */
    // 1 bits representing whiteSpace for atomic simple types derived from string
    private static readonly int SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_OFFSET = SIMPLE_TYPE_HAS_ENUMERATED_VALUES_OFFSET + SIMPLE_TYPE_HAS_ENUMERATED_VALUES_WIDTH;
    public static readonly int SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_MASK = 0x01 << SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_OFFSET;
    /* private static final int SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_WIDTH = 1; */
    // 8 bits indicating the representation of integers
    public static readonly int SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET = SIMPLE_TYPE_HAS_ENUMERATED_VALUES_OFFSET + SIMPLE_TYPE_HAS_ENUMERATED_VALUES_WIDTH;
    internal static readonly int SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_MASK = 0xFF << SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET;

    public const int SIMPLE_TYPE_BASE_TYPE = 0;
    public const int SIMPLE_TYPE_ITEM_TYPE = 1; // item type for list; (unused for atomic or union simple type)
    public const int SIMPLE_TYPE_FACET_MININCLUSIVE = 1; // variant used only for integral type with n-bit Unsigned Integer representation.
    public static readonly int SZ_SIMPLE_TYPE = 2 + SZ_TYPE;

    // GRAMMAR
    public const int GRAMMAR_NUMBER = 0; // uniquely identifies a grammar in schema
    public const int GRAMMAR_N_PRODUCTION = 1;
    public const int GRAMMAR_N_PRODUCTION_MASK = 0x0000FFFF;
    public const int GRAMMAR_HAS_END_ELEMENT_MASK = 0x00010000;
    public const int GRAMMAR_HAS_CONTENT_GRAMMAR_MASK = 0x00020000;
    public const int GRAMMAR_HAS_EMPTY_GRAMMAR_MASK = 0x00040000;
    public const int SZ_GRAMMAR = 2;

    public const int GRAMMAR_EXT_CONTENT_GRAMMAR = 0;
    public const int GRAMMAR_EXT_EMPTY_GRAMMAR = 1;

    // PRODUCTION
    public const int PRODUCTION_EVENT = 0;
    public const int PRODUCTION_GRAMMAR = 1;
    public const int SZ_PRODUCTION = 2;

    private EXISchemaLayout() { // so that no one can instantiate it.
    }
  }

}