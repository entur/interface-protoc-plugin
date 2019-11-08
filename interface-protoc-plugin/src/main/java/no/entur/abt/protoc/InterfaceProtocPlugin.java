package no.entur.abt.protoc;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import xsd.Xsd;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;

public class InterfaceProtocPlugin extends com.salesforce.jprotoc.Generator {

	private static final String DEFAULT_TARGET_FOLDER = "target/generated-sources/proto-interfaces";
	private String targetFolder;

	private ProtoTypeMap protoTypeMap;

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
		protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());

		return request.getProtoFileList()
				.stream()
				.filter(file -> request.getFileToGenerateList().contains(file.getName()))
				.map(this::handleProtoFile)

				.flatMap(Function.identity())
				.collect(Collectors.toList());
	}

	private Stream<PluginProtos.CodeGeneratorResponse.File> handleProtoFile(final DescriptorProtos.FileDescriptorProto fileDesc) {

		return Stream.of(fileDesc.getMessageTypeList()
				.stream()
				.map(messageType -> new MessageTypeHandler(targetFolder, protoTypeMap, messageType, fileDesc))
				.map(MessageTypeHandler::process)
				.flatMap(List::stream)).flatMap(Function.identity());
	}

}
