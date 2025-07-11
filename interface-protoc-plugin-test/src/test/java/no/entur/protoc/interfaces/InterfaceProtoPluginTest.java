package no.entur.protoc.interfaces;

/*-
 * #%L
 * interface-protoc-plugin-test
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import no.entur.protoc.interfaces.package1.BottomLevel;
import no.entur.protoc.interfaces.package1.BottomLevelI;
import no.entur.protoc.interfaces.package1.MidLevelBuilderI;
import no.entur.protoc.interfaces.package1.MidLevelI;
import no.entur.protoc.interfaces.package2.EnumType;
import no.entur.protoc.interfaces.package2.SimpleType;
import no.entur.protoc.interfaces.package2.TopLevelBuilderI;
import no.entur.protoc.interfaces.package2.TopLevelI;

public class InterfaceProtoPluginTest {

	@Test
	public void testInterfaces() {
		BottomLevel.Builder builder = BottomLevel.newBuilder();
		populateTopLevelFields(builder);
		populateMidLevelFields(builder);
		String bottomLevelStringVal = "bottomLevelString";
		builder.setBottomLevelStringVal(bottomLevelStringVal);

		BottomLevel bottomLevel = builder.build();

		BottomLevelI bottomLevelI = bottomLevel;
		TopLevelI topLevelI = bottomLevel;
		MidLevelI midLevelI = bottomLevel;
		assertTopLevelFields(bottomLevel, topLevelI);
		assertMidLevelFields(bottomLevel, midLevelI);
		Assertions.assertEquals(bottomLevelStringVal, bottomLevel.getBottomLevelStringVal());

		BottomLevel.InnerType innerType = bottomLevelI.getInnerTypeVal();
		List<BottomLevel.InnerType> innerTypeRepeated = bottomLevelI.getInnerTypeRepeatedValList();
	}

	private void assertMidLevelFields(BottomLevel org, MidLevelI midLevelI) {
		Assertions.assertEquals(org.getMidLevelStringVal(), midLevelI.getMidLevelStringVal());

		Message innerType = midLevelI.getInnerTypeVal();
		List<? extends Message> innerTypeRepeated = midLevelI.getInnerTypeRepeatedValList();

		Assertions.assertEquals(org.hasTopLevelMessageVal(), midLevelI.hasTopLevelMessageVal());

	}

	private void assertTopLevelFields(BottomLevel org, TopLevelI topLevelI) {

		Assertions.assertEquals(org.getTopLevelBoolVal(), topLevelI.getTopLevelBoolVal());
		Assertions.assertEquals(org.getTopLevelBytesVal(), topLevelI.getTopLevelBytesVal());
		Assertions.assertEquals(org.getTopLevelDoubleVal(), topLevelI.getTopLevelDoubleVal());
		Assertions.assertEquals(org.getTopLevelEnumVal(), topLevelI.getTopLevelEnumVal());
		Assertions.assertEquals(org.getTopLevelFixed32Val(), topLevelI.getTopLevelFixed32Val());
		Assertions.assertEquals(org.getTopLevelFixed64Val(), topLevelI.getTopLevelFixed64Val());
		Assertions.assertEquals(org.getTopLevelInt32Val(), topLevelI.getTopLevelInt32Val());
		Assertions.assertEquals(org.getTopLevelInt64Val(), topLevelI.getTopLevelInt64Val());
		Assertions.assertEquals(org.getTopLevelSfixed32Val(), topLevelI.getTopLevelSfixed32Val());
		Assertions.assertEquals(org.getTopLevelSfixed64Val(), topLevelI.getTopLevelSfixed64Val());
		Assertions.assertEquals(org.getTopLevelSint32Val(), topLevelI.getTopLevelSint32Val());
		Assertions.assertEquals(org.getTopLevelSint64Val(), topLevelI.getTopLevelSint64Val());
		Assertions.assertEquals(org.getTopLevelUint64Val(), topLevelI.getTopLevelUint64Val());
		Assertions.assertEquals(org.getTopLevelFloatVal(), topLevelI.getTopLevelFloatVal());
		Assertions.assertEquals(org.getTopLevelStringVal(), topLevelI.getTopLevelStringVal());
		Assertions.assertEquals(org.getTopLevelMessageVal().getSimpleTypeStringVal(), org.getTopLevelMessageVal().getSimpleTypeStringVal());

		Assertions.assertEquals(org.getTopLevelOneOfDoubleVal(), topLevelI.getTopLevelOneOfDoubleVal());
		Assertions.assertEquals(org.getTopLevelOneOfStringVal(), topLevelI.getTopLevelOneOfStringVal());

		Assertions.assertEquals(org.getTopLevelRepeatedBoolValList(), topLevelI.getTopLevelRepeatedBoolValList());
		Assertions.assertEquals(org.getTopLevelRepeatedBytesValList(), topLevelI.getTopLevelRepeatedBytesValList());
		Assertions.assertEquals(org.getTopLevelRepeatedDoubleValList(), topLevelI.getTopLevelRepeatedDoubleValList());
		Assertions.assertEquals(org.getTopLevelRepeatedEnumValList(), topLevelI.getTopLevelRepeatedEnumValList());
		Assertions.assertEquals(org.getTopLevelRepeatedFixed32ValList(), topLevelI.getTopLevelRepeatedFixed32ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedFixed64ValList(), topLevelI.getTopLevelRepeatedFixed64ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedInt32ValList(), topLevelI.getTopLevelRepeatedInt32ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedInt64ValList(), topLevelI.getTopLevelRepeatedInt64ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedSfixed32ValList(), topLevelI.getTopLevelRepeatedSfixed32ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedSfixed64ValList(), topLevelI.getTopLevelRepeatedSfixed64ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedSint32ValList(), topLevelI.getTopLevelRepeatedSint32ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedSint64ValList(), topLevelI.getTopLevelRepeatedSint64ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedUint64ValList(), topLevelI.getTopLevelRepeatedUint64ValList());
		Assertions.assertEquals(org.getTopLevelRepeatedFloatValList(), topLevelI.getTopLevelRepeatedFloatValList());
		Assertions.assertEquals(org.getTopLevelRepeatedStringValList(), topLevelI.getTopLevelRepeatedStringValList());
		Assertions.assertEquals(org.getTopLevelRepeatedMessageValList().stream().map(SimpleType::getSimpleTypeStringVal).collect(Collectors.toList()),
				org.getTopLevelRepeatedMessageValList().stream().map(SimpleType::getSimpleTypeStringVal).collect(Collectors.toList()));

		Assertions.assertEquals(org.hasTopLevelMessageVal(), topLevelI.hasTopLevelMessageVal());
	}

	private void populateMidLevelFields(MidLevelBuilderI builder) {
		builder.setMidLevelStringVal("midLevelStringVal");
	}

	private void populateTopLevelFields(TopLevelBuilderI builder) {
		builder.setTopLevelBoolVal(true)
				.setTopLevelDoubleVal(55.5)
				.setTopLevelEnumVal(EnumType.ENUM_VALUE_2)
				.setTopLevelFixed32Val(321)
				.setTopLevelFixed64Val(645)
				.setTopLevelFloatVal(3.3f)
				.setTopLevelInt32Val(326)
				.setTopLevelInt64Val(649)
				.setTopLevelSfixed32Val(324)
				.setTopLevelSfixed64Val(641)
				.setTopLevelSint32Val(322)
				.setTopLevelSint64Val(642)
				.setTopLevelMessageVal(SimpleType.newBuilder().setSimpleTypeStringVal("simpleStringVal").build())
				.setTopLevelUint64Val(645)
				.setTopLevelBytesVal(ByteString.copyFromUtf8("topLevelByteVal"))
				.setTopLevelStringVal("topLevelString")

				.setTopLevelOneOfDoubleVal(56.7)

				.addAllTopLevelRepeatedBoolVal(Arrays.asList(Boolean.FALSE, Boolean.TRUE))
				.addAllTopLevelRepeatedDoubleVal(Arrays.asList(55.5))
				.addAllTopLevelRepeatedEnumVal(Arrays.asList(EnumType.ENUM_VALUE_2))
				.addAllTopLevelRepeatedFixed32Val(Arrays.asList(321))
				.addAllTopLevelRepeatedFixed64Val(Arrays.asList(645l))
				.addAllTopLevelRepeatedFloatVal(Arrays.asList(3.3f))
				.addAllTopLevelRepeatedInt32Val(Arrays.asList(326))
				.addAllTopLevelRepeatedInt64Val(Arrays.asList(649l))
				.addAllTopLevelRepeatedSfixed32Val(Arrays.asList(324))
				.addAllTopLevelRepeatedSfixed64Val(Arrays.asList(641l))
				.addAllTopLevelRepeatedSint32Val(Arrays.asList(322))
				.addAllTopLevelRepeatedSint64Val(Arrays.asList(642l))
				.addAllTopLevelRepeatedMessageVal(Arrays.asList(SimpleType.newBuilder().setSimpleTypeStringVal("simpleStringVal").build()))
				.addAllTopLevelRepeatedUint64Val(Arrays.asList(645l))
				.addAllTopLevelRepeatedBytesVal(Arrays.asList(ByteString.copyFromUtf8("topLevelByteVal")))
				.addAllTopLevelRepeatedStringVal(Arrays.asList("topLevelString"));

	}
}
