package org.peg4d.validator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.peg4d.ParsingObject;

public class XMLPegGenerator extends PegGenerator {
	public XMLPegGenerator(ParsingObject node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	int arrayCount = 0;
	ArrayList<Integer> impliedList;
	String firstElementName;
	Map<String, Integer> elementNameMap = new HashMap<String, Integer>();
	Map<String, Integer> attMap = new HashMap<String, Integer>();
	Map<String, Integer> entityNameMap = new HashMap<String, Integer>();



	@Override
	public String generatePegFile() {
		StringBuilder sb = loadSource("forValidation/rootXml.peg");
		this.getElementName(this.node);
		this.getEntityName(this.node);
		this.generate(sb, this.node, 0);
		String generatedFilePath = "forValidation/generatedXml.peg";
		File newFile = new File(generatedFilePath);
		try {
			newFile.createNewFile();
			File file = new File(generatedFilePath);
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(sb.toString());
			fileWriter.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		return generatedFilePath;
	}
	
	private final void getElementName(ParsingObject node) {
		for (int i = 0; i < node.size(); i++) {
			ParsingObject subnode = node.get(i);
			String tag = subnode.getTag().toString();
			if (tag.equals("docTypeName")) {
				this.elementNameMap.put(subnode.getText(), 0);
				if (node.get(i + 1).getTag().toString().equals("element")) { // to get first element name
					firstElementName = node.get(i + 1).get(0).getText();
				}
			} else if (tag.equals("attlist")) {
				this.attMap.put(subnode.get(0).getText(), i);
			} else if (tag.equals("element")) {
				this.elementNameMap.put(subnode.get(0).getText(), i);
			}
		}
	}
	
	private final void getEntityName(ParsingObject node) {
		int count = 0;
		for (ParsingObject subnode : node) {
			if (subnode.getTag().toString().equals("entity")) {
				this.entityNameMap.put(subnode.get(0).getText(), count++);
			}
		}
	}

	private final void generate(StringBuilder sb, ParsingObject node, int index) {
		if (!this.entityNameMap.isEmpty()) {
			sb.append("PCdata = { ( @{ TEXT #text} / entity  )* #PCDATA }\n\n");
			generateEntityList(sb);
		} else {
			sb.append("PCdata = { TEXT #PCDATA }\n\n");
		}
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "docTypeName": // top of DTD
					sb.append("Element0 = Member0 _* \n\n");
					sb.append("Member0 = { @E_").append(firstElementName).append(" #member }\n\n"); //set start point
					break;
				//E_text
					
				case "element":
					generateElementRule(sb, subnode);
					break;

				case "attlist":
					generateAttributeRule(sb, subnode);
					break;

				case "entity" :
					generateEntityRules(sb, subnode);
					break;
			}
		}
	}
	
	private final void generateElementRule(StringBuilder sb, ParsingObject node) {
		String elementName = node.get(0).getText();
		int index = this.elementNameMap.get(elementName);
		if (this.attMap.containsKey(node.get(0).getText())) { // check whether attribute exists
			sb.append("E_").append(elementName)
					//sb.append("Element").append(index)
					.append(" = { '<").append(elementName)
					.append("\' _* @A_").append(elementName)
					.append(" _* ( '/>' / '>' _* @Member").append(index);
			//			if (node.size() == 3) { // when regular expression exists
			//					sb.append(node.get(2).getText()); //regex
			//				}
			sb.append(" _* '</").append(elementName).append(">' ) _* #element }\n\n");
		} else {
			sb.append("E_").append(elementName)
					//			sb.append("Element").append(index)
					.append(" = { '<").append(elementName)
					.append("\' _* '>' _* ")
					.append("@Member").append(index);
			//				if (node.size() == 3) {  // when regular expression exists
			//					sb.append(node.get(2).getText()); //regex
			//				}
			sb.append(" _* '</").append(elementName).append(">' _* #element }\n\n");
		}
		generateMemberListRule(sb, node, index);
	}
	
	private final void generateMemberListRule(StringBuilder sb, ParsingObject node, int index) {
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "member" :
					generateMemberRule(sb, subnode, index);
					break;
				case "others" :
					sb.append("Member").append(index)
							.append(" =");
					if (subnode.getText().equals("EMPTY")) {
						sb.append(" Empty \n\n");
					} else if (subnode.getText().equals("ANY")) {
						sb.append(" Any \n\n");
					}
					break;
			}
		}
	}

	private final void generateMemberRule(StringBuilder sb,
			ParsingObject node,
			int index)
	{
		//		int count = 0;
		for(ParsingObject subnode : node){
			switch (subnode.getTag().toString()) {
				case "seq" :
				case "choice" :
					StringBuilder groupedRule = new StringBuilder();
					generateGroupedRule(groupedRule, subnode);
					sb.append("Member")
							.append(index)
							.append(" = { (").append(groupedRule).append(" #member }\n\n");
					break;
				case "data" :
					if (node.size() == 1) {
						sb.append("Member").append(index)
								.append(" = { (@PCdata)? #data }\n\n"); //insert '?' to solve priority problems
					} else if (node.size() == 2) {
						sb.append("Member").append(index)
								.append(" = { (@PCdata)").append(node.get(1).getText())
								.append(" #data }\n\n");
					}
					break;
			}
		}
	}

	private final void generateGroupedRule(StringBuilder groupedRule, ParsingObject node) {
		for (ParsingObject subnode : node) {
			String tag = subnode.getTag().toString();
			if (tag.equals("member")) {
				generateGroupedMemberRule(groupedRule, subnode);
			}
			else if (tag.equals("or")) {
				groupedRule.append(" / ");
			}
			else if (tag.equals("regex")) {
				groupedRule.append(" )").append(subnode.getText());
				return;
			}
		}
		groupedRule.append(" ) ");
	}

	private final void generateGroupedMemberRule(StringBuilder sb, ParsingObject node) {
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "memberName" :
					String memberName = subnode.getText();
					if (node.size() == 1) {
						sb.append(" @E_").append(memberName);
					} else if (node.size() == 2) { // when regular expression exists
						sb.append(" (@E_").append(memberName).append(")")
								.append(node.get(1).getText()); // insert regex
					}
					break;
				case "seq" :
				case "choice" :
					StringBuilder innerRule = new StringBuilder();
					generateGroupedRule(innerRule, subnode);
					sb.append(" ( ").append(innerRule).append(" ");
					break;
				case "data" :
					sb.append(" @PCdata "); //
					break;
			}
		}
	}

	private final void generateAttributeRule(StringBuilder sb, ParsingObject node) {
		String elementName = node.get(0).getText();
		int index = elementNameMap.get(elementName);
		int attDefSize = node.size() - 1;
		int[] attList = new int[attDefSize];
		impliedList = new ArrayList<Integer>();
		for (int i = 0; i < attDefSize; i++) {
			attList[i] = i;
		}
		generateAttDefRule(sb, node, index);

		sb.append("A_").append(elementName).append(" = {");
		if (attDefSize == 1) {
			sb.append(" @AttDef").append(index).append("_0");
		} else {
			generatePermutaitonAttributeRule(sb, attList, index);
			sb.append("\n");
			if (!impliedList.isEmpty()) {
				sb.append("/ ");
				generateImpliedRule(sb, attList, index);
			}
		}
		sb.append(" #attribute }\n\n");
	}

	private final void generatePermutaitonAttributeRule(StringBuilder sb, int[] attList, int index) {
		int listLength = attList.length;
		int[] nextList = new int[listLength - 1];
		if (listLength == 1) {
			sb.append(" (@AttDef").append(index).append("_").append(attList[0]).append(") /");
		}
		else if (listLength == 2) {
			sb.append(" (@AttDef").append(index).append("_").append(attList[0])
					.append(" _* @AttDef").append(index).append("_").append(attList[1])
					.append(" ) /")
					.append(" (@AttDef").append(index).append("_").append(attList[1])
					.append(" _* @AttDef").append(index).append("_").append(attList[0])
					.append(" )");
		} else {
			for (int currentHeadNum = 0; currentHeadNum < attList.length; currentHeadNum++) {
				nextList = extractOtherNum(currentHeadNum, listLength);
				sb.append(" (@AttDef").append(index).append("_").append(currentHeadNum)
						.append(" _*(");
				generatePermutaitonAttributeRule(sb, nextList, index);
				sb.append(")) /");
			}
		}
	}

	private final void generateImpliedRule(StringBuilder sb, int[] attlist, int index) {
		int impliedListLength = impliedList.size();
		int totalRuleLength = attlist.length;
		if (impliedListLength == 1) { //impliedで宣言されているルール数が1のとき
			int[] requiredList = extractRequiredRule(attlist);
			generatePermutaitonAttributeRule(sb, requiredList, index); //requiredルールのみの順列を生成する
			sb.deleteCharAt(sb.length() - 1);
		}
		else if ((impliedListLength > 1) && (impliedListLength < totalRuleLength)) { //impliedルール数が2以上ルール総数未満
			generateMixedPermRule(sb, attlist, index);
		}
		else if (impliedListLength == totalRuleLength) { // 全てがimpliedルール
			generateCombinationAttributeRule(sb, attlist, totalRuleLength, index);
		}
	}

	private final int[] extractRequiredRule(int[] attlist) {
		int[] target = new int[attlist.length - impliedList.size()];
		if (target.length == 0)
			return target;
		int arrIndex = 0;
		for (int requiredNum : attlist) {
			if (!impliedList.contains(requiredNum)) {
				target[arrIndex++] = requiredNum;
			}
		}
		return target;
	}

	private final int[] extractImpliedRule(int[] attlist) {
		int[] target = new int[attlist.length - impliedList.size()];
		if (target.length == 0)
			return target;

		int arrIndex = 0;
		for (int impliedNum : attlist) {
			if (impliedList.contains(impliedNum)) {
				target[arrIndex++] = impliedNum;
			}
		}
		return target;
	}

	private final int[] extractOtherNum(int headNum, int maxRuleLength) {
		int[] otherNumList = new int[maxRuleLength - 1];
		int arrIndex = 0;
		for (int otherNum = 0; otherNum <= otherNumList.length; otherNum++) {
			if (otherNum != headNum) {
				otherNumList[arrIndex++] = otherNum;
			}
		}
		return otherNumList;
	}

	private final void generateCombinationAttributeRule(StringBuilder sb,int[] attList ,int totalRuleLength,int index) {
		for (int numOfRules = totalRuleLength - 1; numOfRules <= 1; numOfRules--) {
			int[][] combRules = Combination.combinationList(attList, numOfRules);
			for (int lineNum = 0; lineNum < combRules.length; lineNum++) {
				generatePermutaitonAttributeRule(sb, combRules[lineNum], index);
			}
			sb.append(" / ''");
		}
	}

	private final void generateMixedPermRule(StringBuilder sb, int[] attlist, int index) {
		int[] reqRuleList = extractRequiredRule(attlist);
		int maxRuleLength = attlist.length;
		int minRuleLength = reqRuleList.length;
		generatePermutaitonAttributeRule(sb, reqRuleList, index); //generate minimum length rule(not mixed choice)
		for (int ruleLength = minRuleLength + 1; ruleLength < maxRuleLength; ruleLength++) {
			for (int currentHeadNum = 0; currentHeadNum < maxRuleLength; currentHeadNum++) { //先頭のルール番号を決定する
				int[] otherList = extractOtherNum(currentHeadNum, maxRuleLength); // 残ったルール番号を元のリストから抽出する
				int[] extractedReqList = extractRequiredRule(otherList);
				int[] extractedImpList = extractImpliedRule(otherList);
				int numOfImpliedRule = ruleLength - (extractedReqList.length + 1);

				//				if (numOfImpliedRule == 1) {
				//					StringBuilder impliedRule = new StringBuilder();
				//					impliedRule.append("(");
				//					for (int i : extractedImpList) {
				//						impliedRule.append(" @AttDef").append("_").append(i).append(" /");
				//					}
				//					impliedRule.deleteCharAt(impliedRule.length() - 1); //delete "/"
				//					impliedRule.append(")");
				//					sb.append(" (@AttDef").append(index).append("_").append(currentHeadNum)
				//							.append(" _*");
				//					sb.append(impliedRule);
				//					sb.append(") /");
				//				}
				if (numOfImpliedRule >= 2) {
					sb.append(" (@AttDef").append(index).append("_").append(currentHeadNum)
							.append(" _* (");
					generateMixedPermRule(sb, otherList, index);
					sb.append(") /");
				}
			}
		}
	}

	private final void generateAttDefRule(StringBuilder sb, ParsingObject node, int index) {
		int count = 0;
		for (ParsingObject subnode : node) {
			if (subnode.getTag().toString().equals("attDef")) {
				String attName = subnode.get(0).getText();
				String attType = subnode.get(1).get(0).getTag().toString();
				String constraint = subnode.get(2).get(0).getTag().toString();
				sb.append("AttDef").append(index).append("_").append(count)
						.append(" = { ");
				StringBuilder att = new StringBuilder();
				switch (constraint) {
					case "IMPLIED" :
						generateAttTypedRule(att, attName, attType, subnode);
						sb.append(att);
						impliedList.add(count);
						break;
					case "REQUIRED" :
						generateAttTypedRule(att, attName, attType, subnode);
						sb.append(att);
						break;
					case "FIXED" :
						String constValue = subnode.get(2).get(1).getText(); //node must have constant value when #FIXED constraint
						sb.append("'").append(attName).append("' '=' '\"").append(constValue)
								.append("\"'");
						break;
					default :
						generateAttTypedRule(sb, attName, attType, subnode);
						String defaultValue = subnode.get(2).get(0).getText();
						sb.append(att).append(" / `").append(defaultValue).append("`");
						break;
				}
				sb.append(" #attPara } \n\n");
				count++;
			}
		}
	}

	private final void generateAttTypedRule(StringBuilder sb, String attName, String attType,
			ParsingObject node) {
		sb.append("'").append(attName).append("'").append(" '=' ");
		switch (attType) {
			case "CDATA" :
				sb.append("STRING");
				break;
			case "ID" :
				sb.append("'\"' <def IDLIST ([A-Za-z0-9:._]+) > '\"' ");
				break;
			case "IDREF" :
				sb.append("'\"' IDTOKEN '\"'");
				break;
			case "IDREFS" :
				sb.append("'\"' IDTOKEN (_ IDTOKEN)* '\"'");
				break;
			case "ENTITY" :
				sb.append("'\"' entity '\"'");
				break;
			case "ENTITIES" :
				sb.append("'\"' entity ( _ entity )* '\"'");
				break;
			case "NMTOKEN" :
				sb.append("NMTOKEN");
				break;
			case "NMTOKENS" :
				sb.append("'\"' NAME ( _ NAME) '\"'");
				break;
			case "enum" :
				sb.append("(");
				generateEnumMembers(sb, node.get(1).get(0));
				sb.append(")");
				break;
		}
	}

	private final void generateEnumMembers(StringBuilder sb, ParsingObject node) {
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "enumMember" :
					sb.append("'\"").append(subnode.getText()).append("\"'");
					break;
				case "or" :
					sb.append(subnode.getText()); // insert "/" 
					break;
			}
		}
	}

	private final void generateEntityRules(StringBuilder sb, ParsingObject node) {
		String entityName = node.get(0).getText();
		String replacedString = node.get(1).getText();
		int entityNum = this.entityNameMap.get(entityName);
		
		sb.append("Entity").append(entityNum)
				.append(" = { ").append(entityName).append(" `").append(replacedString)
				.append("` #entity }\n\n");
	}

	private final void generateEntityList(StringBuilder sb) {
		List<Map.Entry<String,Integer>> entries = 
				new ArrayList<Map.Entry<String,Integer>>(entityNameMap.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String,Integer>>() {
	
		@Override
		public int compare(
		Entry<String,Integer> entry1, Entry<String,Integer> entry2) {
				return ((Integer) entry2.getValue()).compareTo((Integer) entry1.getValue());
			}
		});
		sb.append("entity = (");
		for (Entry<String,Integer> s : entries) {
			sb.append(" @Entity").append(s.getValue()).append(" /");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(" ) \n\n");
	}
}

