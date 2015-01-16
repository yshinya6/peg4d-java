package org.peg4d.validator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.peg4d.ParsingObject;
import org.peg4d.UMap;

public class XMLPegGenerator extends PegGenerator {
	public XMLPegGenerator(ParsingObject node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	int arrayCount = 0;
	UMap<Integer> NameMap = new UMap<Integer>();
	UMap<Integer> AttMap = new UMap<Integer>();


	@Override
	public String generatePegFile() {
		StringBuilder sb = loadSource("forValidation/rootXml.peg");
		this.getElementName(this.node);
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
			if (subnode.getTag().toString().equals("docTypeName")) {
				this.NameMap.put(subnode.getText(), 0);
			} else if (subnode.getTag().toString().equals("attlist")) {
				this.AttMap.put(subnode.get(0).getText(), i);
			} else if (subnode.getTag().toString().equals("element")) {
				this.NameMap.put(subnode.get(0).getText(), i);
			}
		}
	}
	
	private final void generate(StringBuilder sb, ParsingObject node, int index) {
		int count = 0;
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "docTypeName": // top of DTD
					sb.append( "Element0 = _*  Member0 _* \n\n");
					sb.append( "Member0 = { @Element1 #member }\n\n");
					break;
					
				case "element":
					generateElementRules(sb, subnode, index);
					break;

				case "attlist":
					index = NameMap.get(subnode.get(0).getText());
					generate(sb, subnode, index);
					sb.append("Attribute").append(index).append(" ={");
					for (int i = 0; i <= subnode.size() - 2; i++) {
						if (subnode.get(i + 1).get(2).get(0).getText().equals("#IMPLIED")) {
							sb.append( "  (@AttParameter")
									.append(index).append("_").append(i)
								.append(")? _* #attribute");
						} else if (subnode.get(i + 1).get(2).get(0).getText().equals("#REQUIRED")) {
							sb.append("  @AttParameter").append(index).append("_").append(i)
								.append(" _* #attribute");
						}
					}
					sb.append( "}\n\n" );
					break;

				case "attParameter":
					if (subnode.get(2).get(0).getText().equals("#IMPLIED")) {
						sb.append("AttParameter").append(index).append("_").append(count)
							.append(" = { @AttName").append(index).append("_").append(count)
							.append(" '=' (@String)? #attPara } \n\n");
					} else {
						sb.append("AttParameter").append(index).append("_").append(count)
							.append(" = { @AttName").append(index).append("_").append(count)
							.append(" '=' @String #attPara } \n\n");
					}
					sb.append("AttName").append(index).append("_").append(count)
						.append(" = { '")
						.append(subnode.get(0).getText())
						.append("' #attName } \n\n");
					count++;
					break;

				case "or":
					count++;
					break;
			}
		}
	}
	
	private final void generateElementRules(StringBuilder sb,ParsingObject node, int index){
		String elementName = node.get(0).getText();
		index = this.NameMap.get(elementName);
		if (this.AttMap.hasKey(node.get(0).getText())) { // check whether attribute exists
			sb.append("Element").append(index)
					.append(" = { _* '<").append(elementName)
					.append("\' _+ @Attribute").append(index)
					.append(" _* ( '/>' / '>' _* @Members").append(index);
			if (node.size() == 3) { // when regular expression exists
					sb.append(node.get(2).getText()); //regex
				}
			sb.append(" _* '</").append(elementName).append(">' ) _* #element }\n\n");
			generateAttributeRules();
		} else {
				sb.append("Element").append(index)
						.append(" = { _* '<").append(elementName)
						.append("\' _* ( '/>' / '>' _* ")
						.append("(@Members").append(index)
						.append(")");
				if (node.size() == 3) {  // when regular expression exists
					sb.append(node.get(2).getText()); //regex
				}
				sb.append(" _* '</").append(elementName).append(">' ) _* #element }\n\n");
		}
		generateMemberList(sb, node, index);
	}
	
	private final void generateMember(StringBuilder sb, StringBuilder members, ParsingObject node,
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
								.append(this.NameMap.get(subnode.get(0).getText()))
								.append(" #member}\n\n");
					} else if (subnode.size() == 2) { // when regular expression exists
						sb.append("Member")
								.append(index).append("_").append(count)
								.append(" = { (@Element")
								.append(this.NameMap.get(subnode.get(0).getText()))
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
								.append(" = { CHARDATA #data }\n\n");
					} else {
						sb.append("Member").append(index).append("_").append(count)
								.append(" = { CHARDATA? #data }\n\n");
					}
					members.append(" @Member").append(index).append("_").append(count);
					count++;
					break;
			}
		}
	}
	
	private final void generateMemberList(StringBuilder sb, ParsingObject node, int index) {
		for (ParsingObject subnode : node) {
			switch (subnode.getTag().toString()) {
				case "member" :
					StringBuilder members = new StringBuilder();
					generateMember(sb, members, subnode, index);
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

	private final void generateAttributeRules(){
		
	}


}
