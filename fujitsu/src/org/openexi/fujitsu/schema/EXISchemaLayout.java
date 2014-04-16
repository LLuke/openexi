package org.openexi.fujitsu.schema;

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
  public static final int NAMESPACE_NAME                 = 1;
  public static final int NAMESPACE_NUMBER               = 2;
  public static final int NAMESPACE_N_ELEMS              = 3;
  public static final int NAMESPACE_N_ATTRS              = 4;
  public static final int NAMESPACE_N_TYPES              = 5;
  public static final int SZ_NAMESPACE                   = 6;

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
  public static final int ELEMENT_ISNILLABLE_MASK      = 0x0008;
  public static final int ELEMENT_ISABSTRACT_MASK      = 0x0010;
  public static final int ELEMENT_ISSIMPLETYPE_MASK = 0x0020;
  public static final int ELEMENT_ISURTYPE_MASK        = 0x0040;
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
  public static final int TYPE_BASE_TYPE        = 4;
  public static final int TYPE_BOOLEANS         = 5;
  public static final int TYPE_ISURTYPE_MASK    = 0x0001;
  public static final int TYPE_ISFIXTURE_MASK   = 0x0002;
  public static final int TYPE_HASSUBTYPE_MASK  = 0x0004;
  public static final int SZ_TYPE               = 6;

  // SIMPLE_TYPE_NODE
  /**
   * The top 8 bits indicate the representation of integral simple types.
   */
  public static final int INTEGRAL_SIMPLE_TYPE_WIDTH_OFFSET = 24;
  public static final int SIMPLE_TYPE_VARIETY              = 0; // atomic, list or union
  public static final int SIMPLE_TYPE_AUX_TYPE             = 1;
  public static final int SIMPLE_TYPE_BOOLEANS             = 2;
  public static final int SIMPLE_TYPE_ISBUILTIN_MASK       = 0x00000001;
  public static final int SIMPLE_TYPE_ISPRIMITIVE_MASK     = 0x00000002;
  // SIMPLE_TYPE_ISINTEGRAL_MASK affects how 0xFF000000 is used.
  // 0xFF amd 0xFE each represents a plain integer encoding and
  // non-negative integer encoding. Other patterns represents n-bit 
  // width.
  public static final int SIMPLE_TYPE_ISINTEGRAL_MASK      = 0x00000004;
  public static final int SIMPLE_TYPE_ISLIST_CONTENT_MASK  = 0x00000008;
  public static final int SIMPLE_TYPE_FACET_LENGTH         = 3; // int
  public static final int SIMPLE_TYPE_FACET_MINLENGTH      = 4; // int
  public static final int SIMPLE_TYPE_FACET_MAXLENGTH      = 5; // int
  public static final int SIMPLE_TYPE_FACET_WHITESPACE     = 6; // int
  public static final int SIMPLE_TYPE_FACET_MAXINCLUSIVE   = 7; // variant
  public static final int SIMPLE_TYPE_FACET_MAXEXCLUSIVE   = 8; // variant
  public static final int SIMPLE_TYPE_FACET_MINEXCLUSIVE   = 9; // variant
  public static final int SIMPLE_TYPE_FACET_MININCLUSIVE   = 10; // variant
  public static final int SIMPLE_TYPE_FACET_TOTALDIGITS    = 11; // int
  public static final int SIMPLE_TYPE_FACET_FRACTIONDIGITS = 12; // int
  public static final int SIMPLE_TYPE_N_RESTRICTED_CHARSET = 13; // int
  public static final int SIMPLE_TYPE_N_FACET_ENUMERATIONS = 14; // int
  public static final int SIMPLE_TYPE_N_MEMBER_TYPES       = 15;
  public static final int SIMPLE_TYPE_NEXT_SIMPLE_TYPE     = 16;
  public static final int SZ_SIMPLE_TYPE                   = 17;

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
