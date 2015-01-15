package org.peg4d.validator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.peg4d.Grammar;
import org.peg4d.GrammarFactory;
import org.peg4d.Main;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class JSONValidator {
	private final String pegForJSON = "forValidation/json.peg";
	private String JSONSchemaFile;
	private String inputJSONFile;
	private String errorMessage;
	private boolean result;

	public JSONValidator(String JSONSchemaFile, String inputJSONFile) {
		this.JSONSchemaFile = JSONSchemaFile;
		this.inputJSONFile = inputJSONFile;
		this.result = false;
		this.errorMessage = "";
	}

	public boolean run() {
		GrammarFactory jsonSchemaGrammarFactory = new GrammarFactory();
		Grammar peg4d = jsonSchemaGrammarFactory.newGrammar("JSONSchema",
				pegForJSON);
		ParsingSource schemaSource = ParsingSource.loadSource(JSONSchemaFile);
		ParsingContext schemaContext = new ParsingContext(schemaSource);
		ParsingObject node = schemaContext.parse(peg4d, "File");
		JSONPegGenerator gen = new JSONPegGenerator(node);
		String genPegSource = gen.generatePegFile();
		GrammarFactory jsonGrammarFactory = new GrammarFactory();
		Grammar genPeg = jsonGrammarFactory.newGrammar("JSON", genPegSource);
		ParsingSource jsonSource = ParsingSource.loadSource(inputJSONFile);
		ParsingContext jsonContext = new ParsingContext(jsonSource);
		ParsingObject jsonNode = jsonContext.parse(genPeg, "File");
		return !jsonContext.hasByteChar();
	}

	public boolean getResult() {
		return this.result;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	private StringBuilder loadSource(String fileName) {
		InputStream Stream = Main.class.getResourceAsStream("/" + fileName);
		if (Stream == null) {
			try {
				// File f = new File(fileName);
				// if(f.length() > 128 * 1024) {
				// return new FileSource(fileName);
				// }
				Stream = new FileInputStream(fileName);
			} catch (IOException e) {
				Main._Exit(1, "file error: " + fileName);
				return null;
			}
		}
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(Stream));
		try {
			StringBuilder builder = new StringBuilder();
			String line = reader.readLine();
			while (line != null) {
				builder.append(line);
				builder.append("\n");
				line = reader.readLine();
			}
			return builder;
		} catch (IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}

}
