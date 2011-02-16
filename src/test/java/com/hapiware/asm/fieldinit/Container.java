package com.hapiware.asm.fieldinit;

import java.util.Date;

public class Container
{
	private Date _date;
	private Date _date2;
	private int _i = -1;
	private Address _address;
	private Address _address2;
	
	
	public Date getDate()
	{
		return _date;
	}
	public void setDate(Date date)
	{
		_date = date;
	}
	public Date getDate2()
	{
		return _date2;
	}
	public void setDate2(Date date2)
	{
		_date2 = date2;
	}
	public int getI()
	{
		return _i;
	}
	public void setI(int i)
	{
		_i = i;
	}
	public Address getAddress()
	{
		return _address;
	}
	public void setAddress(Address address)
	{
		_address = address;
	}
	public void setAddress2(Address address2)
	{
		_address2 = address2;
	}
	public Address getAddress2()
	{
		return _address2;
	}
}
