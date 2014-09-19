package org.peg4d;

class ParsingRule {
	Grammar  peg;
	String ruleName;

	ParsingObject po;
	ParsingType type;
	ParsingExpression expr;
	
	int minlen = -1;

	ParsingRule(Grammar peg, String ruleName, ParsingObject po, ParsingExpression e) {
		this.peg = peg;
		this.po = po;
		this.ruleName = ruleName;
		this.expr = e;
	}
	
	final String getUniqueName() {
		return this.peg.uniqueRuleName(ruleName);
	}
	
	@Override
	public String toString() {
		return type + " " + this.ruleName + "[" + this.minlen + "]" + "=" + this.expr;
	}
	
	final void report(ReportLevel level, String msg) {
		if(this.po != null) {
			Main._PrintLine(po.formatSourceMessage(level.toString(), msg));
		}
		else {
			System.out.println("" + level.toString() + ": " + msg);
		}
	}
	
	Grammar getGrammar() {
		return this.peg;
	}
	
	class PegRuleAnnotation {
		String key;
		ParsingObject value;
		PegRuleAnnotation next;
		PegRuleAnnotation(String key, ParsingObject value, PegRuleAnnotation next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}
	}

	PegRuleAnnotation annotation;
	
	public void addAnotation(String key, ParsingObject value) {
		this.annotation = new PegRuleAnnotation(key,value, this.annotation);
	}
	
	public final void testExample1(Grammar peg, ParsingContext context) {
		PegRuleAnnotation a = this.annotation;
		while(a != null) {
			boolean isExample = a.key.equals("example");
			boolean isBadExample = a.key.equals("bad-example");
			if(isExample || isBadExample) {
				boolean ok = true;
				ParsingSource s = ParsingObjectUtils.newStringSource(a.value);
				context.resetSource(s, 0);
				context.parse(peg, this.ruleName);
//				System.out.println("@@ " + context.isFailure() + " " + context.hasByteChar() + " " + isExample + " " + isBadExample);
				if(context.isFailure() || context.hasByteChar()) {
					if(isExample) ok = false;
				}
				else {
					if(isBadExample) ok = false;
				}
				String msg = ( ok ? "[PASS]" : "[FAIL]" ) + " " + this.ruleName + " " + a.value.getText();
				if(Main.TestMode && !ok) {	
					Main._Exit(1, "[FAIL] tested " + a.value.getText() + " by " + peg.getRule(this.ruleName));
				}
				Main.printVerbose("Testing", msg);
			}
			a = a.next;
		}
	}
	
	boolean isObjectType() {
		return this.type.isObjectType();
	}

	void typeCheck() {
		boolean firstUpperCase = Character.isUpperCase(this.ruleName.charAt(0));
		boolean containUpperCase = false;
		boolean containLowerCase = false;
		for(int i = 1; i < this.ruleName.length(); i++) {
			if(Character.isUpperCase(this.ruleName.charAt(i))) {
				containUpperCase = true;
			}
			if(Character.isLowerCase(this.ruleName.charAt(i))) {
				containLowerCase = true;
			}
		}
		if(firstUpperCase) {
			if(containLowerCase) { // CamelStyle
				if(!this.type.isObjectType()) {
					this.report(ReportLevel.warning, this.ruleName + " must be a production rule");
				}
			}
			else {
				if(!this.type.isEmpty()) {
					this.report(ReportLevel.warning, this.ruleName + " must be a lexical rule");
				}
			}
		}
		else {
			if(containUpperCase) {
				if(this.type.isObjectType() || !this.type.isEmpty()) {
					this.report(ReportLevel.warning, this.ruleName + " must be an operation rule");
				}
			}
			else {
				
			}
		}
		
	}
}