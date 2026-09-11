package com.systech.ms.list;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.systech.ms.list.utils.Utils;

public class UtilsTest {
	
	@Test
	void removeNonAlphaNumericAndSpaceAndLowerTest() {
		assertEquals("akz",Utils.removeNonAlphaNumericAndSpaceAndLower("a-. k?=z"));
	}
}
