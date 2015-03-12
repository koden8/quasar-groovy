package org.codehaus.groovy.quasar;

import co.paralleluniverse.fibers.instrument.DefaultSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.QuasarInstrumentor;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.BytecodeProcessor;

/**
 * @author <a href="mailto:kovalenchenko.denis@gmail.com" >Denis Kovalenchenko</a>
 */
public class QuasarBytecodeProcessor implements BytecodeProcessor {
	private final QuasarInstrumentor instrumentor;

	public QuasarBytecodeProcessor(GroovyClassLoader groovyClassLoader) {
		instrumentor = new QuasarInstrumentor(true, groovyClassLoader, new DefaultSuspendableClassifier(groovyClassLoader));
	}

	@Override
	public byte[] processBytecode(String name, byte[] original) {
		return instrumentor.instrumentClass(name, original);
	}
}
