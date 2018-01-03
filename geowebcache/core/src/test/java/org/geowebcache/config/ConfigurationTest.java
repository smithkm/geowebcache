package org.geowebcache.config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class ConfigurationTest<I extends Info, C extends BaseConfiguration> {
    
    C config;
    
    @Rule 
    public ExpectedException exception = ExpectedException.none();
    
    @Before
    public void setUpTestUnit() throws Exception {
        config = getConfig();
    }
    
    @After 
    public void assertNameSetMatchesCollection() throws Exception {
        assertNameSetMatchesCollection(config);
    }
    
    @Test
    public void testAdd() throws Exception {
        I goodGridSet = getGoodInfo("test", 1);
        addInfo(config, goodGridSet);
        I retrieved = getInfo(config, "test");
        assertThat(retrieved, infoEquals(goodGridSet));
    }
    
    @Test
    public void testPersistAdd() throws Exception {
        I goodGridSet = getGoodInfo("test", 1);
        addInfo(config, goodGridSet);

        C config2 = getConfig();
        I retrieved = getInfo(config2, "test");
        assertThat(retrieved, infoEquals(goodGridSet));
        assertNameSetMatchesCollection(config2);
    }
    
    @Test
    public void testDoubleAddException() throws Exception {
        I goodGridSet = getGoodInfo("test", 1);
        I doubleGridSet = getGoodInfo("test", 2);
        assertThat("Invalid test", goodGridSet, not(infoEquals(doubleGridSet)));
        addInfo(config, goodGridSet);
        exception.expect(instanceOf(IllegalArgumentException.class)); // May want to change to something more specific.
        addInfo(config, doubleGridSet);
    }
    
    @Test
    public void testDoubleAddNoChange() throws Exception {
        I goodGridSet = getGoodInfo("test", 1);
        I doubleGridSet = getGoodInfo("test", 2);
        assertThat("Invalid test", goodGridSet, not(infoEquals(doubleGridSet)));
        addInfo(config, goodGridSet);
        try {
            addInfo(config, doubleGridSet);
        } catch (IllegalArgumentException ex) { // May want to change to something more specific.
            
        }
        I retrieved = getInfo(config, "test");
        assertThat(retrieved, infoEquals(goodGridSet));
    }
    
    @Test
    public void testAddBadInfoException() throws Exception {
        I badGridSet = getBadInfo("test", 1);
        exception.expect(IllegalArgumentException.class);// May want to change to something more specific.
        addInfo(config, badGridSet);
    }
    
    @Test
    public void testBadInfoNoAdd() throws Exception {
        I badGridSet = getGoodInfo("test", 1);
        addInfo(config, badGridSet);
        try {
            addInfo(config, badGridSet);
        } catch (IllegalArgumentException ex) { // May want to change to something more specific.
            
        }
        I retrieved = getInfo(config, "test");
        assertThat(retrieved, infoEquals(badGridSet));
    }
    
    @Test
    public void testRemove() throws Exception {
        testAdd();
        removeInfo(config, "test");
        I retrieved = getInfo(config, "test");
        assertThat(retrieved, nullValue());
    }
    
    @Test
    public void testPersistRemove() throws Exception {
        testPersistAdd();
        
        removeInfo(config, "test");

        C config2 = getConfig();
        I retrieved = getInfo(config2, "test");
        assertThat(retrieved, nullValue());
        assertNameSetMatchesCollection(config2);
    }
    
    @Test
    public void testGetNotExists() throws Exception {
        exception.expect(NoSuchElementException.class);
        @SuppressWarnings("unused")
        I retrieved = getInfo(config, "GridSetThatDoesntExist");
    }
    
    @Test
    public void testRemoveNotExists() throws Exception {
        exception.expect(NoSuchElementException.class);
        removeInfo(config, "GridSetThatDoesntExist");
    }
    @Test
    public void testModify() throws Exception {
        testAdd();
        I goodGridSet = getGoodInfo("test", 2);
        modifyInfo(config, goodGridSet);
        
        I retrieved = getInfo(config, "test");
        assertThat(retrieved, infoEquals(goodGridSet));
    }

    @Test
    public void testModifyBadGridSetException() throws Exception {
        testAdd();
        I badGridSet = getBadInfo("test", 2);
        
        exception.expect(IllegalArgumentException.class); // Could be more specific
        
        modifyInfo(config, badGridSet);
    }

    @Test
    public void testModifyBadGridSetNoChange() throws Exception {
        testAdd();
        I goodGridSet = getInfo(config, "test");
        I badGridSet = getBadInfo("test", 2);
        
        try {
            modifyInfo(config, badGridSet);
        } catch (IllegalArgumentException ex) { // Could be more specific
            
        }
        
        I retrieved = getInfo(config, "test");
        assertThat(retrieved, infoEquals(goodGridSet));
    }
    
    @Test
    public void testPersistModify() throws Exception {
        testPersistAdd();
        
        I goodGridSet = getGoodInfo("test", 2);
        modifyInfo(config, goodGridSet);

        C config2 = getConfig();
        I retrieved = getInfo(config2, "test");
        assertThat(retrieved, infoEquals(goodGridSet));
        assertNameSetMatchesCollection(config2);
    }
    
    @Test
    public void testModifyNotExistsExcpetion() throws Exception {
        I goodGridSet = getGoodInfo("test", 2);
        exception.expect(NoSuchElementException.class);// Inconsistent with other methods
        modifyInfo(config, goodGridSet);
    }
    
    @Test
    public void testModifyNotExistsNoChange() throws Exception {
        I goodGridSet = getGoodInfo("GridSetThatDoesntExist", 2);
        try {
            modifyInfo(config, goodGridSet);
        } catch(NoSuchElementException ex) {// Inconsistent with other methods.
            
        }
        I retrieved = getInfo(config, "GridSetThatDoesntExist");
        assertThat(retrieved, nullValue()); // Possibly should be exception instead?
    }
    
    @Test
    public void testGetExisting() throws Exception {
        I retrieved = getInfo(config, getExistingInfo());
        assertThat(retrieved, notNullValue());
    }
    
    /**
     * Create a GridSet that should be saveable in the configuration being tested. Throw 
     * AssumptionViolatedException if this is a read only GridSetConfiguration.
     * @param id ID for the GridSet
     * @param rand GridSets created with different values should not be equal to one another.
     * @return
     */
    protected abstract I getGoodInfo(String id, int rand) throws Exception;
    
    /**
     * Create a GridSet that should not be saveable in the configuration being tested. Throw 
     * AssumptionViolatedException if this is a read only GridSetConfiguration.
     * @param id ID for the GridSet
     * @param rand GridSets created with different values should not be equal to one another.
     * @return
     */
    protected abstract I getBadInfo(String id, int rand) throws Exception;
    
    /**
     * Get an ID for a pre-existing GridSet. Throw AssumptionViolatedException if this this
     * configuration does not have existing GridSets.
     * @return
     */
    protected abstract String getExistingInfo() throws Exception;

    /**
     * Create a GridSetConfiguration to test.  Subsequent calls should create new configurations using the
     * same persistence or throw AssumptionViolatedException if this is a non-persistent 
     * configuration.
     * @return
     * @throws Exception 
     */
    protected abstract C getConfig() throws Exception;
    
    /**
     * Check that two GridSets created by calls to getGoodGridSet, which may have been persisted and 
     * depersisted, are equal if and only if they had the same rand value.
     * @param expected
     * @return
     */
    protected abstract Matcher<I> infoEquals(final I expected);
    
    protected abstract void addInfo(C config, I info) throws Exception;
    protected abstract I getInfo(C config, String name) throws Exception;
    protected abstract Collection<? extends I> getInfos(C config) throws Exception;
    protected abstract Set<String> getInfoNames(C config) throws Exception;
    protected abstract void removeInfo(C config, String name) throws Exception;
    protected abstract void renameInfo(C config, String name1, String name2) throws Exception;
    protected abstract void modifyInfo(C config, I info) throws Exception;
    
    public void assertNameSetMatchesCollection(C config) throws Exception {
        Set<String> collectionNames = getInfos(config).stream().map(Info::getName).collect(Collectors.toSet());
        Set<String> setNames = getInfoNames(config);
        assertThat(setNames, equalTo(collectionNames));
    }

}