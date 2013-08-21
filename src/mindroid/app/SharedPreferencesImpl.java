/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2013 Daniel Himmelein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mindroid.app;

import mindroid.content.SharedPreferences;
import mindroid.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class SharedPreferencesImpl implements SharedPreferences {
	private static final String LOG_TAG = "SharedPreferencesImpl";
	private static final String UTF_8 = "UTF-8";
	private static final int SIZE_16_KB = 16 * 1024;
	private final static String MAP_TAG = "map";
	private final static String BOOLEAN_TAG = "boolean";
	private final static String INT_TAG = "int";
	private final static String LONG_TAG = "long";
	private final static String FLOAT_TAG = "float";
	private final static String STRING_TAG = "string";
	private final static String STRING_SET_TAG = "set";
	private final static String NAME_ATTR = "name";
	private final static String VALUE_ATTR = "value";

    private final File mFile;
    private final File mBackupFile;
    private Map mMap;

    SharedPreferencesImpl(File file, int mode) {
        mFile = file;
        mBackupFile = makeBackupFile(file);
        mMap = null;
        loadSharedPrefs();
    }

    private synchronized void loadSharedPrefs() {
        if (mMap != null) {
            return;
        }
        if (mBackupFile.exists()) {
            mFile.delete();
            mBackupFile.renameTo(mFile);
        }

        Map map = null;
        try {
            if (mFile.canRead()) {
                BufferedInputStream is = null;
                try {
                	is = new BufferedInputStream(new FileInputStream(mFile), SIZE_16_KB);
                    map = readMap(is);
                } catch (XmlPullParserException e) {
                    Log.w(LOG_TAG, "loadSharedPrefs", e);
                } catch (FileNotFoundException e) {
                    Log.w(LOG_TAG, "loadSharedPrefs", e);
                } catch (IOException e) {
                    Log.w(LOG_TAG, "loadSharedPrefs", e);
                } finally {
                	if (is != null) {
                		is.close();
                	}
                }
            }
        } catch (Exception e) {
        	Log.e(LOG_TAG, "Cannot read file: " + mFile.getName());
        }
        if (map != null) {
            mMap = map;
        } else {
            mMap = new HashMap();
        }
    }

    private static File makeBackupFile(File prefsFile) {
        return new File(prefsFile.getPath() + ".bak");
    }

    public Map getAll() {
        synchronized (this) {
            return new HashMap(mMap);
        }
    }

    public String getString(String key, String defValue) {
        synchronized (this) {
            String v = (String) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    public Set getStringSet(String key, Set defValues) {
        synchronized (this) {
            Set v = (Set) mMap.get(key);
            return v != null ? v : defValues;
        }
    }

    public int getInt(String key, int defValue) {
        synchronized (this) {
            Integer v = (Integer) mMap.get(key);
            return v != null ? v.intValue() : defValue;
        }
    }
    public long getLong(String key, long defValue) {
        synchronized (this) {
            Long v = (Long) mMap.get(key);
            return v != null ? v.longValue() : defValue;
        }
    }
    public float getFloat(String key, float defValue) {
        synchronized (this) {
            Float v = (Float) mMap.get(key);
            return v != null ? v.floatValue() : defValue;
        }
    }
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (this) {
            Boolean v = (Boolean) mMap.get(key);
            return v != null ? v.booleanValue() : defValue;
        }
    }

    public boolean contains(String key) {
        synchronized (this) {
            return mMap.containsKey(key);
        }
    }

    public Editor edit() {
        return new EditorImpl();
    }

    public final class EditorImpl implements Editor {
        private final Map mModifications = new HashMap();
        private boolean mClearMap = false;

        public Editor putString(String key, String value) {
            synchronized (this) {
                mModifications.put(key, value);
                return this;
            }
        }
        
        public Editor putStringSet(String key, Set values) {
            synchronized (this) {
                mModifications.put(key,
                        (values == null) ? null : new HashSet(values));
                return this;
            }
        }
        
        public Editor putInt(String key, int value) {
            synchronized (this) {
                mModifications.put(key, new Integer(value));
                return this;
            }
        }
        
        public Editor putLong(String key, long value) {
            synchronized (this) {
                mModifications.put(key, new Long(value));
                return this;
            }
        }
        
        public Editor putFloat(String key, float value) {
            synchronized (this) {
                mModifications.put(key, new Float(value));
                return this;
            }
        }
        
        public Editor putBoolean(String key, boolean value) {
            synchronized (this) {
                mModifications.put(key, new Boolean(value));
                return this;
            }
        }

        public Editor remove(String key) {
            synchronized (this) {
                mModifications.put(key, this);
                return this;
            }
        }

        public Editor clear() {
            synchronized (this) {
                mClearMap = true;
                return this;
            }
        }

        public void apply() {
        	commit();
        }

        public boolean commit() {
        	synchronized (this) {
        		boolean modifications = false;
        		
        		if (mClearMap) {
                    if (!mMap.isEmpty()) {
                    	modifications = true;
                        mMap.clear();
                    }
                    mClearMap = false;
                }
        		
        		Iterator itr = mModifications.entrySet().iterator();
    		    while (itr.hasNext()) {
    		        Map.Entry pair = (Map.Entry) itr.next();
                    String k = (String) pair.getKey();
                    Object v = pair.getValue();
                    if (v == this) { // magic value for a removal
                        if (!mMap.containsKey(k)) {
                            continue;
                        }
                        mMap.remove(k);
                    } else {
                        if (mMap.containsKey(k)) {
                            Object existingValue = mMap.get(k);
                            if (existingValue != null && existingValue.equals(v)) {
                                continue;
                            }
                        }
                        mMap.put(k, v);
                    }
                    
                    modifications = true;
                }

    		    mModifications.clear();
    		    
    		    if (modifications) {
    		    	if (mFile.exists()) {
    		    		if (!mBackupFile.exists()) {
    		                if (!mFile.renameTo(mBackupFile)) {
    		                    Log.e(LOG_TAG, "Cannot rename file " + mFile + " to backup file " + mBackupFile);
    		                    return false;
    		                }
    		            } else {
    		                mFile.delete();
    		            }
    		    	}
    		    	
	    		    try {
	    	            FileOutputStream os = new FileOutputStream(mFile);
	    	            writeMap(os, mMap);
	    	            os.close();
	    	            mBackupFile.delete();
	    	            return true;
	    	        } catch (XmlPullParserException e) {
	    	            Log.w(LOG_TAG, "commit", e);
	    	        } catch (IOException e) {
	    	            Log.w(LOG_TAG, "commit", e);
	    	        }
	    		    
	    	        // Clean up an unsuccessfully written file
	    	        if (mFile.exists()) {
	    	            if (!mFile.delete()) {
	    	                Log.e(LOG_TAG, "Cannot clean up partially-written file " + mFile);
	    	            }
	    	        }
    		    }
    		    
    		    return false;
        	}
        }
    }

    private Map readMap(InputStream is) throws XmlPullParserException, IOException {
		KXmlParser parser;
		parser = new KXmlParser();
		parser.setInput(is, UTF_8);
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, MAP_TAG);
		
		Map map = new HashMap();
		for (int eventType = parser.nextTag(); !parser.getName().equals(MAP_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			if (eventType == XmlPullParser.END_TAG) {
				throw new XmlPullParserException("Invalid XML format");
			}
			if (parser.getName().equals(BOOLEAN_TAG)) {
				parseBoolean(parser, map);
			} else if (parser.getName().equals(INT_TAG)) {
				parseInt(parser, map);
			} else if (parser.getName().equals(LONG_TAG)) {
				parseLong(parser, map);
			} else if (parser.getName().equals(FLOAT_TAG)) {
				parseFloat(parser, map);
			} else if (parser.getName().equals(STRING_TAG)) {
				parseString(parser, map);
			} else if (parser.getName().equals(STRING_SET_TAG)) {
				parseStringSet(parser, map);
			} else {
				String tag = parser.getName();
				parser.skipSubTree();
				parser.require(XmlPullParser.END_TAG, null, tag);
			}
		}
		
		parser.require(XmlPullParser.END_TAG, null, MAP_TAG);
		parser.next();
		parser.require(XmlPullParser.END_DOCUMENT, null, null);
		
		return map;
    }
    
    private static void parseBoolean(KXmlParser parser, Map map) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, null, BOOLEAN_TAG);
		
		String name = null;
		Boolean value = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("name")) {
				name = attributeValue;
			} else if (attributeName.equals("value")) {
				if (attributeValue.equals("true")) {
					value = new Boolean(true);
				} else {
					value = new Boolean(false);
				}
			}
		}
		parser.nextTag();

		parser.require(XmlPullParser.END_TAG, null, BOOLEAN_TAG);
		
		if ((name != null) && (value != null)) {
			map.put(name, value);
		}
	}
    
    private static void parseInt(KXmlParser parser, Map map) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, null, INT_TAG);
		
		String name = null;
		Integer value = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("name")) {
				name = attributeValue;
			} else if (attributeName.equals("value")) {
				try {
					value = new Integer(attributeValue);
				} catch (NumberFormatException e) {
				}
			}
		}
		parser.nextTag();

		parser.require(XmlPullParser.END_TAG, null, INT_TAG);
		
		if ((name != null) && (value != null)) {
			map.put(name, value);
		}
	}
    
    private static void parseLong(KXmlParser parser, Map map) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, null, LONG_TAG);
		
		String name = null;
		Long value = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("name")) {
				name = attributeValue;
			} else if (attributeName.equals("value")) {
				try {
					value = new Long(attributeValue);
				} catch (NumberFormatException e) {
				}
			}
		}
		parser.nextTag();

		parser.require(XmlPullParser.END_TAG, null, LONG_TAG);
		
		if ((name != null) && (value != null)) {
			map.put(name, value);
		}
	}
    
    private static void parseFloat(KXmlParser parser, Map map) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, null, FLOAT_TAG);
		
		String name = null;
		Float value = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals(NAME_ATTR)) {
				name = attributeValue;
			} else if (attributeName.equals(VALUE_ATTR)) {
				try {
					value = new Float(attributeValue);
				} catch (NumberFormatException e) {
				}
			}
		}
		parser.nextTag();

		parser.require(XmlPullParser.END_TAG, null, FLOAT_TAG);
		
		if ((name != null) && (value != null)) {
			map.put(name, value);
		}
	}
    
    private static void parseString(KXmlParser parser, Map map) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, null, STRING_TAG);
		
		String name = null;
		String value = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("name")) {
				name = attributeValue;
			}
		}
		value = parser.nextText();

		parser.require(XmlPullParser.END_TAG, null, STRING_TAG);
		
		if ((name != null) && (value != null)) {
			map.put(name, value);
		}
	}
    
    private static void parseStringSet(KXmlParser parser, Map map) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, null, STRING_SET_TAG);
		
		String name = null;
		Set value = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("name")) {
				name = attributeValue;
			}
		}
		
		value = new HashSet();
		for (int eventType = parser.nextTag(); !parser.getName().equals(STRING_SET_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			if (eventType == XmlPullParser.END_TAG) {
				throw new XmlPullParserException("Invalid XML format");
			}
			if (parser.getName().equals(STRING_TAG)) {
				parser.require(XmlPullParser.START_TAG, null, STRING_TAG);
				value.add(parser.nextText());
				parser.require(XmlPullParser.END_TAG, null, STRING_TAG);
			} else {
				String tag = parser.getName();
				parser.skipSubTree();
				parser.require(XmlPullParser.END_TAG, null, tag);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, STRING_SET_TAG);
		
		if ((name != null) && (value != null)) {
			map.put(name, value);
		}
	}
    
    private void writeMap(OutputStream os, Map map) throws XmlPullParserException, IOException {
    	Element rootTag = new Element();
		rootTag.setName(MAP_TAG);
		
		Iterator itr = map.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            Element e = writeValue((String) entry.getKey(), entry.getValue());
            if (e != null) {
            	rootTag.addChild(Node.ELEMENT, e);
            }
        }

		Document doc = new Document();
		doc.addChild(0, Node.ELEMENT, rootTag);
		
		KXmlSerializer serializer = new KXmlSerializer();
		serializer.setOutput(os, UTF_8);
		doc.write(serializer);
		serializer.flush();
    }
    
    private static final Element writeValue(String name, Object value) throws XmlPullParserException, java.io.IOException {
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
        	Element element = new Element();
        	element.setName(BOOLEAN_TAG);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, NAME_ATTR, name);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, VALUE_ATTR, value.toString());
        	return element;
        } else if (value instanceof Integer) {
        	Element element = new Element();
        	element.setName(INT_TAG);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, NAME_ATTR, name);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, VALUE_ATTR, value.toString());
        	return element;
        } else if (value instanceof Long) {
        	Element element = new Element();
        	element.setName(LONG_TAG);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, NAME_ATTR, name);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, VALUE_ATTR, value.toString());
        	return element;
        } else if (value instanceof Float) {
        	Element element = new Element();
        	element.setName(FLOAT_TAG);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, NAME_ATTR, name);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, VALUE_ATTR, value.toString());
        	return element;
        } else if (value instanceof String) {
        	Element element = new Element();
        	element.setName(STRING_TAG);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, NAME_ATTR, name);
        	element.addChild(Node.TEXT, value.toString());
            return element;
        } else if (value instanceof Set) {
        	Element element = new Element();
        	element.setName(STRING_SET_TAG);
        	element.setAttribute(KXmlParser.NO_NAMESPACE, NAME_ATTR, name);
        	Iterator itr = ((Set) value).iterator();
            while (itr.hasNext()) {
                Element childElement = new Element();
                childElement.setName(STRING_TAG);
                childElement.addChild(Node.TEXT, itr.next().toString());
                element.addChild(Node.ELEMENT, childElement);
            }
            return element;
        } else {
            throw new RuntimeException("SharedPreferences.writeValue: Unable to write value " + value);
        }
    }
}
