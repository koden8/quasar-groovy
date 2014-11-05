package org.codehaus.groovy.quasar;

import org.junit.Ignore;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:kovalenchenko.denis@gmail.com" >Denis Kovalenchenko</a>
 */
@Ignore
class PrintSignatures {
	public static void main(String[] args) throws ClassNotFoundException {
		Class aClass = Class.forName("groovy.lang.Script");
		for (Method method : aClass.getDeclaredMethods()) {
			String methodDescriptor = Type.getMethodDescriptor(method);
			System.out.println(method.getDeclaringClass().getName() + "." + method.getName() + methodDescriptor);
		}
	}
}
