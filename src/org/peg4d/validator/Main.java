package org.peg4d.validator;

import org.peg4d.MemoizationManager;
import org.peg4d.ParsingObject;



public class Main {
	private final static String defaultStartPoint = "Toplevel";
	private static String SchemaFile = null;
	private static String InputFile = null;
	private static boolean XMLMode = false;
	private static boolean JSONMode = false;
	private static boolean BenchmarkXML = false;
	public static boolean IgnoreRequired = false;
	public static boolean CompileMode = false;


	public final static void main(String[] args) {
		ParsingObject resultNode = null;
		parseCommandOption(args);
		if (XMLMode) {
			XMLValidator validator = new XMLValidator(SchemaFile, InputFile);
			boolean result = validator.run();
			if (result) {
				System.out.println("VALID XML FILE");
			} else {
				System.out.println("INVALID XML FILE");
				System.out.println(validator.getErrorMessage());
			}
			System.exit(0);
		}
		else if (BenchmarkXML) {
			XMLValidator validator = new XMLValidator(SchemaFile, InputFile);
			validator.bench();
			System.exit(0);
		}
		else if (CompileMode) {
			XMLValidator validator = new XMLValidator(SchemaFile, InputFile);
			validator.measureCompileTime();
			System.exit(0);
		}
		else if (JSONMode) {
			JSONValidator validator = new JSONValidator(SchemaFile, InputFile);
			boolean result = validator.run();
			if (result) {
				System.out.println("VALID JSON FILE");
			} else {
				System.out.println("INVALID JSON FILE");
				System.out.println(validator.getErrorMessage());
			}
			System.exit(0);
		}
	}

	final static void showUsage(String Message) {
		System.out.println(Message);
		System.out.println("<Usage>\n");
		System.out
				.println("Converting Schema to PEGs & Validation Mode : --xml <DTDFilePath> <InputXMLFilePath>\n");
		System.out
				.println("                             Benchmark Mode : --bench <DTDFilePath> <InputXMLFilePath>\n\n");
	}

	private static void parseCommandOption(String[] args) {
		int index = 0;
		while (index < args.length) {
			String argument = args[index];
			index++;
			if ((argument.equals("--xml") || argument.equals("--XML")) && (index < args.length)) {
				XMLMode = true;
				if (index < args.length)
					SchemaFile = args[index++];
				if (index <= args.length)
					InputFile = args[index++];
			}
			else if ((argument.equals("--bench"))
					&& (index < args.length)) {
				BenchmarkXML = true;
				if (index < args.length)
					SchemaFile = args[index++];
				if (index <= args.length)
					InputFile = args[index++];
			}
			else if ((argument.equals("--compile"))
					&& (index < args.length)) {
				CompileMode = true;
				if (index < args.length)
					SchemaFile = args[index++];
			}
			else if ((argument.equals("--json") || argument.equals("--JSON"))
					&& (index < args.length)) {
				JSONMode = true;
				if (index < args.length)
					SchemaFile = args[index++];
				if (index <= args.length)
					InputFile = args[index++];
			}
			else if ((argument.startsWith("--memo"))) {
				if (argument.equals("--memo:none")) {
					MemoizationManager.NoMemo = true;
				}
				else if (argument.equals("--memo:packrat")) {
					MemoizationManager.PackratParsing = true;
				}
				else if (argument.equals("--memo:window")) {
					MemoizationManager.SlidingWindowParsing = true;
				}
				else if (argument.equals("--memo:slide")) {
					MemoizationManager.SlidingLinkedParsing = true;
				}
				else if (argument.equals("--memo:notrace")) {
					MemoizationManager.Tracing = false;
				}
				else {
					showUsage("unknown option: " + argument);
				}
			}
			else {
				showUsage("Error!!");
				Main._Exit(0, "Unknown Option or Command: " + argument);
			}
		}
	}

	public final static void _Exit(int status, String message) {
		System.out.println("EXIT " + message);
		System.exit(status);
	}
}
