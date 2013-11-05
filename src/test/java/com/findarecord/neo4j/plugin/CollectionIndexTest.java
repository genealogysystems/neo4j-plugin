package com.findarecord.neo4j.plugin;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class CollectionIndexTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CollectionIndexTest(String testName)
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( CollectionIndexTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testCollectionIndex()
    {
        assertTrue( true );
    }
}
