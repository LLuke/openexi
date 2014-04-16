using System.Text;

namespace Nagasena.Scomp {

  public class EXISchemaFactoryTestUtilContext {

    internal EXISchemaFactoryTestUtilContext() {
      this.stringBuilder = new StringBuilder();
      this.schemaReader = null;
    }

    internal EXISchemaFactoryTestUtilContext(StringBuilder stringBuilder) {
      this.stringBuilder = stringBuilder;
      this.schemaReader = null;
    }

    internal EXISchemaFactoryTestUtilContext(StringBuilder stringBuilder, EXISchemaReader schemaReader) {
      this.stringBuilder = stringBuilder;
      this.schemaReader = schemaReader;
    }

    internal readonly StringBuilder stringBuilder;
    internal readonly EXISchemaReader schemaReader;

  }

}