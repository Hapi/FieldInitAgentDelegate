package com.hapiware.asm.fieldinit;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import com.hapiware.asm.fieldinit.FieldInitAgentDelegate.TargetClass;

import static org.objectweb.asm.Opcodes.ACC_STATIC;


/**
 * Initialises {@link FieldInitAdapter} for class manipulation.
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 *
 */
public class FieldInitTransformer
	implements
		ClassFileTransformer
{
	private Pattern[] _includePatterns;
	private Pattern[] _excludePatterns;
	private TargetClass[] _targetClasses;
	
	public FieldInitTransformer(
		Pattern[] includePatterns,
		Pattern[] excludePatterns,
		TargetClass[] targetClasses
	)
	{
		_includePatterns = includePatterns;
		_excludePatterns = excludePatterns;
		_targetClasses = targetClasses;
	}

	public byte[] transform(
		ClassLoader loader,
		final String className,
		Class<?> classBeingRedefined,
		ProtectionDomain protectionDomain,
		byte[] classFileBuffer) throws IllegalClassFormatException
	{
		for(Pattern p : _excludePatterns)
			if(p.matcher(className).matches())
				return null;
		
		for(Pattern p : _includePatterns) {
			if(p.matcher(className).matches()) 
			{
				try
				{
					ClassReader cr = new ClassReader(classFileBuffer);
					ClassWriter cw = new ClassWriter(0);
					cr.accept(
						new ClassAdapter(cw)
						{
							public FieldVisitor visitField(
								int access,
								String name,
								String desc,
								String signature,
								Object value
							)
							{
								FieldVisitor fv =
									super.visitField(access, name, desc, signature, value);
								// TODO: Find out static fields for later reference!!!
								for(Pattern p : _loggerPatterns) {
									if((access & ACC_STATIC) == ACC_STATIC && p.matcher(desc).matches())
										loggerDescs.add(new LoggerDesc(name, desc));
								}
								return fv;
							}
							
							public MethodVisitor visitMethod(
								int access,
								String name,
								String desc,
								String signature,
								String[] exceptions
							)
							{
								MethodVisitor mv =
									super.visitMethod(access, name, desc, signature, exceptions);
								if(name.equals("<clinit>") || name.equals("<init>"))
									return new FieldInitAdapter(access, name, desc, mv);
								else
									return mv;
							} 
						},
						0
					);
					return cw.toByteArray();
				}
				catch(Throwable e)
				{
					throw new Error("Instrumentation of a class " + className + " failed.", e);
				}
			}
		}
		return null;
	}
}
