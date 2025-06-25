package no.entur.protoc.interfaces;

/*-
 * #%L
 * interface-protoc-plugin
 * %%
 * Copyright (C) 2019 - 2025 Entur
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * 
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import xsd.Xsd;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.ProtoTypeMap;

public class InterfaceProtocContext {
	public final boolean generateJavalite;
	public final String targetFolder;
	public final ProtoTypeMap protoTypeMap;
	public final Map<String, DescriptorProtos.DescriptorProto> baseTypes;
	public final Set<String> messageTypes;

	// Disabled interfaces as return type for generated classes at it requires use of wildcard types which is strongly discourage + does not work with esper.
	public final boolean useInterfacesForLocalReturnTypes = false;

	public InterfaceProtocContext(String targetFolder, PluginProtos.CodeGeneratorRequest request, boolean generateJavalite) {
		this.targetFolder = targetFolder;
		this.generateJavalite = generateJavalite;
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
