package org.peg4d.pegcode;

import org.peg4d.Grammar;
import org.peg4d.ParsingRule;
import org.peg4d.expression.NonTerminal;
import org.peg4d.expression.ParsingAnd;
import org.peg4d.expression.ParsingAny;
import org.peg4d.expression.ParsingApply;
import org.peg4d.expression.ParsingAssert;
import org.peg4d.expression.ParsingBlock;
import org.peg4d.expression.ParsingByte;
import org.peg4d.expression.ParsingByteRange;
import org.peg4d.expression.ParsingCatch;
import org.peg4d.expression.ParsingChoice;
import org.peg4d.expression.ParsingConnector;
import org.peg4d.expression.ParsingConstructor;
import org.peg4d.expression.ParsingEmpty;
import org.peg4d.expression.ParsingExport;
import org.peg4d.expression.ParsingExpression;
import org.peg4d.expression.ParsingFailure;
import org.peg4d.expression.ParsingIf;
import org.peg4d.expression.ParsingIndent;
import org.peg4d.expression.ParsingIsa;
import org.peg4d.expression.ParsingMatch;
import org.peg4d.expression.ParsingName;
import org.peg4d.expression.ParsingNot;
import org.peg4d.expression.ParsingOption;
import org.peg4d.expression.ParsingRepetition;
import org.peg4d.expression.ParsingSequence;
import org.peg4d.expression.ParsingString;
import org.peg4d.expression.ParsingTagging;
import org.peg4d.expression.ParsingValue;
import org.peg4d.expression.ParsingWithFlag;
import org.peg4d.expression.ParsingWithoutFlag;

public class CGenerator2 extends GrammarFormatter {

	@Override
	public String getDesc() {
		return "c";
	}
	
	String indent = "";

	protected void writeLine(String s) {
		formatString("\n" + indent + s);
	}
	
	protected void openIndent() {
		writeLine("{");
		indent = "  " + indent;
	}
	protected void closeIndent() {
		indent = indent.substring(2);
		writeLine("}");
	}
		
	class FailurePoint {
		int id;
		FailurePoint prev;
		FailurePoint(int id, FailurePoint prev) {
			this.prev = prev;
			this.id = id;
		}
	}

	int fID = 0;
	FailurePoint fLabel = null;
	void initFailureJumpPoint() {
		fID = 0;
		fLabel = null;
	}
	void pushFailureJumpPoint() {
		fLabel = new FailurePoint(fID, fLabel);
		fID += 1;
	}
	void popFailureJumpPoint(ParsingRule r) {
		writeLine("CATCH_FAILURE" + fLabel.id + ":" + "/* " + r.ruleName + " */");
		fLabel = fLabel.prev;
	}
	void popFailureJumpPoint(ParsingExpression e) {
		writeLine("CATCH_FAILURE" + fLabel.id + ":" + "/* " + e + " */");
		fLabel = fLabel.prev;
	}
	void jumpFailureJump() {
		writeLine("goto CATCH_FAILURE" + fLabel.id + ";");
	}
	void jumpPrevFailureJump() {
		writeLine("goto CATCH_FAILURE" + fLabel.prev.id + ";");
	}
	void let(String type, String var, String expr) {
		if(type != null) {
			writeLine(type + " " + var + " = " + expr + ";");		
		}
		else {
			writeLine("" + var + " = " + expr + ";");
		}
	}
	void letO(String type, String var, String expr) {
		if(type != null) {
			writeLine(type + " " + var + " = NULL;");		
			writeLine("P4D_setObject(c, &" + var + ", " + expr + ");");		
		}
		else {
			writeLine("P4D_setObject(c, &" + var + ", " + expr + ");");		
		}
	}
	void gotoLabel(String label) {
		writeLine("goto " + label + ";");
	}
	void exitLabel(String label) {
		writeLine(label + ": ;; /* <- this is required for avoiding empty statement */");
	}

	String funcName(String symbol) {
		return "p" + symbol;
	}
	
	@Override
	public void formatGrammar(Grammar peg, StringBuilder sb) {
		this.formatHeader();
		for(ParsingRule r: peg.getRuleList()) {
			this.writeLine("int " + funcName(r.ruleName) + "(ParsingContext c);");
		}
		for(ParsingRule r: peg.getRuleList()) {
			this.formatRule(r, sb);
		}
		this.formatFooter();
		System.out.println(sb.toString());
	}

	@Override
	public void formatHeader() {
		writeLine("#include \"libpeg4d/parsing.h\"");
	}


	@Override
	public void visitRule(ParsingRule e) {
		this.initFailureJumpPoint();
		writeLine("int " + funcName(e.ruleName) + "(ParsingContext c)");
		openIndent();
		let("long", "pos", "c->pos");

		this.pushFailureJumpPoint();
		e.expr.visit(this);
		let(null, "c->pos", "pos");
		writeLine("return 0;");
		
		this.popFailureJumpPoint(e);
		writeLine("return -1;");
		closeIndent();
		writeLine("");
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		writeLine("if(" + funcName(e.ruleName) + "(c))");
		openIndent();
		jumpFailureJump();
		closeIndent();
	}

	@Override
	public void visitEmpty(ParsingEmpty e) {

	}

	@Override
	public void visitFailure(ParsingFailure e) {
		jumpFailureJump();
	}

	@Override
	public void visitByte(ParsingByte e) {
		writeLine("if(c->inputs[pos] != " + e.byteChar + ")");
		openIndent();
		jumpFailureJump();
		closeIndent();
		writeLine("pos += 1;");
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		writeLine("if(c->inputs[pos] < " + e.startByteChar + " && c->inputs[pos] > " + e.endByteChar + ")");
		openIndent();
		jumpFailureJump();
		closeIndent();
		writeLine("pos += 1;");
	}

	@Override
	public void visitAny(ParsingAny e) {
		writeLine("if(c->inputs[pos] == 0)");
		openIndent();
		jumpFailureJump();
		closeIndent();
		writeLine("pos +=1;");
	}

	@Override
	public void visitString(ParsingString e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitNot(ParsingNot e) {
		this.openIndent();
		this.pushFailureJumpPoint();
		String posName = "pos" + this.fID;
		let("long", posName, "pos");
		e.inner.visit(this);
		let(null, "pos", posName);
		this.jumpPrevFailureJump();
		this.popFailureJumpPoint(e);
		let(null, "pos", posName);
		this.closeIndent();
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		this.openIndent();
		String posName = "pos" + this.fID;
		let("long", posName, "pos");
		e.inner.visit(this);
		let(null, "pos", posName);
		this.closeIndent();
	}

	@Override
	public void visitOptional(ParsingOption e) {
		this.pushFailureJumpPoint();
		e.inner.visit(this);
		this.popFailureJumpPoint(e);
	}

	@Override
	public void visitRepetition(ParsingRepetition e) {
		this.pushFailureJumpPoint();
		writeLine("while(1)");
		this.openIndent();
		e.inner.visit(this);
		this.closeIndent();
		this.popFailureJumpPoint(e);		
	}

	@Override
	public void visitSequence(ParsingSequence e) {
		for(int i = 0; i < e.size(); i++) {
			e.get(i).visit(this);
		}
	}

	@Override
	public void visitChoice(ParsingChoice e) {
		int fid = this.fID;
		String labelName = "EXIT_CHOICE" + fid;
		String posName = "pos" + fid;
		openIndent();
		let("long", posName, "pos");
		for(int i = 0; i < e.size() - 1; i++) {
			this.pushFailureJumpPoint();
			e.get(i).visit(this);
			this.gotoLabel(labelName);
			this.popFailureJumpPoint(e.get(i));
			let(null, "pos", posName);
		}
		e.get(e.size() - 1).visit(this);
		closeIndent();
		this.exitLabel(labelName);
	}

	@Override
	public void visitConstructor(ParsingConstructor e) {
		for(int i = 0; i < e.prefetchIndex; i++) {
			e.get(i).visit(this);
		}
		this.pushFailureJumpPoint();
		String leftName = "left" + this.fID;
		String labelName = "EXIT_NEW" + this.fID;
		openIndent();
		this.letO("ParsingObject", leftName, "c->left");
		this.let("int", "tid", "P4D_markLogStack(c)");
		this.letO(null, "c->left", "P4D_newObject(c, pos)");
		if(e.leftJoin) {
			//context.lazyJoin(context.left);
			//context.lazyLink(newnode, 0, context.left);
			writeLine("P4D_lazyJoin(c, left);");
			writeLine("P4D_lazyLink(c, c->left, 0, left);");
		}
		for(int i = e.prefetchIndex; i < e.size(); i++) {
			e.get(i).visit(this);
		}
		writeLine("c->left->end_pos = pos;");
		this.letO(null, leftName, "NULL");
		this.gotoLabel(labelName);

		this.popFailureJumpPoint(e);
		this.letO(null, "c->left", leftName);
		this.letO(null, leftName, "NULL");
		this.jumpFailureJump();
		closeIndent();
		this.exitLabel(labelName);
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		this.pushFailureJumpPoint();
		int uid = this.fID;
		String leftName = "left" + uid;
		String labelName = "EXIT_CONNECTOR" + uid;
		String markName = "mark" + uid;
		openIndent();
		this.letO("ParsingObject", leftName, "c->left");
		this.let("int", markName, "P4D_markLogStack(c);");
		e.inner.visit(this);
		writeLine("P4D_commitLog(c, " + markName + ", c->left);");
		writeLine("P4D_lazyLink(c, " + leftName + "," + e.index + " , c->left);");
		this.letO(null, "c->left", leftName);
		this.letO(null, leftName, "NULL");
		this.gotoLabel(labelName);

		this.popFailureJumpPoint(e);
		writeLine("P4D_abortLog(c, " + markName + ");");
		this.letO(null, "c->left", leftName);
		this.letO(null, leftName, "NULL");
		this.jumpFailureJump();
		closeIndent();
		this.exitLabel(labelName);
	}

	@Override
	public void visitTagging(ParsingTagging e) {
		writeLine("c->left->tag = \"#" + e.tag + "\";");
	}

	@Override
	public void visitValue(ParsingValue e) {
		writeLine("c->left->value = \"" + e.value + "\";");
	}

	@Override
	public void visitExport(ParsingExport e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitMatch(ParsingMatch e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitCatch(ParsingCatch e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitAssert(ParsingAssert e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIfFlag(ParsingIf e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitWithFlag(ParsingWithFlag e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitWithoutFlag(ParsingWithoutFlag e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitBlock(ParsingBlock e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIndent(ParsingIndent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitName(ParsingName e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitIsa(ParsingIsa e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visitApply(ParsingApply e) {
		// TODO Auto-generated method stub
		
	}

}