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

import com.google.protobuf.compiler.PluginProtos;

import xsd.Xsd;

public class InterfaceProtocPlugin extends com.salesforce.jprotoc.Generator {

	private static final String DEFAULT_TARGET_FOLDER = "target/generated-sources/proto-interfaces";

	private String targetFolder;

	private boolean generateInterfaces;
	private boolean generateAddInterfaceCodeGenerationFiles;
	private InterfaceProtocContext context;

	public static void main(String[] args) {
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

		String target;
		if (cmd.hasOption(targetOption.getOpt())) {
			target = cmd.getOptionValue(targetOption.getOpt());
		} else {
			target = DEFAULT_TARGET_FOLDER;
		}
		boolean generateInterfaces = cmd.hasOption(generateInterfacesOption.getOpt());
		boolean generateAddInterfaceCodeGenerationFiles = cmd.hasOption(implementInterfacesOption.getOpt());

		com.salesforce.jprotoc.ProtocPlugin.generate(
				Arrays.asList(new InterfaceProtocPlugin(target, generateInterfaces, generateAddInterfaceCodeGenerationFiles)), Arrays.asList(Xsd.baseType));
	}

	public InterfaceProtocPlugin(String targetFolder, boolean generateInterfaces, boolean generateAddInterfaceCodeGenerationFiles) {
		this.targetFolder = targetFolder;
		this.generateInterfaces = generateInterfaces;
		this.generateAddInterfaceCodeGenerationFiles = generateAddInterfaceCodeGenerationFiles;
	}

	@Override
	public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) {


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


}
