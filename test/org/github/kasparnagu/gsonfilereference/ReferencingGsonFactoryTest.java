package org.github.kasparnagu.gsonfilereference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class ReferencingGsonFactoryTest {
	public static class TestClassB {

		String c;
	}

	public static class TestClassA {

		int a;
		String b;
		int c[];
		TestClassB tb_1;
		TestClassB tb_2;
	}

	@Test
	public void noReferenceSerialize() {
		TestClassA t = new TestClassA();
		t.a = 4;
		t.b = "aja";
		t.c = new int[] { 1, 2, 3 };
		t.tb_1 = new TestClassB();
		t.tb_1.c = "lkj";
		String json = ReferencingGsonFactory.getBuilder().create().toJson(t, TestClassA.class);
		assertEquals("{\"a\":4,\"b\":\"aja\",\"c\":[1,2,3],\"tb_1\":{\"c\":\"lkj\"}}",json);
	}

	@Test
	public void noReferenzeDeserialize(){
		String json = "{a:1, b:\"a\", c:[3,1,4], tb_1:{c:null}, tb_2:{c:\"m\"}}";
		TestClassA tt = ReferencingGsonFactory.getBuilder().create().fromJson(json, TestClassA.class);
		assertEquals(1, tt.a);
		assertEquals("a", tt.b);
		assertArrayEquals(new int[]{3,1,4}, tt.c);
		assertNull(tt.tb_1.c);
		assertEquals("m",tt.tb_2.c);
	}
	
	@Test
	public void referencedDeserialize() throws Exception{
		Path jsonPath = Paths.get(ReferencingGsonFactoryTest.class.getResource("test1.json").toURI());
		TestClassA tt = ReferencingGsonFactory.getBuilder()
				.create().fromJson(ReferencingGsonFactory.getFileReader(jsonPath), TestClassA.class);
		assertEquals(31415,tt.a);
		assertEquals("test string from test_string.txt\n",tt.b);
		assertArrayEquals(new int[]{3,1,4,1,5,31415}, tt.c);
		assertNull(tt.tb_1.c);
		assertEquals("b from test_testclassb.json",tt.tb_2.c);
	}
	
	@Test
	public void referenceDeserializeFromString() throws Exception{
		Path jsonPath = Paths.get(ReferencingGsonFactoryTest.class.getResource("test1.json").toURI());
		String baseUri = jsonPath.getParent().toUri().toString();
		String json = "{a:33, b:\""+baseUri+"/subdir/test_number.txt\", c:[2,\""+baseUri+"/subdir/test_number.txt\"], tb_1:\""+baseUri+"/test_testclassb.json\", tb_2:\""+baseUri+"/test_testclassb.json\"}";
		TestClassA tt = ReferencingGsonFactory.getBuilder()
				.create().fromJson(json, TestClassA.class);
		assertEquals(33,tt.a);
		assertEquals("31415\n",tt.b);
		assertArrayEquals(new int[]{2,31415}, tt.c);
		assertEquals("b from test_testclassb.json",tt.tb_1.c);
		assertEquals("b from test_testclassb.json",tt.tb_2.c);
	}

}
