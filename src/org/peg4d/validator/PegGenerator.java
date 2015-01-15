package org.peg4d.validator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.peg4d.Main;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class PegGenerator {
	ParsingObject node;

	public PegGenerator(ParsingObject node) {
		this.node = node;
	}
	
	public String generatePegFile() {
		return null;
	}

	private ParsingSource generate(ParsingSource source, ParsingObject node, int index) {
		return null;
	}
	
	protected StringBuilder loadSource(String fileName) {
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
			return builder;
		} catch (IOException e) {
			e.printStackTrace();
			Main._Exit(1, "file error: " + fileName);
		}
		return null;
	}
}

