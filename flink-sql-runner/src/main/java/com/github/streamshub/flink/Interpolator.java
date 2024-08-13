package com.github.streamshub.flink;

/**
* An abstraction over a class that interpolates templated variables
 */
@FunctionalInterface
public interface Interpolator {

    /**
     * Interpolates templated variables in a given string
     * @param input String containing templated variables
     * @return String with interpolated values
     */
    String interpolate(String input);
}
