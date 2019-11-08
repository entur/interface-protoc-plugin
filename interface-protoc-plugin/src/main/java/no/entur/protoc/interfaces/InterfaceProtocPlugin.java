package no.entur.protoc.interfaces;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;

import xsd.Xsd;

public class InterfaceProtocPlugin extends com.salesforce.jprotoc.Generator {

	private static final String DEFAULT_TARGET_FOLDER = "target/generated-sources/proto-interfaces";

	private String targetFolder;
	private InterfaceProtocContext context;

	public static void main(String[] args) {
		String target = DEFAULT_TARGET_FOLDER;
		if (args.length > 0) {
			target = args[0];
		}

		com.salesforce.jprotoc.ProtocPlugin.generate(Arrays.asList(new InterfaceProtocPlugin(target)), Arrays.asList(Xsd.baseType));
	}

	public InterfaceProtocPlugin(String targetFolder) {
		this.targetFolder = targetFolder;
	}

	@Override
	public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
		ProtoTypeMap protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());

		Set<String> baseTypes = getAllBaseTypes(request);

		context = new InterfaceProtocContext(targetFolder, protoTypeMap, baseTypes);

		return request.getProtoFileList()
				.stream()
				.filter(file -> request.getFileToGenerateList().contains(file.getName()))
				.map(this::handleProtoFile)

				.flatMap(Function.identity())
				.collect(Collectors.toList());
	}

	private Set<String> getAllBaseTypes(PluginProtos.CodeGeneratorRequest request) {
		return request.getProtoFileList()
				.stream()
				.map(DescriptorProtos.FileDescriptorProto::getMessageTypeList)
				.flatMap(List::stream)
				.map(messageTypeDesc -> messageTypeDesc.getOptions().getExtension(Xsd.baseType))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private Stream<PluginProtos.CodeGeneratorResponse.File> handleProtoFile(final DescriptorProtos.FileDescriptorProto fileDesc) {

		return Stream.of(fileDesc.getMessageTypeList()
				.stream()
				.map(messageType -> new MessageTypeHandler(context, messageType, fileDesc))
				.map(MessageTypeHandler::process)
				.flatMap(List::stream)).flatMap(Function.identity());
	}

}
