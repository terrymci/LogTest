package com.terrymci;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Some class that does something.
 */
public class SomeClass {
    private static final Logger LOG = LogManager.getLogger(SomeClass.class);

    public SomeClass() {
        LOG.info("Constructing SomeClass");
    }

    public void doSomething() {
        LOG.debug("Logging something!");
    }

}
