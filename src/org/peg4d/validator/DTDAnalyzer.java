package org.peg4d.validator;

import org.peg4d.ParsingObject;

public class DTDAnalyzer {
	ParsingObject node;
	int elementCount;
	int attlistCount;
	int entityCount;
	int maxAttribute;
	int entityMemberCount;
	int maxEnumMeber;
	int maxRequiredCount;

	public DTDAnalyzer(ParsingObject node) {
		this.node = node;
		this.elementCount = 0;
		this.attlistCount = 0;
		this.entityCount = 0;
		this.maxAttribute = 0;
		this.maxEnumMeber = 0;
		this.maxRequiredCount = 0;
	}

	public void analyze() {
		countTags(this.node);
	}

	private final void countTags(ParsingObject node) {
		for (ParsingObject subnode : node) {
			String tag = subnode.getTag().toString();
			if (tag.equals("attlist")) {
				this.attlistCount++;
				if (this.maxAttribute < (subnode.size() - 1)) {
					this.maxAttribute = subnode.size() - 1;
				}
				countAttMembers(subnode);
			}
			if (tag.equals("element")) {
				this.elementCount++;
			}
			if (tag.equals("entity")) {
				this.entityCount++;
			}
		}
	}

	private void countAttMembers(ParsingObject node) {
		int enumMemberCount = 0;
		int requiredCount = 0;
		for (ParsingObject subnode : node) {
			if (subnode.getTag().toString().equals("attDef")) {
				String type = subnode.get(1).get(0).getTag().toString();
				String constraint = subnode.get(2).get(0).getTag().toString();
				if (constraint.equals("REQUIRED")) {
					requiredCount++;
				}
				if (type.equals("enum")) {
					ParsingObject enumNode = subnode.get(1).get(0);
					for (ParsingObject memberNode : enumNode) {
						if (memberNode.getTag().toString().equals("enumMember"))
							enumMemberCount++;
					}
				}
				if (enumMemberCount > this.maxEnumMeber) {
					this.maxEnumMeber = enumMemberCount;
					enumMemberCount = 0;
				}
			}
		}
		if (requiredCount > maxRequiredCount) {
			maxRequiredCount = requiredCount;
		}

	}
}
