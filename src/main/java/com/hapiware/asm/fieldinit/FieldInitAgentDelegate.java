package com.hapiware.asm.fieldinit;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**
 *  
 * 
 * <h3>Requirements</h3>
 * {@code FieldInitAgentDelegate} requires:
 * <ul>
 * 		<li>{@code com.hapiware.agent.Agent}</li>
 * 		<li>
 * 			ASM 3.0 or later (see <a href="http://asm.ow2.org/" target="_blank">http://asm.ow2.org/</a>)
 * 		</li>
 * </ul>
 * 
 * 
 * <h3>Configuring field initialisation agent</h3>
 * The support agent is configured by using the following elements:
 * 	<ul>
 * 		<li>{@code <agent/delegate>}</li>
 * 		<li>{@code <agent/classpath>}</li>
 * 		<li>{@code <agent/filter>} (Optional but recommended)</li>
 * 	</ul>
 * 
 * For example:
 * <xmp>
 * 	<?xml version="1.0" encoding="UTF-8" ?>
 * 	<agent>
 * 		<delegate>com.hapiware.asm.fieldinit.FieldInitAgentDelegate</delegate>
 *		<classpath>
 * 			<entry>/users/me/agent/target/field-init-agent-delegate-1.0.0.jar</entry>
 * 			<entry>/usr/local/asm-3.1/lib/asm-3.1.jar</entry>
 * 		</classpath>
 * 
 * 		<!--
 * 		-->
 * 		<filter>
 *			<include>^com/hapiware/.+</include>
 * 		</filter>
 * 	</agent>
 * </xmp>
 * 
 * 
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 * @see com.hapiware.asm.agent.Agent
 */
public class FieldInitAgentDelegate
{
	private final static String CONFIGURATION_ELEMENT = "configuration/custom"; 
	private final static String TARGET_CLASS_ELEMENT = CONFIGURATION_ELEMENT + "/target-class"; 
	private final static String TARGET_FIELD_ELEMENT = TARGET_CLASS_ELEMENT + "/target-field"; 
	private final static String INITIALISER_ELEMENT = TARGET_FIELD_ELEMENT + "/initialiser"; 
	private final static String ARGUMENT_ELEMENT = INITIALISER_ELEMENT + "/argument";
	private final static Map<String, Class<?>> PRIMITIVE_TYPES;
	
	static {
		@SuppressWarnings("serial")
		Map<String, Class<?>> primitiveTypes =
			new HashMap<String, Class<?>>()
			{{
				put("short", short.class);
				put("int", int.class);
				put("long", long.class);
				put("boolean", boolean.class);
				put("char", char.class);
				put("byte", byte.class);
				put("float", float.class);
				put("double", double.class);
			}};
		PRIMITIVE_TYPES = Collections.unmodifiableMap(primitiveTypes);
	}
	
	
	/**
	 * This method is called by the general agent {@code com.hapiware.agent.Agent} and
	 * is done before the main method call right after the JVM initialisation. 
	 * <p>
	 * <b>Notice</b> the difference between this method and 
	 * the {@code public static void premain(String, Instrumentation} method described in
	 * {@code java.lang.instrument} package. 
	 *
	 * @param includePatterns
	 * 		A list of patterns to include classes for instrumentation.
	 * 
	 * @param excludePatterns
	 * 		A list patterns to set classes not to be instrumented.
	 * 
	 * @param config
	 * 		Not used.
	 * 
	 * @param instrumentation
	 * 		See {@link java.lang.instrument.Instrumentation}
	 * 
	 * @throws IllegalArgumentException
	 * 		If there is something wrong with the configuration file.
	 *
	 * @see java.lang.instrument
	 */
	public static void premain(
		Pattern[] includePatterns,
		Pattern[] excludePatterns,
		Object config,
		Instrumentation instrumentation
	)
	{
		try {
			instrumentation.addTransformer(
				new FieldInitTransformer(includePatterns, excludePatterns, (TargetClass[])config)
			);
		}
		catch(Exception e) 
		{
			System.err.println(
				"Couldn't start the field initialisation agent delegate due to an exception. "
					+ e.getMessage()
			);
			e.printStackTrace();
		}
	}
	
	
	public static Object unmarshall(Element configElement)
	{
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			
			NodeList targetClassEntries =
				(NodeList)xpath.evaluate(
					"./target-class",
					configElement,
					XPathConstants.NODESET
				);
			List<TargetClass> targetClasses = new ArrayList<TargetClass>();
			for(int i = 0; i < targetClassEntries.getLength(); i++) {
				Element targetClassElement = (Element)targetClassEntries.item(i);
				int numOfNameAttributes = targetClassElement.getAttributes().getLength();
				if(numOfNameAttributes < 1)
					throw
						new ConfigurationError(
							"A required 'name' attribute is missing from " + TARGET_CLASS_ELEMENT + " element."
						);
				if(numOfNameAttributes > 1)
					throw
						new ConfigurationError(
							"Only one 'name' attribute is allowed for " + TARGET_CLASS_ELEMENT + " element."
						);
				String targetClassName = targetClassElement.getAttribute("name");
				if(targetClassName == null)
					throw
						new ConfigurationError(
							"Only 'name' attribute is allowed for " + TARGET_CLASS_ELEMENT + " element."
						);
				targetClasses.add(
					new TargetClass(targetClassName, unmarshallTargetFields(xpath, targetClassElement))
				);
			}
			
			return targetClasses.toArray(new TargetClass[0]);
		}
		catch(XPathExpressionException e) {
			throw
				new ConfigurationError(
					"A config node was not found from the field initialisation agent delegate configuration file.",
					e
				);
		}
	}

	private static List<TargetField> unmarshallTargetFields(XPath xpath, Element targetClassElement)
		throws
			XPathExpressionException
	{
		NodeList targetFieldEntries =
			(NodeList)xpath.evaluate(
				"./target-field",
				targetClassElement,
				XPathConstants.NODESET
			);
		List<TargetField> targetFields = new ArrayList<TargetField>();
		for(int i = 0; i < targetFieldEntries.getLength(); i++) {
			Element targetFieldElement = (Element)targetFieldEntries.item(i);
			int numOfNameAttributes = targetFieldElement.getAttributes().getLength();
			if(numOfNameAttributes < 1)
				throw
					new ConfigurationError(
						"A required 'name' attribute is missing from " + TARGET_FIELD_ELEMENT + " element."
					);
			String targetFieldName = targetFieldElement.getAttribute("name");
			if(targetFieldName == null)
				throw
					new ConfigurationError(
						"Only 'name' attribute is allowed for " + TARGET_FIELD_ELEMENT + " element."
					);
			
			targetFields.add(
				new TargetField(targetFieldName, unmarshallInitialiser(xpath, targetFieldElement))
			);
		}
		return targetFields;
	}
	
	
	private static Initialiser unmarshallInitialiser(XPath xpath, Element targetFieldElement)
		throws
			XPathExpressionException
	{
		NodeList initialiserEntries =
			(NodeList)xpath.evaluate(
				"./initialiser",
				targetFieldElement,
				XPathConstants.NODESET
			);
		if(initialiserEntries.getLength() < 1)
			throw new ConfigurationError("A required " + INITIALISER_ELEMENT + " is missing.");
		if(initialiserEntries.getLength() > 1)
			throw new ConfigurationError("Only one " + INITIALISER_ELEMENT + " is allowed.");
		Element initialiserElement = (Element)initialiserEntries.item(0);
		NamedNodeMap attributes = initialiserElement.getAttributes();
		Node typeNode = attributes.getNamedItem("type");
		Node classNode = attributes.getNamedItem("class");
		Node methodNode = attributes.getNamedItem("method");
		if(typeNode == null) {
			if(attributes.getLength() != 2)
				throw
					new ConfigurationError(
						"Only single 'class' and 'method' attributes can be defined for "
							+ INITIALISER_ELEMENT + " element."
					);
			if(classNode == null)
				throw
					new ConfigurationError(
						"'method' attribute for " + INITIALISER_ELEMENT + " element requires aslo 'class' attribute."
					);
			if(methodNode == null)
				throw
					new ConfigurationError(
						"'class' attribute for " + INITIALISER_ELEMENT + " element requires aslo 'method' attribute."
					);
		}
		else
			if(attributes.getLength() != 1)
				throw
					new ConfigurationError(
						"'type' attribute for " + INITIALISER_ELEMENT + " element can exists only alone."
					);
		if(typeNode != null)
			return
				new Initialiser(
					typeNode.getTextContent(),
					unmarshallConstructorArguments(xpath, initialiserElement)
				);
		else
			return new Initialiser(classNode.getTextContent(), methodNode.getTextContent());
	}
	
	
	private static List<ConstructorArgument> unmarshallConstructorArguments(XPath xpath, Element initialiserElement)
		throws
			XPathExpressionException
	{
		NodeList argumentEntries =
			(NodeList)xpath.evaluate(
				"./argument",
				initialiserElement,
				XPathConstants.NODESET
			);
		String initialiserType = initialiserElement.getAttribute("type");
		final boolean primitiveInitialiser = PRIMITIVE_TYPES.containsKey(initialiserType);
		if(primitiveInitialiser && argumentEntries.getLength() > 1)
			throw
				new ConfigurationError(
					"For the primitive type '" + initialiserType 
						+ "' only one "	+ ARGUMENT_ELEMENT + " element is allowed."
				);
		List<ConstructorArgument> arguments = new ArrayList<ConstructorArgument>();
		for(int i = 0; i < argumentEntries.getLength(); i++) {
			Element argumentElement = (Element)argumentEntries.item(i);
			int numOfTypeAttributes = argumentElement.getAttributes().getLength();
			if(!primitiveInitialiser && numOfTypeAttributes < 1)
				throw
					new ConfigurationError(
						"A required 'type' attribute is missing from " + ARGUMENT_ELEMENT + " element."
					);
			if(numOfTypeAttributes > 1)
				throw
					new ConfigurationError(
						"Only one 'type' attribute is allowed for " + ARGUMENT_ELEMENT + " element."
					);
			if(!primitiveInitialiser) {
				String typeName = argumentElement.getAttribute("type");
				if(typeName == null)
					throw
						new ConfigurationError(
							"Only 'type' attribute is allowed for " + ARGUMENT_ELEMENT + " element."
						);
				arguments.add(
					new ConstructorArgument(typeName, argumentElement.getTextContent())
				);
			}
			else
				arguments.add(
					new ConstructorArgument(null, argumentElement.getTextContent())
				);
		}
		return arguments;
	}
	
	
	public static class TargetClass
	{
		private final String _name;
		private final TargetField[] _targetFields;
		
		
		public TargetClass(String name, List<TargetField> targetFields)
		{
			_name = name;
			_targetFields = targetFields.toArray(new TargetField[0]);
		}


		public String getName()
		{
			return _name;
		}


		public TargetField[] getTargetFields()
		{
			return _targetFields;
		}
	}

	public static class TargetField
	{
		private final String _name;
		private boolean _isStatic;
		private final Initialiser _initialiser;

		public TargetField(String name, Initialiser initialiser)
		{
			_name = name;
			_initialiser = initialiser;
		}

		public String getName()
		{
			return _name;
		}

		public boolean isStatic()
		{
			return _isStatic;
		}
		
		public void setStatic(boolean isStatic)
		{
			_isStatic = isStatic;
		}

		public Initialiser getInitialiser()
		{
			return _initialiser;
		}
	}
	
	public static class Initialiser
	{
		private final String _typeName;
		private final ConstructorArgument[] _constructorArguments;
		private final String _className;
		private final String _methodName;
		
		public Initialiser(String typeName, List<ConstructorArgument> constructorArguments)
		{
			_typeName = typeName;
			_constructorArguments = constructorArguments.toArray(new ConstructorArgument[0]);
			_className = null;
			_methodName = null;
		}

		public Initialiser(String className, String methodName)
		{
			_typeName = null;
			_constructorArguments = null;
			_className = className;
			_methodName = methodName;
		}

		public String getTypeName()
		{
			return _typeName;
		}

		public ConstructorArgument[] getConstructorArguments()
		{
			return _constructorArguments;
		}

		public String getClassName()
		{
			return _className;
		}

		public String getMethodName()
		{
			return _methodName;
		}
	}
	
	public static class ConstructorArgument
	{
		private final String _type;
		private final String _value;
		
		public ConstructorArgument(String type, String value)
		{
			_type = type;
			_value = value;
		}

		public String getType()
		{
			return _type;
		}

		public String getValue()
		{
			return _value;
		}
	}
	
	/**
	 * A runtime error to indicate that there is something wrong with the configuration of
	 * the field initialisation agent delegate. 
	 * 
	 * @author hapi
	 *
	 */
	static class ConfigurationError extends Error
	{
		private static final long serialVersionUID = -236321167558175285L;

		public ConfigurationError()
		{
			super();
		}

		public ConfigurationError(String message, Throwable cause)
		{
			super(message, cause);
		}

		public ConfigurationError(String message)
		{
			super(message);
		}

		public ConfigurationError(Throwable cause)
		{
			super(cause);
		}
	}
	
}

