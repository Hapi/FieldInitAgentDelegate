package com.hapiware.asm.fieldinit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TestBase
{
	protected Document configDoc;
	protected Element custom;

	protected void setup() throws ParserConfigurationException
	{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		configDoc = builder.newDocument();

		// /agent/configuration/custom
		custom = configDoc.createElement("custom");
		configDoc.appendChild(custom);
	}
}
