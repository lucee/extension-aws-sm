package org.lucee.extension.aws.sm;

import org.lucee.extension.aws.sm.util.CommonsUtil;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;

public class SecretReciever {

	public static final String AWSPENDING = "AWSPENDING";
	public static final String AWSCURRENT = "AWSCURRENT";
	public static final String AWSPREVIOUS = "AWSPREVIOUS";

	public static String getSecret(PageContext pc, String secretName, String staging, String region, String accessKeyId,
			String secretKey, String endpoint, boolean checkEnvVarAndSystemProWhenArgNotProvided) throws PageException {
		AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();

		if (checkEnvVarAndSystemProWhenArgNotProvided) {
			CFMLEngine eng = CFMLEngineFactory.getInstance();
			if (pc == null)
				pc = eng.getThreadPageContext();

			// application.cfc
			if (pc != null) {
				Struct data = CommonsUtil.getApplicationData(pc);
				Struct sm = eng.getCastUtil().toStruct(data.get("sm", null), null);
				if (sm != null) {
					// accessKeyId
					if (Util.isEmpty(accessKeyId, true))
						accessKeyId = eng.getCastUtil().toString(sm.get("accesskeyid", null), null);
					if (Util.isEmpty(accessKeyId, true))
						accessKeyId = eng.getCastUtil().toString(sm.get("accesskey", null), null);

					// secretKey
					if (Util.isEmpty(secretKey, true))
						secretKey = eng.getCastUtil().toString(sm.get("secretkey", null), null);
					if (Util.isEmpty(secretKey, true))
						secretKey = eng.getCastUtil().toString(sm.get("awssecretkey", null), null);

					// region
					if (Util.isEmpty(region, true))
						region = eng.getCastUtil().toString(sm.get("region", null), null);
					if (Util.isEmpty(region, true))
						region = eng.getCastUtil().toString(sm.get("location", null), null);

					// endpoint
					if (Util.isEmpty(endpoint, true))
						endpoint = eng.getCastUtil().toString(sm.get("endpoint", null), null);
					if (Util.isEmpty(endpoint, true))
						endpoint = eng.getCastUtil().toString(sm.get("host", null), null);
					if (Util.isEmpty(endpoint, true))
						endpoint = eng.getCastUtil().toString(sm.get("server", null), null);
				}

			}

			// accessKeyId
			if (Util.isEmpty(accessKeyId, true))
				accessKeyId = CommonsUtil.getSystemPropOrEnvVar("secretmanager.accesskeyid", null);
			if (Util.isEmpty(accessKeyId, true))
				accessKeyId = CommonsUtil.getSystemPropOrEnvVar("secretmanager.accesskey", null);

			// secretKey
			if (Util.isEmpty(secretKey, true))
				secretKey = CommonsUtil.getSystemPropOrEnvVar("secretmanager.secretkey", null);
			if (Util.isEmpty(secretKey, true))
				secretKey = CommonsUtil.getSystemPropOrEnvVar("secretmanager.awssecretkey", null);

			// region
			if (Util.isEmpty(region, true))
				region = CommonsUtil.getSystemPropOrEnvVar("secretmanager.region", null);
			if (Util.isEmpty(region, true))
				region = CommonsUtil.getSystemPropOrEnvVar("secretmanager.location", null);

			// endpoint
			if (Util.isEmpty(endpoint, true))
				endpoint = CommonsUtil.getSystemPropOrEnvVar("secretmanager.endpoint", null);
			if (Util.isEmpty(endpoint, true))
				endpoint = CommonsUtil.getSystemPropOrEnvVar("secretmanager.host", null);
			if (Util.isEmpty(endpoint, true))
				endpoint = CommonsUtil.getSystemPropOrEnvVar("secretmanager.server", null);
		}

		if (!Util.isEmpty(endpoint, true)) {
			String lcep = endpoint.toLowerCase();
			// no protocol
			if (lcep.indexOf("://") == -1) {
				endpoint = "https://" + endpoint;
			}
		}

		// aws credetials (optional when within EC2)
		if (!Util.isEmpty(accessKeyId, true)) {
			builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey)));
		}

		// endpoint
		if (!Util.isEmpty(endpoint, true)) {
			builder.withEndpointConfiguration(
					new EndpointConfiguration(endpoint, !Util.isEmpty(region, true) ? region : null));
		}
		// region
		else if (!Util.isEmpty(region, true)) {
			builder.withRegion(region);
		}

		AWSSecretsManager client = builder.build();

		GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName)
				.withVersionStage(staging);
		GetSecretValueResult getSecretValueResult = client.getSecretValue(getSecretValueRequest);

		String str = getSecretValueResult.getSecretString();
		if (Util.isEmpty(str, true))
			str = CFMLEngineFactory.getInstance().getCastUtil().toBase64(getSecretValueResult.getSecretBinary());

		return str;
	}
}
