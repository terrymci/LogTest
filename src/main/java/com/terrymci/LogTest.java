package com.terrymci;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Log testing application class.
 */
public class LogTest {

    // Different log mode options
    private enum LogMode {
        Synchronous,        // Synchsonous logging
        AsyncLocking,       // "Classic" asycnhronous logging that uses a locking queue
        AsyncNonlocking;    // Asynchronous logging that uses LMAX Disruptor threading (non-locking)
    }

    // Change this value to test different logging modes, since the log configuration is done
    //   at static initialization time
    private static final LogMode mode = LogMode.AsyncLocking;

    // Static initialization block - does the logging configuration set-up/start
    static {
        configureLogging();
    }

    private static final Logger LOG = LogManager.getLogger(LogTest.class);
    private static SomeClass someObject = new SomeClass();

    /**
     * Application main entry point
     */
    public static void main(String[] args) {
        LOG.info("LogTest.main() started, mode = {}", mode);
        long msStart = System.currentTimeMillis();
        long msStop = msStart + 100;
        int index = 0;
        while (System.currentTimeMillis() < msStop) {
            LOG.debug("Loop iteration {}", index);
            someObject.doSomething();
            ++index;
        }
        LOG.info("Thread count: {}", Thread.activeCount());
        LOG.info("LogTest.main() finished, mode = {}", mode);
    }

    /**
     * Logging configuration set-up and start
     */
    private static void configureLogging() {
        // New logging configuration builder: this aggregates all the other configuration component "builders"
        ConfigurationBuilder<BuiltConfiguration> configBuilder
                = ConfigurationBuilderFactory.newConfigurationBuilder();

        // Console appender
        AppenderComponentBuilder appenderConsole
                = configBuilder.newAppender("console", "Console");

        // Appender for rolling files
        AppenderComponentBuilder rollingFile
                = configBuilder.newAppender("rollingFile", "RollingFile");
        rollingFile.addAttribute("fileName", "LogTest.log");
        rollingFile.addAttribute("filePattern", "LogTest.log.%i");

        // Max size for each rolling file
        ComponentBuilder triggerPolicy = configBuilder.newComponent("Policies")
                .addComponent(configBuilder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", "1 K"));
        rollingFile.addComponent(triggerPolicy);

        // Maximum count of rolling files
        ComponentBuilder rollStrategy = configBuilder.newComponent("DefaultRolloverStrategy")
                .addAttribute("max", 5);
        rollingFile.addComponent(rollStrategy);

        // Standard text formatting layout, applied to both appenders
        LayoutComponentBuilder layout
                = configBuilder.newLayout("PatternLayout");
        layout.addAttribute("pattern", "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level - %m%n");
        appenderConsole.add(layout);
        rollingFile.add(layout);

        // Add the two appenders to the configuration builder
        configBuilder.add(appenderConsole);
        configBuilder.add(rollingFile);

        // If doing blocking async logging, set up async appender that refers to the
        //   console and rolling file appenders, adn add it to the configuration builder
        if (LogMode.AsyncLocking.equals(mode)) {
            AppenderComponentBuilder appenderAsync
                    = configBuilder.newAppender("asyncAppender", "Async");
            appenderAsync.addComponent(configBuilder.newAppenderRef("console"));
            appenderAsync.addComponent(configBuilder.newAppenderRef("rollingFile"));
            configBuilder.add(appenderAsync);
        }

        RootLoggerComponentBuilder rootLogger;
        if (LogMode.AsyncNonlocking.equals(mode)) {
            rootLogger = configBuilder.newAsyncRootLogger(Level.DEBUG);
        } else {
            rootLogger = configBuilder.newRootLogger(Level.DEBUG);
        }

        // If logging in locking async, just add to the root logger the async appender that refers to the other two
        if (LogMode.AsyncLocking.equals(mode)) {
            rootLogger.add(configBuilder.newAppenderRef("asyncAppender"));
        } else {
            // otherwise, add the two appenders for console and rolling files
            rootLogger.add(configBuilder.newAppenderRef("console"));
            rootLogger.add(configBuilder.newAppenderRef("rollingFile"));
        }

        // Add the root logger to the configuration builder
        configBuilder.add(rootLogger);

        // Build the configuration and start the logging based on it
        BuiltConfiguration logConfig = configBuilder.build();
        Configurator.initialize(logConfig);
    }
}
