package org.jbpm.extensions.notifications.impl.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {

    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

	public static String read(InputStream input) {
        String lineSeparator = System.getProperty("line.separator");

        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")))) {
            return buffer.lines().collect(Collectors.joining(lineSeparator));
        } catch (Exception e) {
            logger.error("Error trying to read from inputstream", e);
            return null;
        }
    }
}
