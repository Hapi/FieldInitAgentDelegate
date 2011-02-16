package com.hapiware.asm.fieldinit;

import java.util.StringTokenizer;

public class Address
{
	private final String _street;
	private final String _postalCode;
	private final String _city;
	
	public Address(Object address)
	{
		StringTokenizer tokenizer = new StringTokenizer((String)address, ",");
		_street = tokenizer.nextToken();
		_postalCode = tokenizer.nextToken();
		_city = tokenizer.nextToken(); 
	}
	public Address(String street, String city, String postalCode)
	{
		_street = street;
		_city = city;
		_postalCode = postalCode;
	}

	public String getStreet()
	{
		return _street;
	}

	public String getPostalCode()
	{
		return _postalCode;
	}

	public String getCity()
	{
		return _city;
	}
	
	@Override
	public String toString()
	{
		return _street + ", " + _postalCode + " " + _city;
	}
}
