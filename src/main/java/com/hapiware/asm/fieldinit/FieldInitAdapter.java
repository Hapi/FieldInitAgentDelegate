package com.hapiware.asm.fieldinit;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import com.hapiware.asm.fieldinit.FieldInitAgentDelegate.ConstructorArgument;
import com.hapiware.asm.fieldinit.FieldInitAgentDelegate.Initialiser;
import com.hapiware.asm.fieldinit.FieldInitAgentDelegate.TargetField;


/**
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 * @see FieldInitAgentDelegate
 */
public class FieldInitAdapter extends AdviceAdapter
{
	private final TargetField[] _targetFields;
	private final String _owner;
	
	public FieldInitAdapter(TargetField[] targetFields, int access, String name, String desc, MethodVisitor mv)
	{
		super(mv, access, name, desc);
		_targetFields = targetFields;
		_owner = name;
	}

	@Override
	protected void onMethodExit(int opcode)
	{
		// TODO: Add functionality!!!
		for(TargetField tf : _targetFields) {
			Initialiser ini = tf.getInitialiser();
			boolean hasFactoryMethod = ini.getClassName() != null; 
			mv.visitVarInsn(ALOAD, 0);

			if(ini.isPrimitive()) {
				// TODO: Handle primitives
			} else
				if(hasFactoryMethod) {
					mv.visitMethodInsn(
						INVOKESTATIC,
						ini.getClassName(),
						ini.getMethodName(),
						ini.getTypeName()
					);
				} else {
					mv.visitTypeInsn(NEW, ini.getTypeName());
					mv.visitInsn(DUP);
					
					// TODO: Add correct number (and types) of arguments for constructor.
					for(ConstructorArgument ca : ini.getConstructorArguments()) {
						
					}
					
					mv.visitMethodInsn(INVOKESPECIAL, ini.getTypeName(), "<init>", ini.getDescriptor());				}

			mv.visitFieldInsn(
				tf.isStatic() ? PUTSTATIC : PUTFIELD,
				_owner,
				tf.getName(),
				tf.getTargetTypeDescriptor()
			);
		}
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals)
	{
		// TODO: Let asm to compute this!!!
		mv.visitMaxs(maxStack + 2, maxLocals);
	}
}
