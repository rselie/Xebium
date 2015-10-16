package com.xebia.incubator.xebium;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.thoughtworks.selenium.CommandProcessor;
import com.thoughtworks.selenium.webdriven.WebDriverCommandProcessor;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebDriverException;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class SeleniumDriverFixtureTest {

	@Mock
	private WebDriver webdriver;
	
	@Mock
	private Options options;
	
	@Mock
	private WebDriver.Window window;
	
	@Mock
	private Dimension demension;

	@Mock
	private WebDriverCommandProcessor webDriverCommandProcessor;

	@Mock
	private CommandProcessor commandProcessor;
	
	@Mock
	private ScreenCapture screenCapture;
	
	@Mock 
	VisualAnalyzer visualAnalyzer;

	private SeleniumDriverFixture seleniumDriverFixture;

	@Before
	public void setup() throws Exception {
		this.seleniumDriverFixture = new SeleniumDriverFixture();
		seleniumDriverFixture.setCommandProcessor(webDriverCommandProcessor);
		seleniumDriverFixture.setScreenCapture(screenCapture);
		seleniumDriverFixture.setVisualAnalyzer(visualAnalyzer);
	}

    @Test
    public void shouldDoVerifyRegularTextPresent() {
        given(webDriverCommandProcessor.doCommand("isTextPresent", new String[] { "foo" })).willReturn("true");
        final boolean result = seleniumDriverFixture.doOn("verifyTextPresent", "foo");
        assertThat(result, is(true));
    }

    @Test
    public void checkIsRegularTextPresent() {
        given(webDriverCommandProcessor.doCommand("isTextPresent", new String[] { "foo" })).willReturn("true");
        final String result = seleniumDriverFixture.isOn("isTextPresent", "foo");
        assertThat(result, is("true"));
    }

    @Test
    public void shouldDoVerifyRegularTextNotPresent() {
        given(webDriverCommandProcessor.doCommand("isTextPresent", new String[] { "foo" })).willReturn("false");
        final boolean result = seleniumDriverFixture.doOn("verifyTextNotPresent", "foo");
        assertThat(result, is(true));
    }

    @Test
    public void checkIsRegularTextNotPresent() {
        given(webDriverCommandProcessor.doCommand("isTextPresent", new String[] { "foo" })).willReturn("true");
        final String result = seleniumDriverFixture.isOn("isTextNotPresent", "foo");
        assertThat(result, is("false"));
    }

    @Test
	public void shouldVerifyRegularTextWithRegularExpressions() throws Exception {
		given(webDriverCommandProcessor.doCommand(anyString(), isA(String[].class))).willReturn("Di 9 november 2010. Het laatste nieuws het eerst op nu.nl");
		final boolean result = seleniumDriverFixture.doOnWith("verifyText", "//*[@id='masthead']/div/h1", "regexp:.*Het laatste nieuws het eerst op nu.nl");
		assertThat(result, is(true));
	}

	@Test
	public void shouldNegateIfCommandRequiresIt() throws Exception {
		given(webDriverCommandProcessor.doCommand(anyString(), isA(String[].class))).willReturn("Di 9 november 2010. Het laatste nieuws het eerst op nu.nl");
		final boolean result = seleniumDriverFixture.doOnWith("verifyNotText", "//*[@id='masthead']/div/h1", "regexp:.*Het laatste nieuws het eerst op nu.nl");
		assertThat(result, is(false));
	}

	@Test
	public void shouldMatchWithoutRegularExpression() throws Exception {
		given(webDriverCommandProcessor.doCommand(anyString(), isA(String[].class))).willReturn("Di 9 november 2010. Het laatste nieuws het eerst op nu.nl");
		final boolean result = seleniumDriverFixture.doOnWith("verifyText", "//*[@id='masthead']/div/h1",  "*Het laatste nieuws het eerst op nu.nl");
		assertThat(result, is(true));
	}
	
	@Test
	public void shouldMatchMultiValueStrings() {
		given(webDriverCommandProcessor.getStringArray(anyString(), isA(String[].class))).willReturn(new String[] { "Suite", "Test", "Normal" });
		final boolean result = seleniumDriverFixture.doOnWith("verifySelectOptions", "//foo",  "Suite,Test,Normal");
		assertThat(result, is(true));
	}

    @Test
    public void shouldResolveAlias() {
        String expectedString = "Het laatste nieuws het eerst op nu.nl";
        String alias = "laatsteNieuws";

        given(webDriverCommandProcessor.doCommand(anyString(), isA(String[].class))).willReturn(expectedString);
        seleniumDriverFixture.addAliasForLocator(alias, expectedString);
        final boolean result = seleniumDriverFixture.doOnWith("verifyText", "//*[@id='masthead']/div/h1", "%" + alias);
        assertThat(result, is(true));
    }

    @Test
    public void shouldIgnoreMissingAlias() {
        given(webDriverCommandProcessor.doCommand(anyString(), isA(String[].class))).willReturn("%foo");
        final boolean result = seleniumDriverFixture.doOnWith("verifyText", "//*[@id='masthead']/div/h1", "%foo");
        assertThat(result, is(true));
    }

    @Test
    public void shouldIgnoreEmptyAlias() {
        given(webDriverCommandProcessor.doCommand(anyString(), isA(String[].class))).willReturn("%");
        final boolean result = seleniumDriverFixture.doOnWith("verifyText", "//*[@id='masthead']/div/h1", "%");
        assertThat(result, is(true));
    }

	@Test
	public void shouldTakeScreenshotOnError() throws IOException {
		seleniumDriverFixture.saveScreenshotAfter("FAILURE");

		when(webDriverCommandProcessor.getBoolean("isElementPresent", new String[] { "id=verwijderen" })).thenReturn(true);
		when(webDriverCommandProcessor.doCommand("click", new String[] {"id=verwijderen"}))
				.thenThrow(new WebDriverException("Click failed: ReferenceError: Can't find variable: handle"));
		when(screenCapture.requireScreenshot(any(ExtendedSeleniumCommand.class), anyBoolean()))
				.thenReturn(true);

		try {
			seleniumDriverFixture.doOn("clickAndWait", "id=verwijderen");
		} catch (Throwable t) {
			// Not sure whether we want to propagate this exception... that's the current behaviour though.
		}

		verify(screenCapture).captureScreenshot("clickAndWait", new String[] { "id=verwijderen" });
	}
	
	@Test
	public void shouldTakeEndSendScreenshot() throws IOException {
		given(webDriverCommandProcessor.getWrappedDriver()).willReturn(webdriver);
		given(webdriver.manage()).willReturn(options);
		given(options.window()).willReturn(window);
		given(window.getSize()).willReturn(demension);
		given(demension.getHeight()).willReturn(120);
		given(demension.getWidth()).willReturn(320);
		String output = "Di 9 november 2010. Het laatste nieuws het eerst op nu.nl";
		given(webDriverCommandProcessor.doCommand(anyString(), isA(String[].class))).willReturn(output);
		try {
			seleniumDriverFixture.createVisualAnalyzeForProjectSuiteHostPort("project1", "suiteName", "localhost", "7000");
			seleniumDriverFixture.doOn("analyzeScreenshot", "NameOfScreenshot");
		} catch (Throwable t) {
			// Not sure whether we want to propagate this exception... that's the current behaviour though.
		}
		verify(visualAnalyzer).setSize(demension);
		verify(visualAnalyzer).takeAndSendScreenshot("NameOfScreenshot", output);
	}
	
	@Test
	public void shouldTakeEndSendScreenshotWithUnkownResolution() throws IOException {
		seleniumDriverFixture.setCommandProcessor(commandProcessor);
		String output = "Di 9 november 2010. Het laatste nieuws het eerst op nu.nl";
		given(commandProcessor.doCommand(anyString(), isA(String[].class))).willReturn(output);
		try {
			seleniumDriverFixture.createVisualAnalyzeForProjectSuiteHostPort("project1", "suiteName", "localhost", "7000");
			seleniumDriverFixture.doOn("analyzeScreenshot", "NameOfScreenshot");
		} catch (Throwable t) {
			// Not sure whether we want to propagate this exception... that's the current behaviour though.
		}
		verify(visualAnalyzer).setSize(null);
		verify(visualAnalyzer).takeAndSendScreenshot("NameOfScreenshot", output);
	}
}
