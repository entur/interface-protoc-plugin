package no.entur.protoc.interfaces;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLiteOrBuilder;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.compiler.PluginProtos;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import xsd.Xsd;

/**
 * Generate interfaces for a proto message and create CodeGeneratorResponse.File to add it to generated java class.
 */
public class MessageTypeHandler {
	private static final String JAVA_EXTENSION = ".java";

	private final InterfaceProtocContext context;
	private final DescriptorProtos.DescriptorProto messageTypeDesc;
	private final com.google.protobuf.DescriptorProtos.FileDescriptorProto fileDesc;

	private String protoFullPath;

	private String javaPackageName;
	private String interfaceFullName;

	private String builderInterfaceFullName;

	private String baseTypeFullPath;

	public MessageTypeHandler(InterfaceProtocContext context, DescriptorProtos.DescriptorProto messageTypeDesc, DescriptorProtos.FileDescriptorProto fileDesc) {
		this.context = context;
		this.messageTypeDesc = messageTypeDesc;
		this.fileDesc = fileDesc;
		init();
	}

	private void init() {
		protoFullPath = fileDesc.getPackage() + "." + messageTypeDesc.getName();
		javaPackageName = getJavaPackageName(fileDesc.getPackage(), messageTypeDesc.getName());

		interfaceFullName = javaPackageName + "." + getInterfaceName();
		builderInterfaceFullName = javaPackageName + "." + getBuilderInterfaceName(messageTypeDesc);

		String baseTypeOptionalsVal = messageTypeDesc.getOptions().getExtension(Xsd.baseType);

		if (!StringUtils.isEmpty(baseTypeOptionalsVal) && !baseTypeOptionalsVal.contains(".")) {
			// Optional value is a relative references within same file/package. Append package name from current file
			baseTypeOptionalsVal = fileDesc.getPackage() + "." + baseTypeOptionalsVal;
		}

		baseTypeFullPath = baseTypeOptionalsVal;

	}

	private boolean hasBaseType() {
		return !StringUtils.isEmpty(baseTypeFullPath) && !StringUtils.isEmpty(getBaseTypeJavaPackageName());
	}

	public void generateInterfaces() {

		createInterface();
		createBuilderInterface();
	}

	public List<PluginProtos.CodeGeneratorResponse.File> generateAddInterfaceCodeGenerationFiles() {
		List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();
		files.add(PluginProtos.CodeGeneratorResponse.File.newBuilder()
				.setName(getCodeGeneratorFileName())
				.setInsertionPoint("message_implements:" + protoFullPath)
				.setContent(interfaceFullName + ",")
				.build());

		files.add(PluginProtos.CodeGeneratorResponse.File.newBuilder()
				.setName(getCodeGeneratorFileName())
				.setInsertionPoint("builder_implements:" + protoFullPath)
				.setContent(builderInterfaceFullName + ",")
				.build());

		return files;
	}

	private List<DescriptorProtos.FieldDescriptorProto> getInterfaceFields(boolean includeInnerFields) {
		Set<String> baseTypeFields = getBaseTypeFields();

		return messageTypeDesc.getFieldList()
				.stream()
				.filter(field -> includeInnerFields || !isInnerMessage(field.getTypeName())) // TODO ignore inner types for now as these are not supported by
				// schema2proto
				.filter(field -> !baseTypeFields.contains(field.getName()) || isInnerMessage(field.getTypeName())) // Exclude fields inherited from base type,
																													// unless they are inner
				.collect(Collectors.toList());

	}

	private boolean isInnerMessage(String typeName) {
		return typeName.startsWith("." + protoFullPath + ".");
	}

	private Set<String> getBaseTypeFields() {
		DescriptorProtos.DescriptorProto baseTypeDesc = context.baseTypes.get(baseTypeFullPath);
		if (baseTypeDesc != null) {
			return baseTypeDesc.getFieldList().stream().map(DescriptorProtos.FieldDescriptorProto::getName).collect(Collectors.toSet());
		}

		return new HashSet<>();
	}

	private void createBuilderInterface() {
		List<MethodSpec> methods = new ArrayList<>();

		String builderInterfaceName = getBuilderInterfaceName(messageTypeDesc);
		TypeName builderInterfaceTypeName = ClassName.get(javaPackageName, builderInterfaceName);

		for (DescriptorProtos.FieldDescriptorProto field : getInterfaceFields(false)) {

			String fieldAsCamelCase = toPascalCase(field.getName());
			TypeName type = mapType(field, false);

			if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) {

				TypeName typeArgument = getSetterListTypeArgument(field, type);
				ParameterizedTypeName repeatedType = ParameterizedTypeName.get(ClassName.get(Iterable.class), typeArgument);

				MethodSpec getMethod = MethodSpec.methodBuilder("addAll" + fieldAsCamelCase)
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.addParameter(repeatedType, "values")
						.returns(builderInterfaceTypeName)
						.build();
				methods.add(getMethod);
			} else {
				MethodSpec setMethod = MethodSpec.methodBuilder("set" + fieldAsCamelCase)
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.addParameter(type, "value")
						.returns(builderInterfaceTypeName)
						.build();
				methods.add(setMethod);
			}
		}

		ClassName interfaceClassName = ClassName.get(javaPackageName, getInterfaceName());
		MethodSpec buildMethod = MethodSpec.methodBuilder("build").addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(interfaceClassName).build();
		methods.add(buildMethod);

		TypeName baseType;
		if (hasBaseType()) {
			String baseTypeBuilderInterfaceName = getBuilderInterfaceName(getBaseTypeMessageName());
			baseType = getBaseType(baseTypeBuilderInterfaceName);
		} else {
			baseType = getBaseBuilderClass();
		}

		writeInterface(builderInterfaceName, methods, baseType);
	}

	private TypeName getBaseType(String baseTypeInterfaceName) {
		String baseTypeJavaPackageName = getBaseTypeJavaPackageName();
		return ClassName.get(baseTypeJavaPackageName, baseTypeInterfaceName, new String[0]);
	}

	private void createInterface() {
		List<MethodSpec> methods = new ArrayList<>();

		for (DescriptorProtos.FieldDescriptorProto field : getInterfaceFields(true)) {

			String fieldAsCamelCase = toPascalCase(field.getName());
			TypeName type = mapType(field, context.useInterfacesForLocalReturnTypes);

			if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) {
				if (isMapEntry(field)) {
					// Ignore Map entries for now. Generated Java code does not result in getXXXMapEntryList, but instead getXXXMap
					continue;
				}

				TypeName typeArgument = getGetterListTypeArgument(field, type);

				ParameterizedTypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), typeArgument);
				MethodSpec getMethod = MethodSpec.methodBuilder("get" + fieldAsCamelCase + "List")
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.returns(listType)
						.build();
				methods.add(getMethod);
			} else {
				TypeName returnType;
				// Cannot use exact return type for inner types as these are generated per message and type is not inherited
				if (isInnerTypeInBaseType(field)) {
					returnType = getBaseMessageClass();
				} else {
					returnType = type;

				}
				MethodSpec getMethod = MethodSpec.methodBuilder("get" + fieldAsCamelCase)
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.returns(returnType)
						.build();
				methods.add(getMethod);

				if (field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE) {
					MethodSpec hasMethod = MethodSpec.methodBuilder("has" + fieldAsCamelCase)
							.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
							.returns(Boolean.TYPE)
							.build();
					methods.add(hasMethod);
				}
			}
		}
		String interfaceName = getInterfaceName();

		TypeName baseType;
		if (hasBaseType()) {
			String baseTypeInterfaceName = getInterfaceName(getBaseTypeMessageName());
			baseType = getBaseType(baseTypeInterfaceName);
		} else {
			baseType = getBaseMessageClass();

		}
		writeInterface(interfaceName, methods, baseType);
	}

	private ClassName getBaseMessageClass() {
		if (context.generateJavalite) {
			return ClassName.get(MessageLite.class);
		}
		return ClassName.get(Message.class);
	}

	private ClassName getBaseBuilderClass() {
		if (context.generateJavalite) {
			return ClassName.get(MessageLiteOrBuilder.class);
		}
		return ClassName.get(MessageOrBuilder.class);
	}

	private boolean isMapEntry(DescriptorProtos.FieldDescriptorProto field) {
		// TODO this may match non map inner types with name ending with 'Entry'. Find more robust way of detecting maps
		return isInnerMessage(field.getTypeName()) && field.getTypeName().endsWith("Entry");
	}

	private boolean isInnerTypeInBaseType(DescriptorProtos.FieldDescriptorProto field) {
		return isInnerMessage(field.getTypeName()) && field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE
				&& context.baseTypes.containsKey(protoFullPath);
	}

	private void writeInterface(String interfaceName, List<MethodSpec> methods, TypeName baseType) {

		TypeSpec.Builder typeSpec = TypeSpec.interfaceBuilder(interfaceName).addModifiers(Modifier.PUBLIC).addMethods(methods);

		if (baseType != null) {
			typeSpec.addSuperinterface(baseType);
		}

		JavaFile javaFile = JavaFile.builder(javaPackageName, typeSpec.build()).build();
		try {
			javaFile.writeTo(new File(context.targetFolder));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private TypeName mapType(DescriptorProtos.FieldDescriptorProto field, boolean useInterfaceForLocalTypes) {

		switch (field.getType()) {
		case TYPE_DOUBLE:
			return TypeName.DOUBLE;
		case TYPE_FLOAT:
			return TypeName.FLOAT;

		case TYPE_SFIXED64:
		case TYPE_SINT64:

		case TYPE_INT64:
		case TYPE_UINT64:
		case TYPE_FIXED64:
			return TypeName.LONG;

		case TYPE_UINT32:
		case TYPE_INT32:
		case TYPE_FIXED32:
		case TYPE_SFIXED32:
		case TYPE_SINT32:
			return TypeName.INT;
		case TYPE_BOOL:
			return TypeName.BOOLEAN;
		case TYPE_STRING:
			return ClassName.get(String.class);

		case TYPE_BYTES:
			return ClassName.get(ByteString.class);

		case TYPE_MESSAGE:
			return getClassNameFromTypeName(field.getTypeName(), useInterfaceForLocalTypes);

		case TYPE_ENUM:
			return getClassNameFromTypeName(field.getTypeName(), false);

		case TYPE_GROUP:
// Groups not supported in proto3
		}

		throw new IllegalArgumentException("Unable to map unknown type: " + field.getType());
	}

	public ClassName getClassNameFromTypeName(String typeName, boolean useInterfaceForLocalTypes) {
		String[] parts = typeName.split("\\.");
		String className = parts[parts.length - 1];
		String packageName = getJavaPackageName(typeName);
		if (useInterfaceForLocalTypes && context.isGeneratedType(typeName)) {
			// Refer to interface type generated proto classes
			className = className + "I";
		}
		return ClassName.get(packageName, className);
	}

	private TypeName getSetterListTypeArgument(DescriptorProtos.FieldDescriptorProto field, TypeName type) {
		TypeName typeArgument;

		boolean useWildcardGenericType = DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING != field.getType();

		if (useWildcardGenericType) {
			typeArgument = WildcardTypeName.subtypeOf(type.box());
		} else {
			typeArgument = type;
		}
		return typeArgument;
	}

	private TypeName getGetterListTypeArgument(DescriptorProtos.FieldDescriptorProto field, TypeName type) {
		TypeName typeArgument;
		boolean useWildcardGenericType = context.useInterfacesForLocalReturnTypes && isInterfacedField(field);
		if (isInnerTypeInBaseType(field)) {
			// Cannot use exact return type for inner types as these are generated per message and type is not inherited
			typeArgument = WildcardTypeName.subtypeOf(getBaseMessageClass());
		} else if (useWildcardGenericType) {
			typeArgument = WildcardTypeName.subtypeOf(type.box());
		} else if (type.isPrimitive()) {
			typeArgument = type.box();
		} else {
			typeArgument = type;
		}
		return typeArgument;
	}

	private boolean isInterfacedField(DescriptorProtos.FieldDescriptorProto field) {
		return field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE && context.isGeneratedType(field.getTypeName());
	}

	private String getJavaPackageName(String packageName, String messageName) {
		return getJavaPackageName("." + packageName + "." + messageName);
	}

	private String getJavaPackageName(String fullTypeName) {
		String javaTypeName = context.protoTypeMap.toJavaTypeName(fullTypeName);

		if (javaTypeName != null) {
			String[] parts = javaTypeName.split("\\.");
			String className = parts[parts.length - 1];
			// Remove class name part of full java type to get package name
			int startOfClassName = javaTypeName.lastIndexOf("." + className);
			String packageName = javaTypeName.substring(0, startOfClassName);

			if (packageName.startsWith(".")) {
				packageName = packageName.substring(1);
			}

			return packageName;
		}
		return null;
	}

	private String getInterfaceName() {
		return getInterfaceName(messageTypeDesc.getName());
	}

	private String getInterfaceName(String messageName) {
		return messageName + "I";
	}

	private String getBuilderInterfaceName(DescriptorProtos.DescriptorProto messageTypeDesc) {
		return getBuilderInterfaceName(messageTypeDesc.getName());
	}

	private String getBuilderInterfaceName(String messageName) {
		return messageName + "BuilderI";
	}

	private String toPascalCase(String snakeCaseString) {
		StringBuilder sb = new StringBuilder(snakeCaseString);

		sb.replace(0, 1, String.valueOf(Character.toUpperCase(sb.charAt(0))));

		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == '_') {
				sb.deleteCharAt(i);
				sb.replace(i, i + 1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
			}
		}
		return sb.toString();
	}

	private String getCodeGeneratorFileName() {
		String javaPackage;
		if (fileDesc.getOptions().hasJavaPackage()) {
			javaPackage = fileDesc.getOptions().getJavaPackage();
		} else {
			javaPackage = fileDesc.getPackage();
		}
		return javaPackage.replace(".", "/") + "/" + messageTypeDesc.getName() + JAVA_EXTENSION;
	}

	private String getBaseTypeJavaPackageName() {
		return getJavaPackageName("." + baseTypeFullPath);
	}

	private String getBaseTypeMessageName() {
		String[] parts = baseTypeFullPath.split("\\.");
		return parts[parts.length - 1];
	}
}
