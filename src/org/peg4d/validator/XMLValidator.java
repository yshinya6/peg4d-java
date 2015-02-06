package org.peg4d.validator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.peg4d.Grammar;
import org.peg4d.GrammarFactory;
import org.peg4d.Main;
import org.peg4d.MemoizationManager;
import org.peg4d.ParsingContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class XMLValidator {
	private final String pegForDTD = "resource/xml_dtd.p4d";
	private String DTDFile;
	private String inputXMLFile;
	private String errorMessage;
	private boolean result;

	public XMLValidator(String DTDFile, String inputXMLFile) {
		this.DTDFile = DTDFile;
		this.inputXMLFile = inputXMLFile;
		this.result = false;
		this.errorMessage = "";
	}

	public boolean run() {
		GrammarFactory dtdGrammarFactory = new GrammarFactory();
		Grammar peg4d = dtdGrammarFactory.newGrammar("DTD", pegForDTD);
		ParsingSource dtdSource = ParsingSource.loadSource(DTDFile);
		ParsingContext dtdContext = new ParsingContext(dtdSource);
		ParsingObject node = dtdContext.parse(peg4d, "File");
		XMLPegGenerator gen = new XMLPegGenerator(node);
		String genPegSource = gen.generatePegFile();
		GrammarFactory xmlGrammarFactory = new GrammarFactory();
		Grammar genPeg = xmlGrammarFactory.newGrammar("XML", genPegSource);
		ParsingSource xmlSource = ParsingSource.loadSource(inputXMLFile);
		ParsingContext xmlContext = new ParsingContext(xmlSource);
		xmlContext.match(genPeg, "File", new MemoizationManager());
		if (xmlContext.hasByteChar()) {
			setErrorMessage(xmlContext.fpos);
		}
		return !xmlContext.hasByteChar();
	}

	public void bench() {
		long startTime = 0;
		long endTime = 0;
		long compStartTime = 0;
		long compENDTime = 0;
		long validateStartTime = 0;
		long validateEndTime = 0;

		for (int i = 0; i < 10; i++) {
			startTime = System.currentTimeMillis();
			compStartTime = System.currentTimeMillis();
			GrammarFactory dtdGrammarFactory = new GrammarFactory();
			Grammar peg4d = dtdGrammarFactory.newGrammar("DTD", pegForDTD);
			ParsingSource dtdSource = ParsingSource.loadSource(DTDFile);
			ParsingContext dtdContext = new ParsingContext(dtdSource);
			ParsingObject node = dtdContext.parse(peg4d, "File");
			XMLPegGenerator gen = new XMLPegGenerator(node);
			String genPegSource = gen.generatePegFile();
			compENDTime = System.currentTimeMillis();
			validateStartTime = System.currentTimeMillis();
			GrammarFactory xmlGrammarFactory = new GrammarFactory();
			Grammar genPeg = xmlGrammarFactory.newGrammar("XML", genPegSource);
			ParsingSource xmlSource = ParsingSource.loadSource(inputXMLFile);
			ParsingContext xmlContext = new ParsingContext(xmlSource);
			xmlContext.match(genPeg, "File", new MemoizationManager());
			validateEndTime = System.currentTimeMillis();
			endTime = System.currentTimeMillis();
			System.out.println("Total Time    : " + (endTime - startTime) + " [ms]");
			System.out.println("Compile Time  : " + (compENDTime - compStartTime) + " [ms]");
			System.out.println("Validate Time : " + (endTime - startTime) + " [ms]\n");

		}
		//		if (xmlContext.hasByteChar()) {
		//			setErrorMessage(xmlContext.fpos);
		//		}
		//		return !xmlContext.hasByteChar();
	}

	//	public void validateForExperiment() {
	//		long startTime = 0;
	//		long endTime = 0;
	//		for (int i = 0; i < 21; i++) {
	//		GrammarFactory xmlGrammarFactory = new GrammarFactory();
	//		Grammar genPeg = xmlGrammarFactory.newGrammar("XML", DTDFile); //DTDFile = generatedXML.peg
	//			ParsingSource xmlSource = ParsingSource.loadSource(inputXMLFile);
	//			ParsingContext xmlContext = new ParsingContext(xmlSource);
	//			startTime = System.currentTimeMillis();
	//			ParsingObject xmlNode = xmlContext.parse(genPeg, "File");
	//			endTime = System.currentTimeMillis();
	//			System.out.println(endTime - startTime + "[ms]");
	//		}
	//	}
	public void measureCompileTime() {
		long startTime = 0;
		long endTime = 0;
		startTime = System.currentTimeMillis();
		GrammarFactory dtdGrammarFactory = new GrammarFactory();
		Grammar peg4d = dtdGrammarFactory.newGrammar("DTD", pegForDTD);
		ParsingSource dtdSource = ParsingSource.loadSource(DTDFile);
		ParsingContext dtdContext = new ParsingContext(dtdSource);
		ParsingObject node = dtdContext.parse(peg4d, "File");
		XMLPegGenerator gen = new XMLPegGenerator(node);
		String genPegSource = gen.generatePegFile();
		endTime = System.currentTimeMillis();
		System.out.println(endTime - startTime + "[ms]");
	}

	public boolean getResult() {
		return this.result;
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	private String loadSource(String fileName) {
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
			return builder.toString();
		} catch (IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}

	private void setErrorMessage(long fpos) {
		String source = loadSource(inputXMLFile);
		long startPos = 0;
		long pos = fpos + 1;
		long endPos = 0;
		this.errorMessage += " ==================== Infomation ==================== \n\n";
		while (true) {
			if (source.charAt((int) pos) == '>') {
				endPos = pos;
				while (!(source.charAt((int) --pos) == '<')) {
					startPos = pos;
				}
				this.errorMessage += "Error Type        : Invalid Element Name or Attribute \n";
				this.errorMessage += "Failure Position  : " + fpos + "\n";
				this.errorMessage += "Failure Tag       : <"
						+ source.substring((int) startPos, (int) endPos)
						+ "> \n";
				this.errorMessage += "Failure Character : ";
//						+ source.substring((int) startPos, (int) fpos)
//						+ "\'"
//						+ source.charAt((int) fpos) + "\'"
//						+ source.substring((int) fpos + 1, (int) endPos);

				break;
			} else if (source.charAt((int) pos) == '<') {
				endPos = pos;
				while (!(source.charAt((int) --pos) == '>')) {
					startPos = pos;
				}
				this.errorMessage += "Error Type        : Invalid Contents \n";
				this.errorMessage += "Failure Position  : " + fpos + "\n";
				this.errorMessage += "Failure Part      : "
						+ source.substring((int) startPos, (int) endPos)
						+ " \n";
				this.errorMessage += "Failure Character : "
						//+ source.substring((int) startPos, (int) fpos)
						+ "\'"
						+ source.charAt((int) fpos) + "\'"
						+ source.substring((int) fpos + 1, (int) endPos);
				break;
			}
			pos++;
		}
		this.errorMessage += "\n\n ==================================================== \n";
		return;
	}


}
