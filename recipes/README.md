### How to run recipe on real server as test

All recipes which can be run as test have similar structure of configs

#### Context configuration files

>__context.xml__ - Runtime configuration file for our test

>__SpreaderBeans.xml__ - Spreader configuration to run in memory

>__SpreaderBeans-jersey.xml__ - Spreader configuration to run on server via http transport

#### YAML configuration files

>__conf.yml__ - Properties to run in memory

>__conf-jersey.yml__ - Properties to run on server


__All properties can be overrided via enviromental variables__

#### Switch between memory and jersey configs

To switch between configs just use enviromental variable __recipesEnv__.
When we set this variable as __REAL__, it starts use configuration from config-jersey.yml

This is example how to run recipe in test via __ru.taskurotta.bootstrap.Bootstrap__ with __RecipesRunner__ helper class:
```java
     @Test
     public void start() throws ArgumentParserException, IOException, ClassNotFoundException {
    		RecipesRunner.run("ru/taskurotta/recipes/wait/");
    		BasicFlowArbiter arbiter = (BasicFlowArbiter) new FlowArbiterFactory().getInstance(); // created in spring context
    		assertTrue(arbiter.waitForFinish(10000));
     }
```
For more information just look in sources of recipes artifact.