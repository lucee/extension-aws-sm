package org.lucee.extension.aws.sm.util;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.type.Struct;

public class CommonsUtil {
	private static BIF bif;

	public static String getSystemPropOrEnvVar(String name, String defaultValue) {
		// env
		String value = System.getenv(name);
		if (!Util.isEmpty(value))
			return value;

		// prop
		value = System.getProperty(name);
		if (!Util.isEmpty(value))
			return value;

		// env 2
		name = name.replace('.', '_').toUpperCase();
		value = System.getenv(name);
		if (!Util.isEmpty(value))
			return value;

		return defaultValue;
	}

	public static Struct getApplicationData(PageContext pc) throws RuntimeException {
		CFMLEngine eng = CFMLEngineFactory.getInstance();
		try {
			if (bif == null || bif.getClass().getClassLoader() != pc.getClass().getClassLoader()) {
				bif = eng.getClassUtil().loadBIF(pc, "lucee.runtime.functions.system.GetApplicationSettings");
			}
			return (Struct) bif.invoke(pc, new Object[] { Boolean.TRUE });
		} catch (Exception e) {
			throw eng.getCastUtil().toPageRuntimeException(e);
		}
	}
}
