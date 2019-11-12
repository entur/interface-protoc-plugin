package no.entur.protoc.interfaces;

import java.util.Map;

import com.google.protobuf.DescriptorProtos;
import com.salesforce.jprotoc.ProtoTypeMap;

public class InterfaceProtocContext {

	public final String targetFolder;
	public final ProtoTypeMap protoTypeMap;
	public final Map<String, DescriptorProtos.DescriptorProto> baseTypes;

	public InterfaceProtocContext(String targetFolder, ProtoTypeMap protoTypeMap, Map<String, DescriptorProtos.DescriptorProto> baseTypes) {
		this.targetFolder = targetFolder;
		this.protoTypeMap = protoTypeMap;
		this.baseTypes = baseTypes;
	}

}
