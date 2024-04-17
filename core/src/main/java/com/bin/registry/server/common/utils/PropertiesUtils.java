package com.bin.registry.server.common.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class PropertiesUtils {


	public static String getProperties(String key,String name) {
		String value = "";
		InputStream is = null;
		try {
			is = PropertiesUtils.class.getClassLoader().getResourceAsStream(name);
			Properties p = new Properties();
			p.load(is);
			value = p.getProperty(key);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return value;
	}

	public static Properties getProperties(String name) {
		Properties p = new Properties();
		InputStream is = null;
		try {
			is = PropertiesUtils.class.getClassLoader().getResourceAsStream(
					name);
			p.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return p;
	}

}