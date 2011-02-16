package com.hapiware.asm.fieldinit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


/**
 * This test itself seems to do practically nothing useful but it proves that just instantiating
 * {@link Container} it has its default values. Running the same (or more like similar) test
 * in {@link ManipulatedAgentTest} the result is completely different.
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 * 
 * @see ManipulatedAgentTest
 */
public class UnmanipulatedAgentTest
{
	@Test
	public void setFields()
	{
		Container container = new Container();
		assertNull(container.getDate());
		assertNull(container.getDate2());
		assertEquals(-1, container.getI());
		assertNull(container.getAddress());
		assertNull(container.getAddress2());
	}
}
