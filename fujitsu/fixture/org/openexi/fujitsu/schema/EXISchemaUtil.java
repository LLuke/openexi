package org.openexi.fujitsu.schema;

public class EXISchemaUtil {

  /**
   * Count the number of types in a schema.
   */
  public static int countTypesOfSchema(EXISchema schema, boolean globalOnly) {
    int n_globalTypes, n_types;
    n_globalTypes = n_types = 0;
    int pos, nodesLen;
    for (pos = EXISchema.THE_SCHEMA, nodesLen = schema.getNodes().length; pos < nodesLen;) {
      final int node = pos;
      pos += EXISchema._getNodeSize(node, schema.getNodes());
      final int nodeType = schema.getNodeType(node);
      if (nodeType == EXISchema.SIMPLE_TYPE_NODE || nodeType == EXISchema.COMPLEX_TYPE_NODE) {
        ++n_types;
        if (schema.getNameOfType(node) != "") {
          ++n_globalTypes;
        }
      }
    }
    return globalOnly ? n_globalTypes : n_types;
  }

  /**
   * Count the number of simple types in a schema.
   */
  public static int countLinkedSimpleTypesOfSchema(EXISchema schema) {
    int tp = schema.getBuiltinTypeOfSchema(EXISchemaConst.ANY_SIMPLE_TYPE);
    int n_simpleTypes = 0;
    do {
      ++n_simpleTypes;
      tp = schema.getNextSimpleType(tp);
    }
    while (tp != EXISchema.NIL_NODE);
    return n_simpleTypes;
  }

}
