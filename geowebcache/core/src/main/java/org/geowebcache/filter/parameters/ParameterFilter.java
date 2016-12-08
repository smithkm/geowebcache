package org.geowebcache.filter.parameters;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface ParameterFilter extends Serializable, Cloneable {
    
    /**
     * Get the key of the parameter to filter.
     * @return
     */
    String getKey();
    
    /**
     * Get the default value to use if the parameter is not specified.
     * @return
     */
    String getDefaultValue();
    
    /**
     * Checks whether a given parameter value applies to this filter.
     * 
     * Calls {@link #apply(String)} and checks for {@link ParameterException}.  Subclasses should 
     * override if a more efficient check is available.
     * 
     * @param parameterValue
     *            the value to check if applies to this parameter filter
     * @return {@code true} if {@code parameterValue} is valid according to this filter,
     *         {@code false} otherwise
     */
    boolean applies(@Nullable String parameterValue);
    
    /**
     * Apply the filter to the specified parameter value.
     * @param str the value of the parameter to filter. {@literal null} if the parameter was unspecified.
     * @return one of the legal values
     * @throws ParameterException if the parameter value could not be reduced to one of the
     *          legal values.
     */
    String apply(@Nullable String str) throws ParameterException;
    
    /**
     * @return null if the legal values cannot be enumerated
     */
    List<String> getLegalValues();
    
}