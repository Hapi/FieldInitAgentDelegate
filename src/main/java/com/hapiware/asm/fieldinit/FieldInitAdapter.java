package com.hapiware.asm.fieldinit;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;


/**
 * 
 * @author <a href="http://www.hapiware.com" target="_blank">hapi</a>
 * @see FieldInitAgentDelegate
 */
public class FieldInitAdapter extends AdviceAdapter
{
	public FieldInitAdapter(int access, String name, String desc, MethodVisitor mv)
	{
		super(mv, access, name, desc);
	}

	@Override
	protected void onMethodExit(int opcode)
	{
		// TODO: Add functionality!!!
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals)
	{
		mv.visitMaxs(maxStack + 2, maxLocals);
	}
}
