package org.openexi.schema;

public final class EXISchemaLayout {

  ///////////////////////////////////////////////////////////////////////////
  // Corpus (corpus is not a node, thus its index start from 0)
  ///////////////////////////////////////////////////////////////////////////

  public static final int SZ_CORPUS        = 1;

  ///////////////////////////////////////////////////////////////////////////
  // Common ancestor
  ///////////////////////////////////////////////////////////////////////////

  public static final int NODE_NODE_TYPE   = 0;

  ///////////////////////////////////////////////////////////////////////////
  // Schema
  ///////////////////////////////////////////////////////////////////////////

  public static final int SCHEMA_N_ELEMS      = 1;
  public static final int SCHEMA_N_ATTRS      = 2;
  public static final int SCHEMA_N_NAMESPACES = 3;
  public static final int SZ_SCHEMA           = 4;

  // NAMESPACE_NODE
  public static final int NAMESPACE_NAME    = 1;
  public static final int NAMESPACE_NUMBER  = 2;
  public static final int NAMESPACE_N_ELEMS = 3; // the number of global elements available in the namespace
  public static final int NAMESPACE_N_ATTRS = 4;
  public static final int NAMESPACE_N_TYPES = 5;
  public static final int SZ_NAMESPACE      = 6;

  // INODE (common to ELEMENT_NODE and ATTRIBUTE_NODE)
  public static final int INODE_NAME                = 1;
  public static final int INODE_TARGET_NAMESPACE    = 2;
  public static final int INODE_TYPE                = 3;
  public static final int INODE_BOOLEANS            = 4;
  public static final int INODE_ISGLOBAL_MASK               = 0x0001;
  public static final int INODE_ISSPECIFIC_IN_FRAGMENT_MASK = 0x0002;
  public static final int INODE_INODE               = 5;
  private static final int SZ_INODE          = 6;

  // ELEMENT_NODE (common part)
  /**
   * The top 3 bits are used for representing element content.
   * Note that simple type becomes SIMPLE_CONTENT.
   */
  public static final int ELEMENT_CONTENT_CLASS_OFFSET = 29;
  /** 0x0001 and 0x0002 are preoccupied by INODE */
  public static final int ELEMENT_ISTYPABLE_MASK       = 0x0004;
  public static final int ELEMENT_ISNILLABLE_MASK      = 0x0008;
  public static final int ELEMENT_ISSIMPLETYPE_MASK    = 0x0010;
  public static final int ELEM_NUMBER             = SZ_INODE;
  public static final int ELEM_CONSTRAINT         = SZ_INODE + 1;
  public static final int ELEM_CONSTRAINT_VALUE   = SZ_INODE + 2;
  public static final int ELEM_SUBST              = SZ_INODE + 3;
  public static final int ELEM_N_SUBSTITUTABLES   = SZ_INODE + 4;
  public static final int ELEM_SUBSTITUTABLES     = SZ_INODE + 5;
  /* The rest are duplicated for run-time efficiency. */
  public static final int ELEM_N_ATTRIBUTE_USES   = SZ_INODE + 6;
  public static final int ELEM_ATTRIBUTE_USES     = SZ_INODE + 7;
  public static final int ELEM_ATTRIBUTE_WC       = SZ_INODE + 8;
  public static final int ELEM_SIMPLE_TYPE        = SZ_INODE + 9; // relevant when SIMPLE_CONTENT
  public static final int ELEM_GROUP              = SZ_INODE + 10; // relevant when ELEMENT_ONLY or MIXED
  public static final int ELEM_GROUP_MINOCCURS    = SZ_INODE + 11;
  public static final int ELEM_GROUP_MAXOCCURS    = SZ_INODE + 12;
  public static final int ELEM_GROUP_N_INITIALS   = SZ_INODE + 13;
  public static final int ELEM_GROUP_INITIALS     = SZ_INODE + 14;
  public static final int SZ_ELEM                 = SZ_INODE + 15;

  // TYPE (common part)
  public static final int TYPE_NAME             = 1;
  public static final int TYPE_TARGET_NAMESPACE = 2;
  public static final int TYPE_NUMBER           = 3;
  public static final int SZ_TYPE               = 4;

  ///////////////////////////////////////////////////////////////////////////
  // SIMPLE_TYPE_NODE
  ///////////////////////////////////////////////////////////////////////////
  
  // ancestryId (5 bits) for atomic simple type; 
  // unused for urtype, list and union
  public static final int SIMPLE_TYPE_AUX                  = 0;
  // 2 bits representing urtype, atomic, list or union
  private static final int SIMPLE_TYPE_VARIETY_OFFSET = 0;
  static final int SIMPLE_TYPE_VARIETY_MASK    = 0x0003 << SIMPLE_TYPE_VARIETY_OFFSET; 
  private static final int SIMPLE_TYPE_VARIETY_WIDTH = 2;
  // 1 bit indicating the presence of enumerated values
  private static final int SIMPLE_TYPE_HAS_ENUMERATED_VALUES_OFFSET = SIMPLE_TYPE_VARIETY_OFFSET + SIMPLE_TYPE_VARIETY_WIDTH;
  public static final int SIMPLE_TYPE_HAS_ENUMERATED_VALUES_MASK = 0x01 << SIMPLE_TYPE_HAS_ENUMERATED_VALUES_OFFSET;
  private static final int SIMPLE_TYPE_HAS_ENUMERATED_VALUES_WIDTH = 1;
  // 5 bits representing ancestryId for atomic simple type
  public static final int SIMPLE_TYPE_ATOMIC_ANCESTRYID_OFFSET = SIMPLE_TYPE_HAS_ENUMERATED_VALUES_OFFSET + SIMPLE_TYPE_HAS_ENUMERATED_VALUES_WIDTH;
  static final int SIMPLE_TYPE_ATOMIC_ANCESTRYID_MASK = 0x1F << SIMPLE_TYPE_ATOMIC_ANCESTRYID_OFFSET;
  private static final int SIMPLE_TYPE_ATOMIC_ANCESTRYID_WIDTH = 5;
  // 2 bits representing whiteSpace for atomic simple types derived from string
  public static final int SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET = SIMPLE_TYPE_ATOMIC_ANCESTRYID_OFFSET + SIMPLE_TYPE_ATOMIC_ANCESTRYID_WIDTH;
  static final int SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_MASK = 0x03 << SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET;
  private static final int SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_WIDTH = 2;
  // 8 bits representing the number of characters in restricted charset
  public static final int SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET = SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET + SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_WIDTH;
  static final int SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_MASK = 0xFF << SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET;
  /* private static final int SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_WIDTH = 8; */
  // 1 bits representing whiteSpace for atomic simple types derived from string
  private static final int SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_OFFSET = SIMPLE_TYPE_ATOMIC_ANCESTRYID_OFFSET + SIMPLE_TYPE_ATOMIC_ANCESTRYID_WIDTH;
  public static final int SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_MASK = 0x01 << SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_OFFSET;
  /* private static final int SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_WIDTH = 1; */
  // 8 bits indicating the representation of integers
  public static final int SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET = SIMPLE_TYPE_ATOMIC_ANCESTRYID_OFFSET + SIMPLE_TYPE_ATOMIC_ANCESTRYID_WIDTH;
  static final int SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_MASK = 0xFF << SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET;

  // base type for atomic simple type; item type for list;
  // the number of member types for union
  public static final int SIMPLE_TYPE_FIELD_INT            = 1;
  public static final int SIMPLE_TYPE_FACET_MININCLUSIVE   = 2; // variant
  public static final int SIMPLE_TYPE_NEXT_SIMPLE_TYPE     = 3;
  public static final int SZ_SIMPLE_TYPE                   = 4;

  // COMPLEX_TYPE_NODE
  public static final int COMPLEX_TYPE_CONTENT_CLASS       = 0;
  public static final int COMPLEX_TYPE_CONTENT_TYPE        = 1;
  public static final int COMPLEX_TYPE_BOOLEANS            = 2;
  public static final int COMPLEX_TYPE_HASPARTICLE_MASK    = 0x0001;
  public static final int COMPLEX_TYPE_N_ATTRIBUTE_USES    = 3;
  public static final int COMPLEX_TYPE_ATTRIBUTE_USES      = 4;
  public static final int COMPLEX_TYPE_ATTRIBUTE_WC        = 5;
  public static final int COMPLEX_TYPE_N_PARTICLES         = 6; 
  public static final int COMPLEX_TYPE_N_INITIALS          = 7; // number of *initial* particles amongst all the particles
  public static final int COMPLEX_TYPE_PARTICLES           = 8; 
  public static final int SZ_COMPLEX_TYPE                  = 9;
  // particle follows if HASPARTICLE is true (i.e. mixed or element-only)

  // PARTICLE_NODE
  public static final int PARTICLE_MINOCCURS               = 1;
  public static final int PARTICLE_MAXOCCURS               = 2;
  public static final int PARTICLE_TERM_TYPE               = 3;
  public static final int PARTICLE_TERM                    = 4;
  public static final int PARTICLE_BOOLEANS                = 5;
  public static final int PARTICLE_ISFIXTURE_MASK          = 0x0001;
  public static final int PARTICLE_N_INITIALS              = 6;
  public static final int PARTICLE_INITIALS                = 7;
  public static final int PARTICLE_N_SUBSTANCES            = 8;
  public static final int PARTICLE_SUBSTANCES              = 9;
  public static final int PARTICLE_SERIAL_INTYPE           = 10;
  public static final int SZ_PARTICLE                      = 11;

  // GROUP_NODE
  public static final int GROUP_COMPOSITOR                 = 1;
  public static final int GROUP_BOOLEANS                   = 2;
  public static final int GROUP_ISFIXTURE_MASK             = 0x0001;
  public static final int GROUP_N_PARTICLES                = 3;
  public static final int GROUP_N_HEAD_SUBSTANCE_NODES     = 4;
  public static final int GROUP_HEAD_SUBSTANCE_NODES       = 5;
  public static final int GROUP_N_MEMBER_SUBSTANCE_NODES   = 6;
  public static final int GROUP_MEMBER_SUBSTANCE_NODES     = 7;
  public static final int GROUP_NUMBER                     = 8;
  public static final int SZ_GROUP                         = 9;

  // WILDCARD_NODE
  public static final int WILDCARD_CONSTRAINT_TYPE         = 1;
  public static final int WILDCARD_PROCESS_CONTENTS        = 2;
  public static final int WILDCARD_N_NAMESPACES            = 3;
  public static final int SZ_WILDCARD                      = 4;

  // ATTRIBUTE_USE_NODE
  public static final int ATTR_USE_ATTR_NAME               = 1;
  public static final int ATTR_USE_ATTR_TARGET_NAMESPACE   = 2;
  public static final int ATTR_USE_ATTRIBUTE               = 3;
  public static final int ATTR_USE_CONSTRAINT              = 4;
  public static final int ATTR_USE_CONSTRAINT_VALUE        = 5;
  public static final int ATTR_USE_BOOLEANS                = 6;
  public static final int ATTR_USE_ISREQUIRED_MASK         = 0x0001;
  public static final int SZ_ATTR_USE                      = 7;

  // ATTRIBUTE_NODE
  public static final int ATTR_CONSTRAINT                  = SZ_INODE;
  public static final int ATTR_CONSTRAINT_VALUE            = SZ_INODE + 1;
  public static final int SZ_ATTR                          = SZ_INODE + 2;

  // OPAQUE_NODE
  public static final int OPAQUE_SIZE                      = 1;
  public static final int SZ_OPAQUE                        = 2;

  private EXISchemaLayout() { // so that no one can instantiate it.
  }
}
