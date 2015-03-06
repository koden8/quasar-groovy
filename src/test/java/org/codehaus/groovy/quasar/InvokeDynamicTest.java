package org.codehaus.groovy.quasar;

import co.paralleluniverse.fibers.*;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import co.paralleluniverse.fibers.instrument.DefaultSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.QuasarInstrumentor;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.invoke.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author <a href="mailto:kovalenchenko.denis@gmail.com" >Denis Kovalenchenko</a>
 */
@Ignore
public class InvokeDynamicTest {
	private transient FiberScheduler scheduler;

	public InvokeDynamicTest() {
		this.scheduler = new FiberForkJoinScheduler("test", 4, null, false);
	}

	/**
	 * Generate invoker with invokeDynamic instructions
	 */
	private static Class createClassWithInvokeDynamicInstruction()
			throws Exception {
		final String dynamicInvokerClassName = (InvokeDynamicTest.class.getPackage().getName() + "." + "MockClass");
		final String dynamicLinkageClassName = (InvokeDynamicTest.class.getName()).replace('.', '/');
		final ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;

		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, dynamicInvokerClassName.replace('.', '/'), null, "java/lang/Object", null);
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "invokeDynamic", "()V", null, new String[]{SuspendExecution.class.getName().replace('.', '/')});
			mv.visitCode();
			MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
					MethodType.class);
			Handle bootstrap = new Handle(H_INVOKESTATIC, dynamicLinkageClassName.replace('.', '/'), "bootstrap",
					mt.toMethodDescriptorString());
			mv.visitInvokeDynamicInsn("sleep", "()V", bootstrap);
			mv.visitInsn(RETURN);
			mv.visitMaxs(0, 1);
			mv.visitEnd();
		}
		cw.visitEnd();

		final ClassLoader classLoader = new ClassLoader() {
			public Class findClass(String name) throws ClassNotFoundException {
				if (dynamicInvokerClassName.equals(name)) {
					byte[] code = cw.toByteArray();
					final QuasarInstrumentor instrumentor = new QuasarInstrumentor(true, this, new DefaultSuspendableClassifier(this));
					code = instrumentor.instrumentClass(name, code);
					return defineClass(name, code, 0, code.length);
				}
				throw new ClassNotFoundException(name);
			}
		};
		return classLoader.loadClass(dynamicInvokerClassName);
	}

	/**
	 * bootstrap method for invokeDynamic support
	 */
	public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type)
			throws NoSuchMethodException, IllegalAccessException {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		Class thisClass = lookup.lookupClass();
		MethodHandle methodHandle = lookup.findStatic(thisClass, name, MethodType.methodType(void.class));
		return new ConstantCallSite(methodHandle.asType(type));
	}

	@Suspendable
	private static void sleep() throws SuspendExecution, ExecutionException, InterruptedException {
		final SettableFuture<Boolean> future = SettableFuture.create();
		final Timer timer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				future.set(true);
			}
		});
		timer.setRepeats(false);
		timer.start();
		AsyncListenableFuture.get(future);
	}

	@Test
	public void testInvokeDynamic() throws Exception {
		final Class invokerClass = createClassWithInvokeDynamicInstruction();
		final AtomicInteger counter = new AtomicInteger(0);
		Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
			@Override
			public void run() throws SuspendExecution, InterruptedException {
				counter.incrementAndGet();
				try {
					invokerClass.getDeclaredMethod("invokeDynamic").invoke(invokerClass);
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					throw new IllegalStateException(e);
				}
				counter.incrementAndGet();
			}
		});
		fiber.start();
		fiber.join();
		Assert.assertEquals(counter.intValue(), 2);
	}
}
