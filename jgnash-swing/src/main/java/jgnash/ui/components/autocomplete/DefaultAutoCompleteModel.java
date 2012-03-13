/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2012 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.ui.components.autocomplete;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Default model for auto complete search.
 *
 * @author Craig Cavanaugh
 * @author Don Brown
 * @version $Id: DefaultAutoCompleteModel.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class DefaultAutoCompleteModel implements AutoCompleteModel {

    private final List<String> list = new LinkedList<String>();

    private boolean ignoreCase;

    private boolean enabled = true;

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**     
     * @see AutoCompleteModel#doLookAhead(java.lang.String) 
     */
    @Override
    public String doLookAhead(final String content) {
        if (enabled) {
            return doLookAhead(content, ignoreCase);
        }
        return null;
    }

    private String doLookAhead(final String content, boolean ignoreCase) {
        if (content.length() > 0) {
            synchronized (list) {
                for (String s : list) {
                    
                    if (ignoreCase) {
                       if (s.equalsIgnoreCase(content)) {
                            break;
                        } 
                    } else {
                        if (s.equals(content)) {
                            break;
                        }
                    }
                    
                    if (startsWith(s, content, ignoreCase)) {
                            return s;
                    }

//                    if (ignoreCase) {
//                        if (s.equalsIgnoreCase(content)) {
//                            break;
//                        } else if (startsWith(s, content, ignoreCase)) {
//                            return s;
//                        }
//                    } else {
//                        if (s.equals(content)) {
//                            break;
//                        } else if (startsWith(s, content, ignoreCase)) {
//                            return s;
//                        }
//                    }
                }
            }
        }
        return null;
    }

    public void addString(final String content) {
        if (content != null && !content.isEmpty()) {
            synchronized (list) {
                int index = Collections.binarySearch(list, content);
                if (index < 0) {
                    list.add(-index - 1, content);
                }
            }
        }
    }

    /**
     * Removes all of the string that have been remembered
     */
    public void purge() {
        synchronized (list) {
            list.clear();
        }
    }

    /**
     * Returns extra information that might be stored with a
     * found string returned by doLookAhead().  This information
     * can be used to populate other fields based on matching the
     * string key.
     *
     * @param key The string key most likely returned from doLookAhead()
     * @return null
     */
    @Override
    public Object getExtraInfo(final String key) {
        return null;
    }
    
    /**
     * Tests if the source string starts with the prefix string. Case is
     * ignored.
     *
     * @param source the source String.
     * @param prefix the prefix String.
     * @param ignoreCase true if case should be ignored
     * @return true, if the source starts with the prefix string.
     */
    private static boolean startsWith(final String source, final String prefix, final boolean ignoreCase) {
        return prefix.length() <= source.length() && source.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
    }
}