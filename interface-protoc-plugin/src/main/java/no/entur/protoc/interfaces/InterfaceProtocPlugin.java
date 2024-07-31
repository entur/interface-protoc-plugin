package no.entur.protoc.interfaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import xsd.Xsd;

import com.google.protobuf.compiler.PluginProtos;

public class InterfaceProtocPlugin extends com.salesforce.jprotoc.Generator {

	private static final String DEFAULT_TARGET_FOLDER = "target/generated-sources/proto-interfaces";

	private String[] commandLineArgs;

	private String targetFolder;

	private boolean generateInterfaces;
	private boolean generateAddInterfaceCodeGenerationFiles;
	private InterfaceProtocContext context;

	public static void main(String[] args) {

		com.salesforce.jprotoc.ProtocPlugin.generate(Arrays.asList(new InterfaceProtocPlugin(args)), Arrays.asList(Xsd.baseType));
	}

	public InterfaceProtocPlugin(String[] commandLineArgs) {
		this.commandLineArgs = commandLineArgs;
	}

	@Override
	public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) {
		parseArgs(combineCommandLineArgsAndPluginParam(commandLineArgs, request.getParameter()));
		context = new InterfaceProtocContext(targetFolder, request);

		List<MessageTypeHandler> messageTypeHandlers = request.getProtoFileList()
				.stream()
				.filter(file -> request.getFileToGenerateList().contains(file.getName()))
				.map(file -> file.getMessageTypeList().stream().map(messageType -> new MessageTypeHandler(context, messageType, file)))
				.flatMap(Function.identity())
				.collect(Collectors.toList());

		if (generateInterfaces) {
			messageTypeHandlers.forEach(MessageTypeHandler::generateInterfaces);
		}
		if (generateAddInterfaceCodeGenerationFiles) {
			return messageTypeHandlers.stream()
					.map(MessageTypeHandler::generateAddInterfaceCodeGenerationFiles)
					.flatMap(List::stream)
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	// Must support both plugin parameters and command line arguments to support usage as stand alone executable and chained plugin
	private String[] combineCommandLineArgsAndPluginParam(String args[], String pluginParamRaw) {
		String[] pluginParams = pluginParamRaw == null ? new String[0] : pluginParamRaw.split(" ");

		int aLen = pluginParams.length;
		int bLen = args.length;
		String[] result = new String[aLen + bLen];
		System.arraycopy(pluginParams, 0, result, 0, aLen);
		System.arraycopy(args, 0, result, aLen, bLen);
		return result;
	}

	private void parseArgs(String args[]) {

		Options options = new Options();

		Option targetOption = new Option("t", "target", true, "target folder for generated interfaces");
		targetOption.setRequired(false);
		options.addOption(targetOption);

		Option generateInterfacesOption = new Option("gi", "generate-interfaces", false, "generate interfaces for proto messages");
		generateInterfacesOption.setRequired(false);
		options.addOption(generateInterfacesOption);

		Option implementInterfacesOption = new Option("ii", "implement-interfaces", false,
				"generate protoc code generation files to make protoc generate java messages that implement interfaces");
		options.addOption(implementInterfacesOption);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("", options);

			System.exit(1);
		}

		if (cmd.hasOption(targetOption.getOpt())) {
			targetFolder = cmd.getOptionValue(targetOption.getOpt());
		} else {
			targetFolder = DEFAULT_TARGET_FOLDER;
		}
		generateInterfaces = cmd.hasOption(generateInterfacesOption.getOpt());
		generateAddInterfaceCodeGenerationFiles = cmd.hasOption(implementInterfacesOption.getOpt());

	}
}
