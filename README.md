quasar-groovy
=============

[Quasar](http://docs.paralleluniverse.co/quasar/) is a library that provides scalable, lightweight
threads for the JVM (see references below for links to more information).
[Groovy](http://groovy-lang.org/) is a powerful, optionally typed and dynamic language for the Java
platform. This package integrates Quasar with Groovy, enabling the use of Quasar's elegant programming 
model from Groovy.

## Getting Started

First, you must build Groovy with Quasar support:

```mvn clean install```

Then, in an application that uses the Groovy replace original dependency:

```
<dependency>
	<groupId>org.codehaus.groovy</groupId>
	<artifactId>groovy</artifactId>
	<version>2.3.7</version>
</dependency>
```
on

```
<dependency>
	<groupId>org.codehaus.groovy</groupId>
	<artifactId>groovy</artifactId>
	<version>2.3.7</version>
	<classifier>quasar-support</classifier>
</dependency>
```

## Architecture

This package contains the original version of Groovy 2.3.7, which has been treated with Quasar InstrumentedTask.   

## References

The ParallelUniverse blog is a good source of information about Quasar:

* http://blog.paralleluniverse.co/2014/02/06/fibers-threads-strands/
* http://blog.paralleluniverse.co/2014/05/01/modern-java/

## Copyright

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Attribution

This library includes open source from the following sources:

* Groovy Libraries. Licensed under the Apache License v2.0 (http://www.apache.org/licenses/).
* Quasar Libraries Copyright 2014 Parallel Universe. Licensed under the GNU Lesser General Public License (http://www.gnu.org/licenses/lgpl.html).
