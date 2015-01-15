package org.peg4d.validator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.peg4d.ParsingObject;

public class JSONPegGenerator extends PegGenerator {
	int itemID = 1;
	int objectID = 1;

	public JSONPegGenerator(ParsingObject node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String generatePegFile() {
		StringBuilder sb = loadSource("forValidation/rootJSON.peg");
		this.generate(sb, this.node, 0);
		String generatedFilePath = "forValidation/generatedJSON.peg";
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

	private final void generate(StringBuilder sb, ParsingObject node, int index) {
		ParsingObject currentNode = node.get(0);
		if (currentNode.get(0).get(0).getText().equals("$schema")) {
			for (int j = 1; j < currentNode.size(); j++) {
				if (currentNode.get(j).get(0).getText().equals("type")) {
					if (currentNode.get(j).get(1).getText().equals("array")) {
						sb.append("Schema0 \t\t= { BEGINARRAY (WS @Item0 (VS @Item0)* )* WS ENDARRAY #Array0 }\n\n");
						generateArrayRules(sb, currentNode, j + 1);
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
	}

	public void generateArrayRules(StringBuilder sb, ParsingObject node, int index) {
		String itemTitle = null;
		if (node.get(index).get(0).getText().equals("items")) {
			ParsingObject schemaPart = node.get(index).get(1);
			for (int i = 0; i < schemaPart.size(); i++) {
				if (schemaPart.get(i).get(0).getText().equals("title")) {
					itemTitle = schemaPart.get(i).get(1).getText();
				} else if (schemaPart.get(i).get(0).getText().equals("type")) {
					if (schemaPart.get(i).get(1).getText().equals("object")) {
						sb.append("Item")
						.append(itemID) 
						.append("\t\t = { @Object")
						.append(objectID)
						.append(" #")
						.append(itemTitle)
						.append(" }\n\n");
						generate(sb, schemaPart, i + 1);
					}
				}
			}
			itemID++;
		}
	}

	public void generateObjectRules(StringBuilder sb, ParsingObject node, int index) {
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
	}

	public ArrayList<String> getRequiredList(ParsingObject node) {
		ParsingObject subnode = node.get(1);
		if (subnode.getTag().toString().equals("String")) {
			ArrayList<String> reqList = new ArrayList<>();
			reqList.add(subnode.getText());
			return reqList;
		} else if (subnode.getTag().equals("Array")) {
			ArrayList<String> reqList = new ArrayList<>();
			for (ParsingObject subsubnode : subnode) {
				int i = 0;
				reqList.add(node.get(i++).getText());
			}
			return reqList;
		}
		return null;
	}

}
