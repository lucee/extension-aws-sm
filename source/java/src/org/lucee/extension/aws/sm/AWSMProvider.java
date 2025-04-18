package org.lucee.extension.aws.sm;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lucee.commons.io.log.Log;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.security.SecretProvider;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

public class AWSMProvider implements SecretProvider {

	private String region;
	private String accessKeyId;
	private String secretKey;
	private String endpoint;
	private boolean checkEnviroment;
	private CFMLEngine e;
	private Cast cast;
	private Config config;
	private String name;
	private boolean jsonTraversal;
	private long timeout;

	private Map<String, Reference<Val>> cache;

	@Override
	public void init(Config config, Struct properties, String name) throws PageException {
		this.config = config;
		this.name = name;
		e = CFMLEngineFactory.getInstance();
		cast = e.getCastUtil();

		// region
		region = cast.toString(properties.get("region", null), null);
		if (e.getDecisionUtil().isEmpty(region, true)) {
			region = "us-east-1";
		}

		// accessKeyId
		accessKeyId = cast.toString(properties.get("accessKeyId", null), null);
		if (e.getDecisionUtil().isEmpty(accessKeyId, true)) {
			accessKeyId = null;
		}

		// secretKey
		secretKey = cast.toString(properties.get("secretKey", null), null);
		if (e.getDecisionUtil().isEmpty(secretKey, true)) {
			secretKey = null;
		}

		// endpoint
		endpoint = cast.toString(properties.get("endpoint", null), null);
		if (e.getDecisionUtil().isEmpty(endpoint, true)) {
			endpoint = null;
		}

		// timeout
		timeout = cast.toLongValue(properties.get("timeout", null), 0L);
		if (timeout > 0L)
			cache = new ConcurrentHashMap<>();

		// checkEnviroment
		checkEnviroment = cast.toBooleanValue(properties.get("checkEnviroment", null), true);

		// enableJsonPath
		jsonTraversal = cast.toBooleanValue(properties.get("jsonTraversal", null), true);
	}

	@Override
	public String getSecret(String key) throws PageException {
		if (Util.isEmpty(key, true)) {
			throw e.getExceptionUtil()
					.createApplicationException("the given key is empty, you need to provide a valid key");
		}

		// Only split by dot if JSON path is enabled
		if (jsonTraversal) {
			String[] arr = e.getListUtil().toStringArray(key, ".");
			String strSecret = get(arr[0]);

			if (arr.length == 1)
				return strSecret;

			// expect a JSON Structure
			Struct data = cast.fromJsonStringToStruct(strSecret);
			Object o;
			for (int i = 1; i < arr.length; i++) {
				o = data.get(arr[i].trim());
				// last array element
				if (i + 1 == arr.length) {
					if (e.getDecisionUtil().isSimpleValue(o))
						return cast.toString(o);
					else
						return cast.fromStructToJsonString(cast.toStruct(o), false);
				}
				data = cast.toStruct(o);
			}
			return null; // we never should get here
		}
		// simple key
		else {
			// Simple mode - just get the exact key without parsing
			return get(key);
		}
	}

	private String get(String key) throws PageException {
		key = key.trim();
		if (timeout > 0) {
			Reference<Val> ref = cache.get(key);
			if (ref != null) {
				Val val = ref.get();
				if (val.created + timeout > System.currentTimeMillis())
					return val.value;
			}
		}

		String value = SecretReciever.getSecret(null, key, SecretReciever.AWSCURRENT, region, accessKeyId, secretKey,
				endpoint, checkEnviroment);
		// cache the value for future use
		if (timeout > 0) {
			cache.put(key, new SoftReference<Val>(new Val(value)));
		}
		return value;

	}

	@Override
	public String getSecret(String key, String defaultValue) {
		try {
			return getSecret(key);
		} catch (PageException e) {
			return defaultValue;
		}
	}

	@Override
	public boolean getSecretAsBoolean(String key) throws PageException {
		return cast.toBooleanValue(getSecret(key));
	}

	@Override
	public boolean getSecretAsBoolean(String key, boolean defaultValue) {
		Object obj = getSecret(key, null);
		if (obj == null)
			return defaultValue;
		return cast.toBooleanValue(obj, defaultValue);
	}

	@Override
	public int getSecretAsInteger(String key) throws PageException {
		return cast.toIntValue(getSecret(key));
	}

	@Override
	public int getSecretAsInteger(String key, int defaultValue) {
		Object obj = getSecret(key, null);
		if (obj == null)
			return defaultValue;
		return cast.toIntValue(obj, defaultValue);
	}

	@Override
	public boolean hasSecret(String key) {
		return getSecret(key, null) != null;
	}

	@Override
	public void refresh() throws PageException {
		// TODO

	}

	@Override
	public Log getLog() {
		return config.getLog("application");
	}

	@Override
	public String getName() {
		return name;
	}

	private static class Val {
		private String value;
		private long created;

		public Val(String value) {
			this.value = value;
			this.created = System.currentTimeMillis();
		}
	}
}
