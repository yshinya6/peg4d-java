package org.peg4d.pegcode;

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
	
	@Override
	public void formatHeader(StringBuilder sb) {
		this.sb = sb;
		writeLine("struct ParsingObject");
		openIndent();
		writeLine("long  oid;");
		writeLine("const char *tag;");
		writeLine("long start_pos;");
		writeLine("long end_pos;");
		writeLine("const char *value;");
		writeLine("struct ParsingObject *parent;");
		writeLine("struct ParsingObject **child;");
		writeLine("int child_size;");
		closeIndent();
		sb.append(";");
		writeLine("struct ParsingContext ");
		openIndent();
		writeLine("unsigned char *inputs;");
		writeLine("long input_size;");
		writeLine("long pos;");
		writeLine("struct ParsingObject *left;");
		writeLine("int tstack_id;");
		closeIndent();
		sb.append(";");
		writeLine("void Pcomit(struct ParsingContext *c, int tid);");
		writeLine("void Pabort(struct ParsingContext *c, int tid);");
		writeLine("void Pconnect(struct ParsingContext *c, struct ParsingObject *parent);");
		writeLine("struct ParsingObject* Pnew(struct ParsingContext *c, long pos);");
		writeLine("void PleftJoin(struct ParsingContext *c, struct ParsingObject *parent);");
		writeLine("void Pdispose(struct ParsingContext *c, int tid, struct ParsingObject *newone);");
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

	String funcName(String symbol) {
		return "p" + symbol;
	}
	
	@Override
	public void visitRule(ParsingRule e) {
		this.initFailureJumpPoint();
		writeLine("int " + funcName(e.ruleName) + "(struct ParsingContext *c)");
		openIndent();
		writeLine("long pos = c->pos;");

		this.pushFailureJumpPoint();
		e.expr.visit(this);
		writeLine("c->pos = pos;");
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
		writeLine("pos++;");
	}

	@Override
	public void visitByteRange(ParsingByteRange e) {
		writeLine("if(c->inputs[pos] < " + e.startByteChar + " && c->inputs[pos] > " + e.endByteChar + ")");
		openIndent();
		jumpFailureJump();
		closeIndent();
		writeLine("pos++;");
	}

	@Override
	public void visitAny(ParsingAny e) {
		writeLine("if(c->inputs[pos] == 0)");
		openIndent();
		jumpFailureJump();
		closeIndent();
		writeLine("pos++;");
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
		writeLine("long " + posName + " = pos;");
		e.inner.visit(this);
		writeLine("pos = " + posName + ";");
		this.jumpPrevFailureJump();
		this.popFailureJumpPoint(e);
		writeLine("pos = " + posName + ";");
		this.closeIndent();
	}

	@Override
	public void visitAnd(ParsingAnd e) {
		this.openIndent();
		String posName = "pos" + this.fID;
		writeLine("long " + posName + " = pos;");
		e.inner.visit(this);
		writeLine("pos = " + posName + ";");
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
		openIndent();
		writeLine("long pos" + fid + " = pos;");
		for(int i = 0; i < e.size() - 1; i++) {
			this.pushFailureJumpPoint();
			e.get(i).visit(this);
			this.writeLine("goto " + labelName + ";");
			this.popFailureJumpPoint(e.get(i));
			writeLine("pos = pos" + fid + ";");
		}
		e.get(e.size() - 1).visit(this);
		closeIndent();
		this.writeLine(labelName + ": ;;");
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
		writeLine("struct ParsingObject *"+leftName+" = c->left;");
		writeLine("int tid = c->tstack_id;");
		writeLine("c->left = Pnew(c, pos);");
		if(e.leftJoin) {
			writeLine("PleftJoin(c, c->left, " + leftName +");");
		}
		for(int i = e.prefetchIndex; i < e.size(); i++) {
			e.get(i).visit(this);
		}
		writeLine("PsetLength(c, pos);");
		writeLine("goto " + labelName + ";");

		this.popFailureJumpPoint(e);
		writeLine("Pdispose(c, tid, c->left);");
		writeLine("c->left = " + leftName + ";");
		this.jumpFailureJump();
		closeIndent();
		writeLine(labelName + ": ;;");
	}

	@Override
	public void visitConnector(ParsingConnector e) {
		this.pushFailureJumpPoint();
		int uid = this.fID;
		String leftName = "left" + uid;
		String labelName = "EXIT_CONNECTOR" + uid;
		openIndent();
		writeLine("struct ParsingObject *"+leftName+" = c->left;");
		writeLine("int tid = c->tstack_id;");
		e.inner.visit(this);
		writeLine("Pcomit(c, tid);");
		writeLine("Pconnect(c, " + leftName + ");");
		writeLine("c->left = " + leftName + ";");
		writeLine("goto " + labelName + ";");
		this.popFailureJumpPoint(e);
		writeLine("Pabort(c, tid);");
		writeLine("c->left = " + leftName + ";");
		this.jumpFailureJump();
		writeLine(labelName + ": ;;");
		closeIndent();
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
