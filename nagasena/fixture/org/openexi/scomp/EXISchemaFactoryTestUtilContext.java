package org.openexi.scomp;

public class EXISchemaFactoryTestUtilContext {
  
  public EXISchemaFactoryTestUtilContext(EXISchemaFactoryErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
    this.entityResolver = null;
    this.stringBuilder = new StringBuilder();
    this.schemaReader = null;
  }
  
  EXISchemaFactoryTestUtilContext(EXISchemaFactoryErrorHandler errorHandler, EntityResolverEx entityResolver) {
    this.errorHandler = errorHandler;
    this.entityResolver = entityResolver;
    this.stringBuilder = new StringBuilder();
    this.schemaReader = null;
  }
  
  public EXISchemaFactoryTestUtilContext(EXISchemaFactoryErrorHandler errorHandler, StringBuilder stringBuilder) {
    this.errorHandler = errorHandler;
    this.entityResolver = null;
    this.stringBuilder = stringBuilder;
    this.schemaReader = null;
  }
  
  EXISchemaFactoryTestUtilContext(EXISchemaFactoryErrorHandler errorHandler, StringBuilder stringBuilder, EXISchemaReader schemaReader) {
    this.errorHandler = errorHandler;
    this.entityResolver = null;
    this.stringBuilder = stringBuilder;
    this.schemaReader = schemaReader;
  }
  
  final EXISchemaFactoryErrorHandler errorHandler;
  final EntityResolverEx entityResolver;
  final StringBuilder stringBuilder;
  final EXISchemaReader schemaReader;
  
}
