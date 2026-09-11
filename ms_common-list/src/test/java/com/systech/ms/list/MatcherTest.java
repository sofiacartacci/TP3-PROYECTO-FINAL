package com.systech.ms.list;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.systech.ms.list.model.CheckResult;
import com.systech.ms.list.model.SearchQuery;
import com.systech.ms.list.service.Matcher;
import com.systech.ms.list.utils.MatchingUtils;
import com.systech.ms.list.utils.MatchingUtilsParams;

@ActiveProfiles("test")
@SpringBootTest(classes = { Matcher.class, MatchingUtils.class, MatchingUtilsParams.class })
@Disabled
public class MatcherTest {
	@Autowired
	Matcher matcher;

	@Test
	void caso1() throws Exception {
		SearchQuery search = new SearchQuery();
		search.setText("asala");
		search.setMinLevel(50);
		CheckResult r = matcher.check(search);
		assertEquals(1, r.getMatches().size());
		assertEquals("WC2", r.getMatches().get(0).getUi());
		assertEquals(1.0, r.getMatches().get(0).getBestCoincidence());
	}

	@Test
	void caso2() throws Exception {
		SearchQuery search = new SearchQuery();
		search.setText("AD002119");
		search.setMinLevel(50);
		CheckResult r = matcher.check(search);
		assertEquals(1, r.getMatches().size());
		assertEquals("WC2221", r.getMatches().get(0).getUi());
		assertEquals(1.0, r.getMatches().get(0).getBestCoincidence());
	}

	@Test
	void caso3() throws Exception {
		SearchQuery search = new SearchQuery();
		search.setText("AL-THANI");
		search.setMinLevel(20);
		CheckResult r = matcher.check(search);
		assertEquals(13, r.getMatches().size());
	}

	@Test
	void caso4() throws Exception {
		SearchQuery search = new SearchQuery();
		search.setText("Abdul MOHAMMAD Rahman TAIB");
		search.setMinLevel(50);
		CheckResult r = matcher.check(search);
		assertEquals(1, r.getMatches().size());
		assertEquals(1.0, r.getMatches().get(0).getBestCoincidence());
		assertEquals("ass", r.getMatches().get(0).getMatchType());
	}

}
