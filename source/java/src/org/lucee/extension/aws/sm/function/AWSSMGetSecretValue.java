package org.lucee.extension.aws.sm.function;

import org.lucee.extension.aws.sm.SecretReciever;
import org.lucee.extension.aws.sm.util.CommonsUtil;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.ext.function.Function;
import lucee.runtime.util.Cast;

public class AWSSMGetSecretValue extends BIF implements Function {

	private static final long serialVersionUID = 1618963422976929654L;
	private static String PATTERN_LIMIT;

	static {
		// we only get this once, because this should only be read at startup, because
		// we don't want that
		// this can be overwritten at runtime by CFML Code.
		PATTERN_LIMIT = CommonsUtil.getSystemPropOrEnvVar("secretmanager.patternlimit", null);

	}

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {

		CFMLEngine e = CFMLEngineFactory.getInstance();
		Cast cast = e.getCastUtil();

		// validate arguments
		if (args.length < 1 || args.length > 6)
			throw e.getExceptionUtil().createFunctionException(pc, "AWSSMGetSecretValue", 1, 6, args.length);
		String secret = cast.toString(args[0]);

		if (!Util.isEmpty(PATTERN_LIMIT, true) && secret.indexOf(PATTERN_LIMIT) == -1)
			throw e.getExceptionUtil().createFunctionException(pc, "AWSSMGetSecretValue", 1, "secret",
					"only secrets containing the following string are allowed [" + PATTERN_LIMIT + "]", "");

		// region
		String region = null;
		if (args.length > 1)
			region = cast.toString(args[1]);
		if (e.getDecisionUtil().isEmpty(region, true))
			region = "us-east-1";

		// credentials
		String accessKeyId = null;
		String secretKey = null;

		if (args.length > 2) {
			if (args.length < 4)
				throw e.getExceptionUtil().createFunctionException(pc, "AWSSMGetSecretValue", 3, "accessKeyId",
						"you cannot define [accessKeyId] without defining [secretKey]", "");
			accessKeyId = cast.toString(args[2]);
			secretKey = cast.toString(args[3]);
		}

		// endpoint
		String endpoint = null;
		if (args.length > 4) {
			endpoint = cast.toString(args[4]);
		}

		// checkEnviroment
		boolean checkEnviroment = true;
		if (args.length > 5) {
			checkEnviroment = cast.toBooleanValue(args[5]);
		}

		String strSecret = SecretReciever.getSecret(pc, secret, SecretReciever.AWSCURRENT, region, accessKeyId,
				secretKey, endpoint, checkEnviroment, pc.getConfig().getLog("application"));
		return e.getCastUtil().fromJsonStringToStruct(strSecret);
	}
}