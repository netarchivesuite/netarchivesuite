/*
 * #%L
 * Netarchivesuite - deploy
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.common.utils;

import java.util.Map;

/**
 * Simple template engine functions that replaces %{...} in an array of strings or a single string. Error handling can
 * be deduced from the unit test. Uses %{..} to avoid replacing ${} strings which are not our concern
 */
public class Template {
	final static char MARKER = '%';
	
    /**
     * Prohibit external construction for now.
     */
    protected Template() {
    }

    /**
     * Takes an array of strings and returns a concatenated string with all %{...} occurrences replaced according to the
     * env map.
     *
     * @param strArray array of strings to be processed with env strings
     * @param env map of replacement strings
     * @param bFailOnMissing throw an exception on missing replacement string or not
     * @param separator separator to insert between lines or null
     * @return concatenated and processed string
     */
    public static String untemplate(String[] strArray, Map<String, String> env, boolean bFailOnMissing, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strArray.length; ++i) {
            sb.append(untemplate(strArray[i], env, bFailOnMissing));
            if (separator != null) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    /**
     * Takes a string and replaces all %{...} occurrences with env map strings.
     *
     * @param str string to be processed
     * @param env map of replacement strings
     * @param bFailOnMissing throw an exception on missing replacement string or not
     * @return processed string
     */
    public static String untemplate(String str, Map<String, String> env, boolean bFailOnMissing) {
        int strLen = str.length();
        String lookupStr;
        String replaceStr;
        StringBuilder sb = new StringBuilder();
        int pIdx = 0;
        int sIdx = 0;
        int fIdx;
        int tIdx;
        int c;
        while (sIdx != -1) {
            sIdx = str.indexOf(MARKER, pIdx);
            if (sIdx != -1) {
                if (sIdx + 1 < strLen) {
                    c = str.charAt(sIdx + 1);
                    if (c == MARKER) {
                        sb.append(str, pIdx, sIdx);
                        sb.append(MARKER);
                        sIdx += 2;
                        pIdx = sIdx;
                    } else if (c == '{') {
                        fIdx = sIdx + 2;
                        tIdx = str.indexOf('}', fIdx);
                        if (tIdx != -1) {
                            sb.append(str, pIdx, sIdx);
                            lookupStr = str.substring(fIdx, tIdx);
                            replaceStr = env.get(lookupStr);
                            if (replaceStr == null) {
                                if (!bFailOnMissing) {
                                    replaceStr = "";
                                } else {
                                    throw new IllegalArgumentException("Env is missing replacement for: " + lookupStr);
                                }
                            }
                            sb.append(replaceStr);
                            sIdx = tIdx + 1;
                            pIdx = sIdx;
                        } else {
                            sb.append(str, pIdx, fIdx);
                            sIdx = fIdx;
                            pIdx = sIdx;
                        }
                    } else {
                        sIdx = -1;
                    }
                } else {
                    sIdx = -1;
                }
            }
        }
        if (strLen > pIdx) {
            sb.append(str, pIdx, strLen);
        }
        return sb.toString();
    }

}
