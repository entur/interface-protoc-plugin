package no.entur.protoc.interfaces;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.ProtoTypeMap;

import xsd.Xsd;

public class InterfaceProtocContext {

	public final String targetFolder;
	public final ProtoTypeMap protoTypeMap;
	public final Map<String, DescriptorProtos.DescriptorProto> baseTypes;
	public final Set<String> messageTypes;

	public InterfaceProtocContext(String targetFolder, PluginProtos.CodeGeneratorRequest request) {
		this.targetFolder = targetFolder;

		this.protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());
		this.baseTypes = getAllBaseTypes(request);
		this.messageTypes = request.getProtoFileList()
				.stream()
				.filter(file -> request.getFileToGenerateList().contains(file.getName()))
				.map(file -> file.getMessageTypeList().stream().map(desc -> "." + file.getPackage() + "." + desc.getName()))
				.flatMap(Function.identity())
				.collect(Collectors.toSet());

	}

	public boolean isGeneratedType(String typeName) {
		return messageTypes.contains(typeName);
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

}
