package com.hapiware.asm.fieldinit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import com.hapiware.asm.fieldinit.FieldInitAgentDelegate.ConstructorArgument;
import com.hapiware.asm.fieldinit.FieldInitAgentDelegate.Initialiser;
import com.hapiware.asm.fieldinit.FieldInitAgentDelegate.TargetClass;
import com.hapiware.asm.fieldinit.FieldInitAgentDelegate.TargetField;

public class ConfigurationTest
	extends
		TestBase
{

	
	@Before
	public void setup() throws ParserConfigurationException
	{
		super.setup();
	}
	
	@Test
	public void primitiveIntArgument()
	{
		Element item =
			createTargetClass(
				"com.hapiware.test.Poro",
				createTargetField(
					"_i",
					createPrimitiveTypeInitialiser("int", "12")
				)
			);
		custom.appendChild(item);
		TargetClass tc = ((TargetClass[])FieldInitAgentDelegate.unmarshall(custom))[0];
		TargetField tf = tc.getTargetFields()[0];
		Initialiser ini = tf.getInitialiser();
		ConstructorArgument ca = ini.getConstructorArguments()[0];
		assertEquals("com/hapiware/test/Poro", tc.getName());
		assertEquals("_i", tf.getName());
		assertEquals("int", ini.getTypeName());
		assertEquals("(I)V", ini.getDescriptor());
		assertNull(ca.getType());
		assertEquals("12", ca.getValue());
	}
	
	@Test
	public void dateWithStringArgument()
	{
		Element item =
			createTargetClass(
				"com.hapiware.test.Poro",
				createTargetField(
					"_date",
					createTypeInitialiser(
						"java.util.Date",
						"java.lang.String",
						"Sat, 12 Aug 1995 13:30:00 GMT"
					)
				)
			);
		custom.appendChild(item);
		TargetClass tc = ((TargetClass[])FieldInitAgentDelegate.unmarshall(custom))[0];
		TargetField tf = tc.getTargetFields()[0];
		Initialiser ini = tf.getInitialiser();
		ConstructorArgument ca = ini.getConstructorArguments()[0];
		assertEquals("com/hapiware/test/Poro", tc.getName());
		assertEquals("_date", tf.getName());
		assertEquals("java/util/Date", ini.getTypeName());
		assertEquals("(Ljava/lang/String;)V", ini.getDescriptor());
		assertEquals("java/lang/String", ca.getType());
		assertEquals("Sat, 12 Aug 1995 13:30:00 GMT", ca.getValue());
	}
	
	@Test
	public void dateWithIntArguments()
	{
		Element item =
			createTargetClass(
				"com.hapiware.test.Poro",
				createTargetField(
					"_date",
					createTypeInitialiser(
						"java.util.Date",
						new String[] { "int", "int", "int" },
						new String[] { "109", "10", "19" }
					)
				)
			);
		custom.appendChild(item);
		TargetClass tc = ((TargetClass[])FieldInitAgentDelegate.unmarshall(custom))[0];
		TargetField tf = tc.getTargetFields()[0];
		Initialiser ini = tf.getInitialiser();
		ConstructorArgument[] ca = ini.getConstructorArguments();
		assertEquals("com/hapiware/test/Poro", tc.getName());
		assertEquals("_date", tf.getName());
		assertEquals("java/util/Date", ini.getTypeName());
		assertEquals("(III)V", ini.getDescriptor());
		assertEquals("int", ca[0].getType());
		assertEquals("109", ca[0].getValue());
		assertEquals("int", ca[1].getType());
		assertEquals("10", ca[1].getValue());
		assertEquals("int", ca[2].getType());
		assertEquals("19", ca[2].getValue());
	}

	@Test
	public void factory()
	{
		Element item =
			createTargetClass(
				"com.hapiware.test.Poro",
				createTargetField(
					"_address",
					createFactoryInitialiser(
						"com.hapiware.test.Address",
						"com.hapiware.test.Factory",
						"createAddress"
					)
				)
			);
		custom.appendChild(item);
		TargetClass tc = ((TargetClass[])FieldInitAgentDelegate.unmarshall(custom))[0];
		TargetField tf = tc.getTargetFields()[0];
		Initialiser ini = tf.getInitialiser();
		assertEquals("com/hapiware/test/Poro", tc.getName());
		assertEquals("_address", tf.getName());
		assertEquals("com/hapiware/test/Address", ini.getTypeName());
		assertEquals("com/hapiware/test/Factory", ini.getClassName());
		assertEquals("createAddress", ini.getMethodName());
		assertEquals("()Lcom/hapiware/test/Address;", ini.getDescriptor());
	}

	
	private Element createPrimitiveTypeInitialiser(String type, String argumentValue)
	{
		Element initialiser = configDoc.createElement("initialiser");
		initialiser.setAttribute("type", type);
		Element argument = configDoc.createElement("argument");
		argument.appendChild(configDoc.createTextNode(argumentValue));
		initialiser.appendChild(argument);
		return initialiser;
	}
	
	private Element createTypeInitialiser(String type, String[] argumentTypes, String[] argumentValues)
	{
		if(argumentTypes.length != argumentValues.length)
			throw new IllegalArgumentException();
		
		Element initialiser = configDoc.createElement("initialiser");
		initialiser.setAttribute("type", type);
		for(int i = 0; i < argumentTypes.length; i++) {
			Element argument = configDoc.createElement("argument");
			argument.setAttribute("type", argumentTypes[i]);
			argument.appendChild(configDoc.createTextNode(argumentValues[i]));
			initialiser.appendChild(argument);
		}
		return initialiser;
	}
	
	private Element createTypeInitialiser(String type, String argumentType, String argumentValue)
	{
		return createTypeInitialiser(type, new String[] { argumentType }, new String[] { argumentValue });
	}
	
	private Element createFactoryInitialiser(String type, String className, String methodName)
	{
		Element initialiser = configDoc.createElement("initialiser");
		initialiser.setAttribute("type", type);
		initialiser.setAttribute("class", className);
		initialiser.setAttribute("method", methodName);
		return initialiser;
	}
	
	private Element createTargetField(String fieldName, Element initialiser)
	{
		Element targetField = configDoc.createElement("target-field");
		targetField.setAttribute("name", fieldName);
		targetField.appendChild(initialiser);
		return targetField;
	}
	
	private Element createTargetClass(String name, Element[] targetFields)
	{
		Element targetClass = configDoc.createElement("target-class");
		targetClass.setAttribute("name", name);
		for(Element targetField : targetFields)
			targetClass.appendChild(targetField);
		return targetClass;
	}
	
	private Element createTargetClass(String name, Element targetField)
	{
		return createTargetClass(name, new Element[] { targetField });
	}
}
