package com.hapiware.asm.fieldinit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * <b>NOTICE!</b> This test is made for testing byte code manipulation and thus does NOT run
 * directly from IDE without configurations (which are not directly included). This test is
 * to run with Maven but even there is a trick because normal Maven commands does not run
 * the test. To run this test follow these
 * steps:
 * 	<ol>
 * 		<li>Run {@code mvn clean install} (or {@code mvn clean package})</li>
 * 		<li>Run {@code mvn test -P test-agent}</li>
 * 	</ol>
 * 
 * The first command builds the agent delegate to be used in the second step and thus you
 * <u>must not use {@code clean} command in the second step</ul>.
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 *
 * @see UnmanipulatedAgentTest
 */
public class ManipulatedAgentTest
{
	@Test
	public void setFields()
	{
		Container container = new Container();
		assertEquals("Wed Mar 16 00:00:00 EET 2011", container.getDate().toString());
		assertEquals("Sat Aug 12 16:30:00 EEST 1995", container.getDate2().toString());
		assertEquals(314, container.getI());
		assertEquals("Street, 12345 City", container.getAddress().toString());
		assertEquals("Tie 2, 12345 Stadi", container.getAddress2().toString());
	}
}
