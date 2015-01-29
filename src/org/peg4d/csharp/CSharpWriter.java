package org.peg4d.csharp;

import org.peg4d.ParsingObject;
import org.peg4d.million.SourceGenerator;
import org.peg4d.writer.ParsingWriter;

public class CSharpWriter extends ParsingWriter {
	static {
		ParsingWriter.registerExtension("csharp", CSharpWriter.class);
	}
	
	@Override
	protected void write(ParsingObject po) {
		SourceGenerator generator = new CSharpGenerator();
		generator.visit(po);
		String str = generator.toString();
		char last = str.charAt(str.length()-1);
		if(last != ';' && last != '}') {
			str = str + ";";
		}
		this.out.println(str);
		

	}
	
	public String generate(ParsingObject po) {
		SourceGenerator generator = new CSharpGenerator();
		generator.visit(po);
		String str = generator.toString();
		char last = str.charAt(str.length()-1);
		if(last != ';' && last != '}') {
			str = str + ";";
		}
		return str;
		

	}
}
