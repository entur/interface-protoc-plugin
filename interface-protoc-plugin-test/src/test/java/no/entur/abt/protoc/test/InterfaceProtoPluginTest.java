package no.entur.abt.protoc.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import no.entur.abt.proto.plugin.test.BottomLevel;
import no.entur.abt.proto.plugin.test.TopLevelI;
import no.entur.abt.proto.plugin.test.TopLevelIBuilder;

public class InterfaceProtoPluginTest {

	@Test
	public void testInterfaces() {
		BottomLevel.Builder builder = BottomLevel.newBuilder();
		populateTopLevelFields(builder);
		BottomLevel bottomLevel = builder.build();

		TopLevelI topLevelI = bottomLevel;

		Assertions.assertEquals(bottomLevel.getBoolVal(), topLevelI.getBoolVal());
		Assertions.assertEquals(bottomLevel.getStringVal(), topLevelI.getStringVal());
		Assertions.assertEquals(bottomLevel.getInt64Val(), topLevelI.getInt64Val());
	}

	private void populateTopLevelFields(TopLevelIBuilder builder) {
		builder.setBoolVal(true).setInt64Val(345).setStringVal("testString");
	}
}
