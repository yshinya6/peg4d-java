package org.peg4d.validator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.peg4d.Main;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;
import org.peg4d.UMap;

public class PegGenerater {
	ParsingObject node;

	public PegGenerater(ParsingObject node) {
		this.node = node;
	}
	
	public String generatePegFile() {
		return null;
	}

	private ParsingSource generate(ParsingSource source, ParsingObject node, int index) {
		return null;
	}
	
	protected String loadSource(String fileName) {
		InputStream Stream = Main.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			try {
				File f = new File(fileName);
//				if(f.length() > 128 * 1024) {
//					return new FileSource(fileName);
//				}
				Stream = new FileInputStream(fileName);
			} catch (IOException e) {
				Main._Exit(1, "file error: " + fileName);
				return null;
			}
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(Stream));
		try {
			StringBuilder builder = new StringBuilder();
			String line = reader.readLine();
			while (line != null) {
				builder.append(line);
				builder.append("\n");
				line = reader.readLine();
			}
			return builder.toString();
		} catch (IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}
}

class XMLPegGenerater extends PegGenerater {
	int arrayCount = 0;
	UMap<Integer> NameMap = new UMap<Integer>();
	UMap<Integer> AttMap = new UMap<Integer>();

	public XMLPegGenerater(ParsingObject node) {
		super(node);
	}

	@Override
	public String generatePegFile() {
		String generatedSource = loadSource("forValidation/rootXml.peg");
		this.getElementName(this.node);
		generatedSource = this.generate(generatedSource, this.node, 0);
		String generatedFilePath = "forValidation/generatedXml.peg";
		File newFile = new File(generatedFilePath);
		try {
			newFile.createNewFile();
			File file = new File(generatedFilePath);
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(generatedSource);
			fileWriter.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		return generatedFilePath;
	}

	private final String generate(String source, ParsingObject node, int index) {
		int count = 0;
		for (int i = 0; i < node.size(); i++) {
			switch (node.get(i).getTag().toString()) {
			case "element":
				index = this.NameMap.get(node.get(i).get(0).getText());
				if (this.AttMap.hasKey(node.get(i).get(0).getText())) {
					if (node.get(i).size() == 3) {
						source += "Element" + index + " = { _* '<' @ElementName" + index + " _+ @Attribute" + index + " _* ( '/>' / '>' _* "
								+ "@Members"
								+ index + node.get(i).get(2).getText() + " _* '</' ELEMENTNAME"
								+ index + "'>' ) _* #element }\n\n";
					} else {
						source += "Element" + index + " = { _* '<' @ElementName" + index + " _+ @Attribute" + index + " _* ( '/>' / '>' _* "
								+ "@Members"
								+ index + " _* '</' ELEMENTNAME" + index
								+ "'>' ) _* #element }\n\n";
					}
				}
				else{
					if (node.get(i).size() == 3) {
						source += "Element" + index + " = { _* '<' @ElementName"
 + index
								+ " _* ( '/>' / '>' _* " + "(@Members" + index + ")"
								+ node.get(i).get(2).getText() + " _* '</' ELEMENTNAME"
								+ index
								+ "'>' ) _* #element }\n\n";
					} else {
						source += "Element" + index + " = { _* '<' @ElementName"
 + index
								+ " _* ( '/>' / '>' _* " + "@Members" + index
								+ " _* '</' ELEMENTNAME" + index + "'>' ) _* #element }\n\n";
					}
				}
				source = generate(source, node.get(i), index);
				break;

			case "elementName":
				source += "ElementName" + index + " = { \"" + node.get(i).getText()
						+ "\" #string }\n\n";
				source += "ELEMENTNAME" + index + " =  \"" + node.get(i).getText() + "\"\n\n";
				break;

			case "member":
				source = generate(source, node.get(i), index);
				source += "Members" + index + " = {";
				for (int j = 0; j < node.get(i).size() - 1; j++) {
					if (node.get(i).size() >= j + 2
							&& node.get(i).get(j + 1).getTag().toString().equals("or")) {
						source += " @Member" + index + "_" + j + " /";
						j++;
					}
 else {
						source += " @Member" + index + "_" + j;
					}
				}
				source += " @Member" + index + "_" + (node.get(i).size() - 1) + "}\n\n";
				break;

			case "others":
				source += "Members" + index + " =";
				if (node.get(i).getText().equals("EMPTY")) {
					source += " Empty \n\n";
				} else if (node.get(i).getText().equals("ANY")) {
					source += " Any \n\n";
				}
				break;

			case "docTypeName": // top of DTD
				source += "Element0 = _*  Member0 _* \n\n";
				source += "Member0 = { @Element1 #member }\n\n";
				break;

			case "memberName":
				if (node.get(i).size() == 1) {
					source += "Member" + index + "_" + count + " = { @Element"
							+ this.NameMap.get(node.get(i).get(0).getText())
							+ " #member}\n\n";
				} else if (node.get(i).size() == 2) {
					source += "Member" + index + "_" + count + " = { (@Element"
							+ this.NameMap.get(node.get(i).get(0).getText()) + ")"
							+ node.get(i).get(1).getText() + " #member}\n\n";
				}
				count++;
				break;

			case "data":
				if (node.size() > 1) {
					source += "Member" + index + "_" + count + " = { CHARDATA #data }\n\n";
				}
				else{
					source += "Member" + index + "_" + count + " = { CHARDATA? #data }\n\n";
				}
				count++;
				break;

			case "attlist":
				source = generate(source, node.get(i), index);
				source += "Attribute" + index + " ={";
				for (int j = 0; j <= node.get(i).size() - 2; j++) {
					if (node.get(i).get(j + 1).get(2).get(0).getText()
							.equals("#IMPLIED")) {
						source += "  (@AttParameter" + index + "_" + j + ")? _* #attribute";
					} else {
						source += "  @AttParameter" + index + "_" + j + " _* #attribute";
					}
				}
				source += "}\n\n";
				break;

			case "attParameter":
				if (node.get(i).get(2).get(0).getText().equals("#IMPLIED")) {
					source += "AttParameter" + index + "_" + count + " = { @AttName" + index + "_"
							+ count + " '=' (@String)? #attPara } \n\n";
				} else {
					source += "AttParameter" + index + "_" + count + " = { @AttName" + index + "_"
							+ count + " '=' @String #attPara } \n\n";
				}
				source += "AttName" + index + "_" + count + " = { '"
 + node.get(i).get(0).getText()
						+ "' #attName } \n\n";
				count++;
				break;

			case "or":
				count++;
				break;
			}
		}
		return source;
	}


	public final void getElementName(ParsingObject node) {
		for (int i = 0; i < node.size(); i++) {
			if (node.get(i).getTag().toString().equals("docTypeName")) {
				this.NameMap.put(node.get(i).getText(), 0);
			} else if (node.get(i).getTag().toString().equals("attlist")) {
				this.AttMap.put(node.get(i).get(0).getText(), i);
			} else if (node.get(i).getTag().toString().equals("element")) {
				this.NameMap.put(node.get(i).get(0).getText(), i);
			}
		}
	}
}


class JSONPegGenerater extends PegGenerater {
	int itemID = 1;
	int objectID = 1;

	public JSONPegGenerater(ParsingObject node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String generatePegFile() {
		String generatedSource = loadSource("forValidation/rootJSON.peg");
		generatedSource = this.generate(generatedSource, this.node, 0);
		String generatedFilePath = "forValidation/generatedJSON.peg";
		File newFile = new File(generatedFilePath);
		try {
			newFile.createNewFile();
			File file = new File(generatedFilePath);
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(generatedSource);
			fileWriter.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		return generatedFilePath;
	}

	private final String generate(String source, ParsingObject node, int index) {
		ParsingObject currentNode = node.get(0);
		if (currentNode.get(0).get(0).getText().equals("$schema")) {
			for (int j = 1; j < currentNode.size(); j++) {
				if (currentNode.get(j).get(0).getText().equals("type")) {
					if (currentNode.get(j).get(1).getText().equals("array")) {
						source += "Schema0" + "		= { BEGINARRAY (WS @Item0 (VS @Item0)* )* WS ENDARRAY #Array0 }\n\n";
						generateArrayRules(source, currentNode, j + 1);
					}
					// if (currentNode.get(j).get(1).equals("object")) {
					// source += "Schema0"
					// +
					// "		= { BEGINARRAY (WS @Item0 (VS @Item0)* )* WS ENDARRAY #Array0 }";
					// generateObjectRules(source, currentNode, j + 1);
					// }
				}
			}
		}
		return source;
	}
	
	public String generateArrayRules(String source, ParsingObject node, int index) {
		String itemTitle = null;
		if (node.get(index).get(0).getText().equals("items")) {
			ParsingObject schemaPart = node.get(index).get(1);
			for (int i = 0; i < schemaPart.size(); i++) {
				if (schemaPart.get(i).get(0).getText().equals("title")) {
					itemTitle = schemaPart.get(i).get(1).getText();
				} else if (schemaPart.get(i).get(0).getText().equals("type")) {
					if (schemaPart.get(i).get(1).getText().equals("object")) {
						source += "Item" + itemID + "		= { @Object" + objectID + " #" + itemTitle + " }\n\n";
						generate(source, schemaPart, i + 1);
					}
				}
			}
			itemID++;
		}
		return source;
	}

	public String generateObjectRules(String source, ParsingObject node, int index){
		// At first, search 'required' key
		// and make required list
		ArrayList<String> requiredTagList;
		for (int j = node.size(); j >= 0; j--) {
			if (node.get(j).get(0).getText().equals("required")) {
				requiredTagList = getRequiredList(node.get(j));
				break;
			}
		}
		if (node.get(index).get(0).getText().equals("properties")) {
			ParsingObject propertiesNode = node.get(index).get(1);
			for (int i = 0; i < node.size(); i++) {

			}
		}
		return source;
	}
	


	public ArrayList<String> getRequiredList(ParsingObject node) {
		if (node.get(1).getTag().toString().equals("String")) {
			ArrayList<String> reqList = new ArrayList<>();
			reqList.add(node.get(1).getText());
			return reqList;
		} else if (node.get(1).getTag().equals("Array")) {
			ArrayList<String> reqList = new ArrayList<>();
			for (int i = 0; i < node.get(1).size(); i++) {
				reqList.add(node.get(i).getText());
			}
			return reqList;
		}
		return null;
	}

}
