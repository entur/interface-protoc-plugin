package no.entur.protoc.interfaces;

import java.util.Set;

import com.salesforce.jprotoc.ProtoTypeMap;

public class InterfaceProtocContext {

	public final String targetFolder;
	public final ProtoTypeMap protoTypeMap;
	public final Set<String> baseTypes;

	public InterfaceProtocContext(String targetFolder, ProtoTypeMap protoTypeMap, Set<String> baseTypes) {
		this.targetFolder = targetFolder;
		this.protoTypeMap = protoTypeMap;
		this.baseTypes = baseTypes;
	}

}
