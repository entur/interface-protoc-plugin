package no.entur.protoc.interfaces;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
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
	public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) {
		ProtoTypeMap protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());

		Map<String, DescriptorProtos.DescriptorProto> baseTypes = getAllBaseTypes(request);

		context = new InterfaceProtocContext(targetFolder, protoTypeMap, baseTypes);

		return request.getProtoFileList()
				.stream()
				.filter(file -> request.getFileToGenerateList().contains(file.getName()))
				.map(this::handleProtoFile)

				.flatMap(Function.identity())
				.collect(Collectors.toList());
	}

	private Map<String, DescriptorProtos.DescriptorProto> getAllBaseTypes(PluginProtos.CodeGeneratorRequest request) {
		List<String> baseTypeFullNames = request.getProtoFileList()
				.stream()
				.map(this::getAllBaseTypesForFile)
				.flatMap(Function.identity())
				.collect(Collectors.toList());

		Map<String, DescriptorProtos.DescriptorProto> baseTypes = new HashMap<>();
		for (String baseTypeFullName : baseTypeFullNames) {
			baseTypes.put(baseTypeFullName, findDescriptor(request, baseTypeFullName));
		}
		return baseTypes;
	}

	private DescriptorProtos.DescriptorProto findDescriptor(PluginProtos.CodeGeneratorRequest request, String messageFullName) {

		return request.getProtoFileList()
				.stream()
				.map(file -> file.getMessageTypeList()
						.stream()
						.filter(messageType -> messageFullName.equals(file.getPackage() + "." + messageType.getName()))
						.findFirst()
						.orElse(null))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);

	}

	private Stream<String> getAllBaseTypesForFile(DescriptorProtos.FileDescriptorProto fileDesc) {
		String packageName = fileDesc.getPackage();
		return fileDesc.getMessageTypeList().stream().map(messageTypeDesc -> getFullBaseTypeName(packageName, messageTypeDesc)).filter(Objects::nonNull);
	}

	private String getFullBaseTypeName(String packageName, DescriptorProtos.DescriptorProto messageTypeDesc) {

		String optionVal = messageTypeDesc.getOptions().getExtension(Xsd.baseType);

		if (!StringUtils.isEmpty(optionVal) && !optionVal.contains(".")) {
			// Optional value is a relative references within same file/package. Append package name from current file
			return packageName + "." + optionVal;
		}
		return optionVal;
	}

	private Stream<PluginProtos.CodeGeneratorResponse.File> handleProtoFile(final DescriptorProtos.FileDescriptorProto fileDesc) {

		return Stream.of(fileDesc.getMessageTypeList()
				.stream()
				.map(messageType -> new MessageTypeHandler(context, messageType, fileDesc))
				.map(MessageTypeHandler::process)
				.flatMap(List::stream)).flatMap(Function.identity());
	}

}
