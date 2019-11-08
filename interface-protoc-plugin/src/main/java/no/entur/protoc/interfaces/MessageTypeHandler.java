package no.entur.protoc.interfaces;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
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
	private final static String JAVA_EXTENSION = ".java";

	private final InterfaceProtocContext context;
	private final DescriptorProtos.DescriptorProto messageTypeDesc;
	private final com.google.protobuf.DescriptorProtos.FileDescriptorProto fileDesc;

	private String protoFullPath;

	private String javaPackageName;
	private String interfaceFullName;

	private String builderInterfaceFullName;

	private String baseType;

	public MessageTypeHandler(InterfaceProtocContext context, DescriptorProtos.DescriptorProto messageTypeDesc, DescriptorProtos.FileDescriptorProto fileDesc) {
		this.context = context;
		this.messageTypeDesc = messageTypeDesc;
		this.fileDesc = fileDesc;

	}

	private void init() {
		protoFullPath = fileDesc.getPackage() + "." + messageTypeDesc.getName();
		javaPackageName = getJavaPackageName(fileDesc.getPackage(), messageTypeDesc.getName());

		interfaceFullName = javaPackageName + "." + getInterfaceName();
		builderInterfaceFullName = javaPackageName + "." + getBuilderInterfaceName(messageTypeDesc);
		baseType = StringUtils.trimToNull(messageTypeDesc.getOptions().getExtension(Xsd.baseType));
	}

	private boolean isBaseType() {
		return context.baseTypes.contains(messageTypeDesc.getName());
	}

	private boolean hasBaseType() {
		return !StringUtils.isEmpty(baseType);
	}

	public List<PluginProtos.CodeGeneratorResponse.File> process() {
		init();

		if (isBaseType()) {
			createInterface();
			createBuilderInterface();
			return createCodeGeneratorResponseFiles(interfaceFullName, builderInterfaceFullName);
		} else if (hasBaseType()) {
			String baseTypeJavaPackageName = getJavaPackageName(fileDesc.getPackage(), baseType);
			String baseTypeInterfaceName = baseTypeJavaPackageName + "." + getInterfaceName(baseType);
			String baseTypeBuilderInterfaceName = baseTypeJavaPackageName + "." + getBuilderInterfaceName(baseType);
			return createCodeGeneratorResponseFiles(baseTypeInterfaceName, baseTypeBuilderInterfaceName);
		}
		return new ArrayList<>();
	}

	private List<PluginProtos.CodeGeneratorResponse.File> createCodeGeneratorResponseFiles(String messageImplementsInterface,
			String builderImplementsInterface) {
		List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();
		files.add(PluginProtos.CodeGeneratorResponse.File.newBuilder()
				.setName(getCodeGeneratorFileName())
				.setInsertionPoint("message_implements:" + protoFullPath)
				.setContent(messageImplementsInterface + ",")
				.build());

		files.add(PluginProtos.CodeGeneratorResponse.File.newBuilder()
				.setName(getCodeGeneratorFileName())
				.setInsertionPoint("builder_implements:" + protoFullPath)
				.setContent(builderImplementsInterface + ",")
				.build());

		return files;
	}

	private void createBuilderInterface() {
		List<MethodSpec> methods = new ArrayList<>();

		String builderInterfaceName = getBuilderInterfaceName(messageTypeDesc);
		TypeName builderInterfaceTypeName = ClassName.get(javaPackageName, builderInterfaceName, new String[0]);

		for (DescriptorProtos.FieldDescriptorProto field : messageTypeDesc.getFieldList()) {

			String fieldAsCamelCase = toPascalCase(field.getName());
			TypeName type = mapType(field);

			if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) {
				TypeName typeArgument;
				if (field.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING) {
					typeArgument = type.box();
				} else {
					typeArgument = WildcardTypeName.subtypeOf(type.box());
				}
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

		ClassName interfaceClassName = ClassName.get(javaPackageName, getInterfaceName(), new String[0]);
		MethodSpec buildMethod = MethodSpec.methodBuilder("build").addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).returns(interfaceClassName).build();
		methods.add(buildMethod);

		String baseTypeBuilderInterfaceName = getBuilderInterfaceName(baseType);

		TypeName baseType = getBaseType(baseTypeBuilderInterfaceName);
		writeInterface(builderInterfaceName, methods, baseType);
	}

	private TypeName getBaseType(String baseTypeInterfaceName) {
		TypeName baseTypeName = null;
		if (baseType != null) {
			// TODO base type must include package name
			String baseTypeJavaPackageName = getJavaPackageName(fileDesc.getPackage(), baseType);
			if (baseTypeJavaPackageName != null) {

				baseTypeName = ClassName.get(baseTypeJavaPackageName, baseTypeInterfaceName, new String[0]);
			}
		}
		return baseTypeName;
	}

	private void createInterface() {
		List<MethodSpec> methods = new ArrayList<>();

		for (DescriptorProtos.FieldDescriptorProto field : messageTypeDesc.getFieldList()) {

			String fieldAsCamelCase = toPascalCase(field.getName());
			TypeName type = mapType(field);

			if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) {
				ParameterizedTypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), type.box());
				MethodSpec getMethod = MethodSpec.methodBuilder("get" + fieldAsCamelCase + "List")
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.returns(listType)
						.build();
				methods.add(getMethod);
			} else {
				MethodSpec getMethod = MethodSpec.methodBuilder("get" + fieldAsCamelCase)
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.returns(type)
						.build();
				methods.add(getMethod);
			}
		}
		String interfaceName = getInterfaceName();

		String baseTypeInterfaceName = getInterfaceName(baseType);
		TypeName baseType = getBaseType(baseTypeInterfaceName);

		writeInterface(interfaceName, methods, baseType);
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
			log(e.getMessage());

			throw new RuntimeException(e);
		}
	}

	private TypeName mapType(DescriptorProtos.FieldDescriptorProto field) {

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
		case TYPE_ENUM:
			return getClassNameFromTypeName(field.getTypeName());

		case TYPE_GROUP:
// Groups not supported in proto3
		}

		throw new IllegalArgumentException("Unable to map unknown type: " + field.getType());
	}

	public ClassName getClassNameFromTypeName(String typeName) {
		String[] parts = typeName.split("\\.");
		String className = parts[parts.length - 1];
		String packageName = getJavaPackageName(typeName);

		return ClassName.get(packageName, className, new String[0]);
	}

	private String getJavaPackageName(String packageName, String messageName) {
		return getJavaPackageName("." + packageName + "." + messageName);
	}

	private String getJavaPackageName(String fullTypeName) {
		String javaTypeName = context.protoTypeMap.toJavaTypeName(fullTypeName);

		if (javaTypeName != null) {
			String[] parts = javaTypeName.split("\\.");
			String className = parts[parts.length - 1];
			String packageName = javaTypeName.replace("." + className, "");
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
		return messageName + "IBuilder";
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

	public static void log(String msg) {
		System.out.println(msg
				+ "                                                                                                                                                                              ");
	}
}
