package put.roadef.conf;

import java.lang.reflect.Constructor;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class RoadefConfiguration {
	PropertiesConfiguration properties;

	public RoadefConfiguration(String fileName) throws ConfigurationException {
		properties = new PropertiesConfiguration(fileName);
	}

	public Object getInstanceAndSetup(String key) {
		String className = getString(key);
		if (className == "null")
			return null;
		String basePath = key;
		if (className.startsWith("@")) {
			basePath = className.substring(1);
			className = getString(basePath + ".class");
		}
		Object o = createInstance(className);
		if (o instanceof Setup)
			((Setup) o).setup(this, basePath);
		return o;
	}

	public static Object createInstance(String className) {
		Class<?> c = getRoadefClass(className);
		Class<?>[] classParams;
		Object[] objectParams;
		objectParams = null;
		classParams = null;
		Constructor<?> co;
		try {
			co = c.getConstructor(classParams);
			return co.newInstance(objectParams);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	public static Class<?> getRoadefClass(String className) {
		String packageNames[] = new String[] { "", "put.roadef.", "put.roadef.solvers.", "put.roadef.tweaks.",
				"put.roadef.neighborhoods.", "put.roadef.bnb.", "put.roadef.ip.", "put.roadef.selectors.", "put.roadef.hh." };
		for (String packageName : packageNames) {
			try {
				String name = packageName + className;
				return Class.forName(name);
			} catch (Exception e) {
				//Retry
			}
		}
		System.err.println("Error: Could not found class " + className);
		System.exit(-1);
		return null;
	}

	/**
	 * @deprecated use getString(key, defaultValue)
	 */
	@Deprecated
	public String getString(String key) {
		beforePropertyRead(key);
		String val = properties.getString(key);
		afterPropertyRead(key, String.valueOf(val));
		return val;
	}

	public String getString(String key, String defaultValue) {
		beforePropertyRead(key);
		String val = properties.getString(key, defaultValue);
		afterPropertyRead(key, String.valueOf(val));
		return val;
	}

	/**
	 * @deprecated use getBoolean(key, defaultValue)
	 */
	@Deprecated
	public boolean getBoolean(String key) {
		beforePropertyRead(key);
		boolean val = properties.getBoolean(key);
		afterPropertyRead(key, String.valueOf(val));
		return val;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		beforePropertyRead(key);
		boolean val = properties.getBoolean(key, defaultValue);
		afterPropertyRead(key, String.valueOf(val));
		return val;
	}

	public int getInt(String key) {
		beforePropertyRead(key);
		int val = properties.getInt(key);
		afterPropertyRead(key, String.valueOf(val));
		return val;
	}

	public int getInt(String key, int defaultValue) {
		beforePropertyRead(key);
		int val = properties.getInt(key, defaultValue);
		afterPropertyRead(key, String.valueOf(val));
		return val;
	}

	public double getDouble(String key) {
		beforePropertyRead(key);
		double val = properties.getDouble(key);
		afterPropertyRead(key, String.valueOf(val));
		return val;
	}
	
	public double getDouble(String key, double defaultValue) {
		beforePropertyRead(key);
		double val = properties.getDouble(key, defaultValue);
		afterPropertyRead(key, String.valueOf(val));
		return val;
	}

	public void beforePropertyRead(String key) {
		//System.out.print(key);
	}

	public void afterPropertyRead(String key, String value) {
		//System.out.println(" = " + value);
		//System.out.println(key + " = " + value);
	}
}
