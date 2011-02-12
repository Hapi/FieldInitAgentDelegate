package com.hapiware.asm.fieldinit;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;



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
	private final static String CONFIGURATION_ELEMENT = "configuration"; 
	private final static String TARGET_CLASS_ELEMENT = CONFIGURATION_ELEMENT + "/target-class"; 
	private final static String TARGET_FIELD_ELEMENT = TARGET_CLASS_ELEMENT + "/target-field"; 
	private final static String INITIALISER_ELEMENT = TARGET_FIELD_ELEMENT + "/initialiser"; 
	private final static String ARGUMENT_ELEMENT = INITIALISER_ELEMENT + "/argument";
	
	
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
			
			String date = (String)xpath.evaluate("./date", configElement, XPathConstants.STRING);
			
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
				new TargetField(targetFieldName, false, false, unmarshallInitialisers(xpath, targetFieldElement))
			);
		}
		return targetFields;
	}
	
	
	private static List<Initialiser> unmarshallInitialisers(XPath xpath, Element targetFieldElement)
	{
		// TODO: Parse config!!!
		return null;
	}
	
	
	public static class TargetClass
	{
		private final String _name;
		private final List<TargetField> _targetFields;
		
		
		public TargetClass(String name, List<TargetField> targetFields)
		{
			_name = name;
			_targetFields = Collections.unmodifiableList(targetFields);
		}


		public String getName()
		{
			return _name;
		}


		public List<TargetField> getTargetFields()
		{
			return _targetFields;
		}
	}

	public static class TargetField
	{
		private final String _name;
		private final boolean _overrideNonNull;
		private final boolean _isStatic;
		private final List<Initialiser> _initialisers;

		public TargetField(
			String name,
			boolean overrideNonNull,
			boolean isStatic,
			List<Initialiser> initialisers
		)
		{
			_name = name;
			_overrideNonNull = overrideNonNull;
			_isStatic = isStatic;
			_initialisers = Collections.unmodifiableList(initialisers);
		}

		public String getName()
		{
			return _name;
		}

		public boolean isOverrideNonNull()
		{
			return _overrideNonNull;
		}

		public boolean isStatic()
		{
			return _isStatic;
		}

		public List<Initialiser> getInitialisers()
		{
			return _initialisers;
		}
	}
	
	public static class Initialiser
	{
		private final String _typeName;
		private final List<ConstructorArgument> _constructorArguments;
		private final String _className;
		private final String _methodName;
		
		public Initialiser(String typeName, List<ConstructorArgument> constructorArguments)
		{
			_typeName = typeName;
			_constructorArguments = Collections.unmodifiableList(constructorArguments);
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

		public List<ConstructorArgument> getConstructorArguments()
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

