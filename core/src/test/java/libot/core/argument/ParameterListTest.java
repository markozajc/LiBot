package libot.core.argument;

import static libot.core.argument.ParameterList.Parameter.mandatory;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.argument.ParameterList.Parameter.ParameterType;

class ParameterListTest {

	@Nonnull private static final MandatoryParameter POS_FIRST = mandatory(ParameterType.POSITIONAL, "p1", "");
	@Nonnull private static final MandatoryParameter POS_SECOND = mandatory(ParameterType.POSITIONAL, "p2", "");
	@Nonnull private static final MandatoryParameter POS_THIRD = mandatory(ParameterType.POSITIONAL, "p3", "");
	@Nonnull private static final MandatoryParameter NAM_FIRST = mandatory(ParameterType.NAMED, "n1", "");
	@Nonnull private static final MandatoryParameter NAM_SECOND = mandatory(ParameterType.NAMED, "n2", "");
	@Nonnull private static final MandatoryParameter NAM_THIRD = mandatory(ParameterType.NAMED, "n3", "");

	@Test
	void testEmptyOf() {
		assertSame(ParameterList.EMPTY, ParameterList.of());
	}

	@Test
	void testEmpty() {
		assertSame(ArgumentList.EMPTY, ParameterList.of(POS_FIRST).parse(""));
	}

	@Test
	void testPositionalSingle() {
		var args = ParameterList.of(POS_FIRST).parse("argument");
		assertEquals("argument", args.get(POS_FIRST).value());
	}

	@Test
	void testPositionalSingleMultiword() {
		var args = ParameterList.of(POS_FIRST).parse("argu ment");
		assertEquals("argu ment", args.get(POS_FIRST).value());
	}

	@Test
	void testPositionalSingleMultiwordExtraSpaces() {
		var args = ParameterList.of(POS_FIRST).parse("argu   ment");
		assertEquals("argu   ment", args.get(POS_FIRST).value());
	}

	@Test
	void testPositionalMultiple() {
		var args = ParameterList.of(POS_FIRST, POS_SECOND, POS_THIRD).parse("ar gu ment");
		assertEquals("ar", args.get(POS_FIRST).value());
		assertEquals("gu", args.get(POS_SECOND).value());
		assertEquals("ment", args.get(POS_THIRD).value());
	}

	@Test
	void testPositionalMultipleMultiword() {
		var args = ParameterList.of(POS_FIRST, POS_SECOND, POS_THIRD).parse("ar gu men t");
		assertEquals("ar", args.get(POS_FIRST).value());
		assertEquals("gu", args.get(POS_SECOND).value());
		assertEquals("men t", args.get(POS_THIRD).value());
	}

	@Test
	void testPositionalMultipleMultiwordExtraSpaces() {
		var args = ParameterList.of(POS_FIRST, POS_SECOND, POS_THIRD).parse("ar   gu   men   t");
		assertEquals("ar", args.get(POS_FIRST).value());
		assertEquals("gu", args.get(POS_SECOND).value());
		assertEquals("men   t", args.get(POS_THIRD).value());
	}

	@Test
	void testNamedSingle() {
		var args = ParameterList.of(NAM_FIRST).parse("--n1 argument");
		assertEquals("argument", args.get(NAM_FIRST).value());
	}

	@Test
	void testNamedSingleExtraSpaces() {
		var args = ParameterList.of(NAM_FIRST).parse("--n1   argument");
		assertEquals("argument", args.get(NAM_FIRST).value());
	}

	@Test
	void testNamedMultiple() {
		var args = ParameterList.of(NAM_FIRST, NAM_SECOND, NAM_THIRD).parse("--n1 ar --n2 gu --n3 ment");
		assertEquals("ar", args.get(NAM_FIRST).value());
		assertEquals("gu", args.get(NAM_SECOND).value());
		assertEquals("ment", args.get(NAM_THIRD).value());
	}

	@Test
	void testNamedMultipleExtraSpaces() {
		var args = ParameterList.of(NAM_FIRST, NAM_SECOND, NAM_THIRD).parse("--n1   ar   --n2   gu   --n3   ment");
		assertEquals("ar", args.get(NAM_FIRST).value());
		assertEquals("gu", args.get(NAM_SECOND).value());
		assertEquals("ment", args.get(NAM_THIRD).value());
	}

	@Test
	void testMixSingle() {
		var args = ParameterList.of(NAM_FIRST, POS_FIRST).parse("--n1 argu men t");
		assertEquals("argu", args.get(NAM_FIRST).value());
		assertEquals("men t", args.get(POS_FIRST).value());
	}

	@Test
	void testMixSingleExtraSpaces() {
		var args = ParameterList.of(NAM_FIRST, POS_FIRST).parse("--n1   argu   men   t");
		assertEquals("argu", args.get(NAM_FIRST).value());
		assertEquals("men   t", args.get(POS_FIRST).value());
	}

	@Test
	void testMixSingleReverse() {
		var args = ParameterList.of(NAM_FIRST, POS_FIRST).parse("men t --n1 argu");
		assertEquals("argu", args.get(NAM_FIRST).value());
		assertEquals("men t", args.get(POS_FIRST).value());
	}

	@Test
	void testMixSingleReverseExtraSpaces() {
		var args = ParameterList.of(NAM_FIRST, POS_FIRST).parse("men   t   --n1   argu");
		assertEquals("argu", args.get(NAM_FIRST).value());
		assertEquals("men   t", args.get(POS_FIRST).value());
	}

	@Test
	void testMixDouble() {
		var args =
			ParameterList.of(NAM_FIRST, NAM_SECOND, POS_FIRST, POS_SECOND).parse("--n1 argu argu --n2 ment men t");
		assertEquals("argu", args.get(NAM_FIRST).value());
		assertEquals("ment", args.get(NAM_SECOND).value());
		assertEquals("argu", args.get(POS_FIRST).value());
		assertEquals("men t", args.get(POS_SECOND).value());
	}

	@Test
	void testMixDoubleExtraSpaces() {
		var args = ParameterList.of(NAM_FIRST, NAM_SECOND, POS_FIRST, POS_SECOND)
			.parse("--n1   argu   argu   --n2   ment   men   t");
		assertEquals("argu", args.get(NAM_FIRST).value());
		assertEquals("ment", args.get(NAM_SECOND).value());
		assertEquals("argu", args.get(POS_FIRST).value());
		assertEquals("men   t", args.get(POS_SECOND).value());
	}

	@Test
	void testMixDoubleReverse() {
		var args =
			ParameterList.of(NAM_FIRST, NAM_SECOND, POS_FIRST, POS_SECOND).parse("argu --n1 argu men t --n2 ment");
		assertEquals("argu", args.get(NAM_FIRST).value());
		assertEquals("ment", args.get(NAM_SECOND).value());
		assertEquals("argu", args.get(POS_FIRST).value());
		assertEquals("men t", args.get(POS_SECOND).value());
	}

	@Test
	void testMixDoubleReverseExtraSpaces() {
		var args = ParameterList.of(NAM_FIRST, NAM_SECOND, POS_FIRST, POS_SECOND)
			.parse("argu   --n1   argu   men   t   --n2   ment");
		assertEquals("argu", args.get(NAM_FIRST).value());
		assertEquals("ment", args.get(NAM_SECOND).value());
		assertEquals("argu", args.get(POS_FIRST).value());
		assertEquals("men   t", args.get(POS_SECOND).value());
	}

	@Test
	void testAlmostNamed() {
		var args = ParameterList.of(POS_FIRST).parse("argu - -- ment");
		assertEquals("argu - -- ment", args.get(POS_FIRST).value());
	}

	@Test
	void testInvalidPositionalSplit() {
		var params = ParameterList.of(NAM_FIRST, POS_FIRST);
		assertThrows(UsageException.class, () -> params.parse("argu ment --n1 argument argu ment"));
	}

	@Test
	void testInvalidPositionalSplitBeforeNamed() {
		var params = ParameterList.of(NAM_FIRST, NAM_SECOND, POS_FIRST);
		assertThrows(UsageException.class, () -> params.parse("argu ment --n1 argument argu ment --n2 arg"));
	}

	@Test
	void testInvalidUnknownNamed() {
		var params = ParameterList.of(NAM_FIRST, POS_FIRST);
		assertThrows(UsageException.class, () -> params.parse("--invalid argument"));
	}

	@Test
	void testInvalidUnknownPositional() {
		var params = ParameterList.of(NAM_FIRST);
		assertThrows(UsageException.class, () -> params.parse("argument"));
	}

	@Test
	void testInvalidNamedNoValue() {
		var params = ParameterList.of(NAM_FIRST);
		assertThrows(UsageException.class, () -> params.parse("--n1"));
	}

	@Test
	void testInvalidEmpty() {
		var params = ParameterList.of();
		assertThrows(UsageException.class, () -> params.parse("argument"));
	}

	@Test
	void testMissingPositional() {
		var params = ParameterList.of(POS_FIRST, POS_SECOND);
		assertThrows(UsageException.class, () -> params.parse("argument"));
	}

	@Test
	void testMissingNamed() {
		var params = ParameterList.of(POS_FIRST, NAM_FIRST);
		assertThrows(UsageException.class, () -> params.parse("argument"));
	}

}
