package org.lucee.extension.aws.sm.function;

import org.lucee.extension.aws.sm.SecretReciever;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.ext.function.Function;
import lucee.runtime.type.Struct;
import lucee.runtime.util.Cast;

public class AWSSMValidateSecret extends BIF implements Function {

	private static final long serialVersionUID = -8851636032019534307L;

	@Override
	public Object invoke(PageContext pc, Object[] args) throws PageException {

		// AWSSMValidateSecret("${SM_ENV_REDIS}/sm-web","password","<5")

		CFMLEngine e = CFMLEngineFactory.getInstance();
		Cast cast = e.getCastUtil();

		// validate arguments
		if (args.length < 1 || args.length > 9)
			throw e.getExceptionUtil().createFunctionException(pc, "AWSSMValidateSecret", 1, 9, args.length);

		// secret
		String secret = cast.toString(args[0]);

		// key within the secret
		String key = null;
		if (args.length > 1)
			key = cast.toString(args[1]);
		if (e.getDecisionUtil().isEmpty(key, true))
			key = null;

		// value within the secret
		String val = null;
		if (args.length > 2)
			val = cast.toString(args[2]);
		if (e.getDecisionUtil().isEmpty(val, true))
			val = null;
		if (key == null && val != null)
			throw e.getExceptionUtil().createFunctionException(pc, "AWSSMValidateSecret", 3, "value",
					"you cannot define [value] without defining [key]", "");

		// key within the secret
		boolean throwOnError = false;
		if (args.length > 3)
			throwOnError = cast.toBooleanValue(args[3]);

		// region
		String region = null;
		if (args.length > 4)
			region = cast.toString(args[4]);
		if (e.getDecisionUtil().isEmpty(region, true))
			region = "us-east-1";

		// credentials from args
		String accessKeyId = null;
		String secretKey = null;
		if (args.length > 5) {
			if (args.length < 7)
				throw e.getExceptionUtil().createFunctionException(pc, "AWSSMValidateSecret", 6, "accessKeyId",
						"you cannot define [accessKeyId] without defining [secretKey]", "");
			accessKeyId = cast.toString(args[5]);
			secretKey = cast.toString(args[6]);
		}

		// endpoint
		String endpoint = null;
		if (args.length > 7) {
			endpoint = cast.toString(args[7]);
		}

		// checkEnviroment
		boolean checkEnviroment = true;
		if (args.length > 8) {
			checkEnviroment = cast.toBooleanValue(args[8]);
		}

		try {

			String strSecret = SecretReciever.getSecret(pc, secret, SecretReciever.AWSCURRENT, region, accessKeyId,
					secretKey, endpoint, checkEnviroment);

			Struct data = e.getCastUtil().fromJsonStringToStruct(strSecret);
			if (key != null) {
				String v = cast.toString(data.get(key, null), null);
				if (v == null) {
					if (throwOnError)
						throw e.getExceptionUtil().createApplicationException(
								"there is no element with name [" + key + "] in the secret structure", "");
					return false;
				}
				if (val != null) {
					if (!val.equals(v)) {
						if (throwOnError)
							throw e.getExceptionUtil().createApplicationException(
									"the provided value does not match the value that is stored in the secret under ["
											+ key + "]",
									"");
						return false;
					}
				}
			}
		} catch (Exception ex) {
			if (throwOnError)
				throw cast.toPageException(ex);
			return false;
		}
		return true;
	}
}