package com.hapiware.asm.fieldinit;


public class Factory
{
	public static Address createAddress()
	{
		return new Address("Tie 2", "Stadi", "12345");
	}
}
