package org.codehaus.groovy.quasar;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import co.paralleluniverse.strands.SuspendableRunnable;
import com.google.common.util.concurrent.SettableFuture;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.MethodClosure;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:kovalenchenko.denis@gmail.com" >Denis Kovalenchenko</a>
 */
public class FiberTest {
	private transient FiberScheduler scheduler;

	public FiberTest() {
		this.scheduler = new FiberForkJoinScheduler("test", 4, null, false);
	}

	private Script createScript(String code, HashMap<String, Object> args) throws Exception {
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
		compilerConfiguration.setSourceEncoding("UTF-8");
		GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader(), compilerConfiguration);
		Class aClass = gcl.parseClass(code);
		final Script script = (Script) aClass.newInstance();
		script.setBinding(new Binding(args));
		return script;
	}


	@Test
	public void testDefaultSleep() throws Exception {
		final Script script = createScript("sleep(1000);", new HashMap<String, Object>());
		final AtomicInteger counter = new AtomicInteger(0);
		Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
			@Override
			public void run() throws SuspendExecution, InterruptedException {
				counter.incrementAndGet();
				script.run();
				counter.incrementAndGet();
			}
		});
		fiber.start();
		fiber.join();
		Assert.assertEquals(counter.intValue(), 2);
	}

	@Test
	public void testClosuresSleep() throws Exception {
		final SleepMethodSupport sleepMethodSupport = new SleepMethodSupport();
		HashMap<String, Object> args = new HashMap<String, Object>() {
			{
				put("sleepMethodSupport", sleepMethodSupport);
				put("_sleep", new MethodClosure(sleepMethodSupport, "_sleep"));
			}
		};
		final Script script = createScript("sleep(1000);_sleep(1000);sleepMethodSupport._sleep(1000)", args);
		final AtomicInteger counter = new AtomicInteger(0);
		Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {
			@Override
			public void run() throws SuspendExecution, InterruptedException {
				counter.incrementAndGet();
				script.run();
				counter.incrementAndGet();
			}
		});
		fiber.start();
		fiber.join();
		Assert.assertEquals(counter.intValue(), 2);
	}


	private static class SleepMethodSupport {
		public void _sleep(int delay) throws SuspendExecution, ExecutionException, InterruptedException {
			final SettableFuture<Boolean> future = SettableFuture.create();
			final Timer timer = new Timer(delay, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					future.set(true);
				}
			});
			timer.setRepeats(false);
			timer.start();
			AsyncListenableFuture.get(future);
		}
	}


	@Test
	public void testCreateFiberInGroovy() throws Exception {
		String code = "package ru.otr.alphaopen\n" +
				"\n" +
				"import co.paralleluniverse.fibers.Fiber\n" +
				"import co.paralleluniverse.fibers.SuspendExecution\n" +
				"import co.paralleluniverse.strands.SuspendableRunnable\n" +
				"import java.util.concurrent.atomic.AtomicInteger\n" +
				"\n" +
				"final AtomicInteger counter = new AtomicInteger(0)\n" +
				"Fiber fiber = new Fiber(scheduler, new SuspendableRunnable() {\n" +
				"\t@Override\n" +
				"\tvoid run() throws SuspendExecution, InterruptedException {\n" +
				"\t\tcounter.incrementAndGet();\n" +
				"\t\tFiber.sleep(1000);\n" +
				"\t\tcounter.incrementAndGet();\n" +
				"\t}\n" +
				"});\n" +
				"fiber.start();\n" +
				"fiber.join();\n" +
				"counter;\n";
		HashMap<String, Object> args = new HashMap<String, Object>();
		args.put("scheduler", scheduler);
		final Script script = createScript(code, args);
		AtomicInteger counter = (AtomicInteger) script.run();
		Assert.assertEquals(counter.intValue(), 2);
	}
}
