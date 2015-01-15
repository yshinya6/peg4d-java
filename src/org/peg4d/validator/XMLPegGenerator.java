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
					index = this.NameMap.get(subnode.get(0).getText());
					if (this.AttMap.hasKey(subnode.get(0).getText())) {
						if (subnode.size() == 3) {
							sb.append("Element")
								.append(index)
								.append(" = { _* '<' @ElementName" )
								.append(index)
								.append(" _+ @Attribute")
								.append(index)
								.append(" _* ( '/>' / '>' _* @Members")
								.append(index)
								.append(subnode.get(2).getText())
								.append(" _* '</' ELEMENTNAME")
								.append(index)
								.append("'>' ) _* #element }\n\n");
						} else {
							sb.append("Element")
								.append(index)
								.append(" = { _* '<' @ElementName")
								.append(index)
								.append(" _+ @Attribute")
								.append(index)
								.append(" _* ( '/>' / '>' _* @Members")
								.append(index)
								.append(" _* '</' ELEMENTNAME")
								.append(index)
								.append("'>' ) _* #element }\n\n");
						}
					} else {
						if (subnode.size() == 3) {
							sb.append("Element")
								.append(index)
								.append( " = { _* '<' @ElementName")
								.append(index)
								.append(" _* ( '/>' / '>' _* ")
								.append("(@Members")
								.append(index)
								.append(")")
								.append(subnode.get(2).getText())
								.append(" _* '</' ELEMENTNAME")
								.append(index)
								.append("'>' ) _* #element }\n\n");
						} else {
							sb.append("Element" + index
									+ " = { _* '<' @ElementName" + index
									+ " _* ( '/>' / '>' _* " + "@Members"
									+ index + " _* '</' ELEMENTNAME" + index
									+ "'>' ) _* #element }\n\n");
						}
					}
					generate(sb, subnode, index);
					break;

				case "elementName":
					sb.append("ElementName") 
						.append(index)
						.append(" = { \"")
						.append(subnode.getText())
						.append("\" #string }\n\n");
					sb.append("ELEMENTNAME")
						.append(index)
						.append(" =  \"")
						.append(subnode.getText())
						.append("\"\n\n");
					break;

				case "member":
					generate(sb, subnode, index);
					sb.append("Members") 
					.append(index) 
					.append(" = {" );
					for (int j = 0; j < subnode.size() - 1; j++) {
						if (subnode.size() >= j + 2 && subnode.get(j + 1).getTag().toString().equals("or")) {
							sb.append( " @Member")
								.append(index)
								.append("_")
								.append(j)
								.append(" /");
							j++;
						} else {
							sb.append(" @Member")
								.append(index)
								.append("_")
								.append(j);
						}
					}
					sb.append( " @Member") 
						.append(index)
						.append("_")
						.append(subnode.size() - 1)
						.append("}\n\n");
					break;

				case "others":
					sb.append( "Members")
						.append(index)
						.append(" =");
					if (subnode.getText().equals("EMPTY")) {
						sb.append( " Empty \n\n" );
					} else if (subnode.getText().equals("ANY")) {
						sb.append( " Any \n\n");
					}
					break;



				case "memberName":
					if (subnode.size() == 1) { 
						sb.append( "Member")
							.append(index)
							.append("_")
							.append(count)
							.append(" = { @Element")
							.append(this.NameMap.get(subnode.get(0).getText()))
							.append(" #member}\n\n");
					} else if (subnode.size() == 2) { //when regular expression exists
						sb.append("Member").append(index).append("_").append(count)
							.append(" = { (@Element")
							.append(this.NameMap.get(subnode.get(0).getText())) 
							.append(")")
							.append(subnode.get(1).getText()) // indicate regex
							.append(" #member}\n\n");
					}
					count++;
					break;

				case "data":
					if (node.size() > 1) {
						sb.append("Member").append(index).append("_").append(count)
							.append(" = { CHARDATA #data }\n\n");
					} else {
						sb.append("Member").append(index).append("_").append(count)
							.append(" = { CHARDATA? #data }\n\n");
					}
					count++;
					break;

				case "attlist":
					generate(sb, subnode, index);
					sb.append("Attribute").append(index).append(" ={");
					for (int j = 0; j <= subnode.size() - 2; j++) {
						if (subnode.get(j + 1).get(2).get(0).getText().equals("#IMPLIED")) {
							sb.append( "  (@AttParameter")
								.append(index).append("_").append(j)
								.append(")? _* #attribute");
						} else if (subnode.get(j + 1).get(2).get(0).getText().equals("#REQUIRED")) {
							sb.append( "  @AttParameter").append(index).append("_").append(j)
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
		
	}
	
	private final void generateAttributeRules(){
		
	}


}
