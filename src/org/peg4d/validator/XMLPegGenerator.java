package org.peg4d.validator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.peg4d.ParsingObject;

public class XMLPegGenerator extends PegGenerator {
	public XMLPegGenerator(ParsingObject node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	int arrayCount = 0;
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
			sb.append("Chardata = { ( @{ TEXT #text} / entity )* #PCDATA }\n\n");
			generateEntityList(sb);
		}
		sb.append("Chardata = { TEXT #PCDATA }\n\n");
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "docTypeName": // top of DTD
					sb.append("Element0 = Member0 _* \n\n");
					sb.append( "Member0 = { @Element1 #member }\n\n");
					break;
					
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
			sb.append("Element").append(index)
					.append(" = { '<").append(elementName)
					.append("\' _* @Attribute").append(index)
					.append(" _* ( '/>' / '>' _* @Members").append(index);
			if (node.size() == 3) { // when regular expression exists
					sb.append(node.get(2).getText()); //regex
				}
			sb.append(" _* '</").append(elementName).append(">' ) _* #element }\n\n");
		} else {
				sb.append("Element").append(index)
					.append(" = { '<").append(elementName)
						.append("\' _* ( '/>' / '>' _* ")
						.append("(@Members").append(index)
						.append(")");
				if (node.size() == 3) {  // when regular expression exists
					sb.append(node.get(2).getText()); //regex
				}
				sb.append(" _* '</").append(elementName).append(">' ) _* #element }\n\n");
		}
		generateMemberListRule(sb, node, index);
	}
	
	private final void generateMemberListRule(StringBuilder sb, ParsingObject node, int index) {
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "member" :
					StringBuilder members = new StringBuilder();
					generateMemberRule(sb, members, subnode, index);
					sb.append("Members")
							.append(index)
							.append(" = {")
							.append(members.toString())
							.append(" } \n\n");
					break;

				case "others" :
					sb.append("Members").append(index)
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

	private final void generateMemberRule(StringBuilder sb, StringBuilder members,
			ParsingObject node,
			int index)
	{
		int count = 0;
		for(ParsingObject subnode : node){
			switch (subnode.getTag().toString()) {
				case "memberName" :
					if (subnode.size() == 1) {
						sb.append("Member")
								.append(index).append("_").append(count)
								.append(" = { @Element")
								.append(this.elementNameMap.get(subnode.get(0).getText()))
								.append(" #member}\n\n");
					} else if (subnode.size() == 2) { // when regular expression exists
						sb.append("Member")
								.append(index).append("_").append(count)
								.append(" = { (@Element")
								.append(this.elementNameMap.get(subnode.get(0).getText()))
								.append(")")
								.append(subnode.get(1).getText()) // insert regex
								.append(" #member}\n\n");
					}
					members.append(" @Member").append(index).append("_").append(count);
					count++;
					break;

				case "or" :
					members.append(" / ");
					break;

				case "data" :
					if (node.size() > 1) {
						sb.append("Member").append(index).append("_").append(count)
								.append(" = { (@Chardata) #data }\n\n");
					} else {
						sb.append("Member").append(index).append("_").append(count)
								.append(" = { (@Chardata)? #data }\n\n");
					}
					members.append(" @Member").append(index).append("_").append(count);
					count++;
					break;
			}
		}
	}

	private final void generateAttributeRule(StringBuilder sb, ParsingObject node) {
		int index = elementNameMap.get(node.get(0).getText());
		generateAttParameterRule(sb, node, index);
		sb.append("Attribute").append(index).append(" ={");
		for (int i = 0; i <= node.size() - 2; i++) {
			if (node.get(i + 1).get(2).get(0).getText().equals("#IMPLIED")) {
				sb.append("  (@AttParameter")
						.append(index).append("_").append(i)
						.append(")? _* ");
			} else {
				sb.append("  @AttParameter").append(index).append("_").append(i)
						.append(" _* ");
			}
		}
		sb.append("#attribute }\n\n");
	}

	private final void generateAttParameterRule(StringBuilder sb, ParsingObject node, int index) {
		int count = 0;
		for (ParsingObject subnode : node) {
			if (subnode.getTag().toString().equals("attParameter")) {

				String attName = subnode.get(0).getText();
				String dataType = subnode.get(1).getText();
				String defaultValue = subnode.get(2).get(0).getText();

				sb.append("AttParameter").append(index).append("_").append(count)
						.append(" = { '").append(attName);

				if (defaultValue.equals("#IMPLIED")) {
					if (dataType.equals("NMTOKEN")) {
						sb.append("' '=' (NMTOKEN)? #attPara } \n\n");
					} else {
						sb.append("' '=' (STRING)? #attPara } \n\n");
					}
				}
				else if (defaultValue.equals("#FIXED")) {
					sb.append("' '=' \"");
					sb.append(subnode.get(2).get(1).getText());
					sb.append("\" #FIXED } \n\n");
				}
				else { // #REQUIRED or Enumerated or only "defaultValue"
					switch (dataType) {
						case "NMTOKEN" :
							sb.append("' '=' NMTOKEN #attPara } \n\n");
							break;
						case "Enum" :
							sb.append("' '=' ( ");
							generateEnumMembers(sb, subnode.get(1).get(0));
							sb.append(" ) #attPara } \n\n");
							break;
						default :
							sb.append("' '=' STRING #attPara } \n\n");
							break;
					}
				}
				count++;
			}
		}
	}

	private final void generateEnumMembers(StringBuilder sb, ParsingObject node) {
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "enumMember" :
					sb.append("'\"").append(subnode.getText()).append("\"'");
					break;
				case "or" :
					sb.append(subnode.getText());
					break;
			}
		}
	}

	private final void generateEntityRules(StringBuilder sb, ParsingObject node) {
		String entityName = node.get(0).getText();
		String replacedString = node.get(1).getText();
		int entityNum = this.entityNameMap.get(entityName);
		
		sb.append("Entity").append(entityNum)
				.append(" = { '").append(entityName).append("' `").append(replacedString)
				.append("` #entity }\n\n");
	}

	private final void generateEntityList(StringBuilder sb) {
		sb.append("entity = '&' (");
		for (int i = 0; i < this.entityNameMap.size() - 1; i++) {
			sb.append(" @Entity").append(i).append(" /");
		}
		sb.append(" @Entity").append(this.entityNameMap.size() - 1)
				.append(") ';' \n\n");
	}
}
