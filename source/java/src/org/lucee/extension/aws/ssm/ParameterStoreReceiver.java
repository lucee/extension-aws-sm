package org.lucee.extension.aws.ssm;

import java.net.URI;

import org.lucee.extension.aws.sm.util.CommonsUtil;

import lucee.commons.io.log.Log;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

public class ParameterStoreReceiver {

	public static String getParameter(PageContext pc, String parameterName, boolean withDecryption, String region,
			String accessKeyId, String secretKey, String endpoint, boolean checkEnvVarAndSystemProWhenArgNotProvided,
			Log log) throws PageException {

		SsmClientBuilder builder = SsmClient.builder();

		String secretOrigin = Util.isEmpty(accessKeyId, true) ? null : "provided";
		String endpointOrigin = Util.isEmpty(endpoint, true) ? null : "provided";

		if (checkEnvVarAndSystemProWhenArgNotProvided) {
			CFMLEngine eng = CFMLEngineFactory.getInstance();
			if (pc == null)
				pc = eng.getThreadPageContext();

			// application.cfc
			if (pc != null) {
				Struct data = CommonsUtil.getApplicationData(pc);
				Struct ssm = eng.getCastUtil().toStruct(data.get("ssm", null), null);
				if (ssm != null) {

					// accessKeyId
					if (Util.isEmpty(accessKeyId, true)) {
						accessKeyId = eng.getCastUtil().toString(ssm.get("accesskeyid", null), null);
						if (!Util.isEmpty(accessKeyId, true)) {
							secretOrigin = "application";
						}
					}
					if (Util.isEmpty(accessKeyId, true)) {
						accessKeyId = eng.getCastUtil().toString(ssm.get("accesskey", null), null);
						if (!Util.isEmpty(accessKeyId, true)) {
							secretOrigin = "application";
						}
					}

					// secretKey
					if (Util.isEmpty(secretKey, true))
						secretKey = eng.getCastUtil().toString(ssm.get("secretkey", null), null);
					if (Util.isEmpty(secretKey, true))
						secretKey = eng.getCastUtil().toString(ssm.get("awssecretkey", null), null);

					// region
					if (Util.isEmpty(region, true))
						region = eng.getCastUtil().toString(ssm.get("region", null), null);
					if (Util.isEmpty(region, true))
						region = eng.getCastUtil().toString(ssm.get("location", null), null);

					// endpoint
					if (Util.isEmpty(endpoint, true)) {
						endpoint = eng.getCastUtil().toString(ssm.get("endpoint", null), null);
						if (!Util.isEmpty(endpoint, true)) {
							endpointOrigin = "application";
						}
					}
					if (Util.isEmpty(endpoint, true)) {
						endpoint = eng.getCastUtil().toString(ssm.get("host", null), null);
						if (!Util.isEmpty(endpoint, true)) {
							endpointOrigin = "application";
						}
					}
					if (Util.isEmpty(endpoint, true)) {
						endpoint = eng.getCastUtil().toString(ssm.get("server", null), null);
						if (!Util.isEmpty(endpoint, true)) {
							endpointOrigin = "application";
						}
					}
				}
			}

			// accessKeyId
			if (Util.isEmpty(accessKeyId, true)) {
				accessKeyId = CommonsUtil.getSystemPropOrEnvVar("parameterstore.accesskeyid", null);
				if (!Util.isEmpty(accessKeyId, true)) {
					secretOrigin = "system property/environment variable";
				}
			}
			if (Util.isEmpty(accessKeyId, true)) {
				accessKeyId = CommonsUtil.getSystemPropOrEnvVar("parameterstore.accesskey", null);
				if (!Util.isEmpty(accessKeyId, true)) {
					secretOrigin = "system property/environment variable";
				}
			}

			// secretKey
			if (Util.isEmpty(secretKey, true))
				secretKey = CommonsUtil.getSystemPropOrEnvVar("parameterstore.secretkey", null);
			if (Util.isEmpty(secretKey, true))
				secretKey = CommonsUtil.getSystemPropOrEnvVar("parameterstore.awssecretkey", null);

			// region
			if (Util.isEmpty(region, true))
				region = CommonsUtil.getSystemPropOrEnvVar("parameterstore.region", null);
			if (Util.isEmpty(region, true))
				region = CommonsUtil.getSystemPropOrEnvVar("parameterstore.location", null);

			// endpoint
			if (Util.isEmpty(endpoint, true)) {
				endpoint = CommonsUtil.getSystemPropOrEnvVar("parameterstore.endpoint", null);
				if (!Util.isEmpty(endpoint, true)) {
					endpointOrigin = "system property/environment variable";
				}
			}
			if (Util.isEmpty(endpoint, true)) {
				endpoint = CommonsUtil.getSystemPropOrEnvVar("parameterstore.host", null);
				if (!Util.isEmpty(endpoint, true)) {
					endpointOrigin = "system property/environment variable";
				}
			}
			if (Util.isEmpty(endpoint, true)) {
				endpoint = CommonsUtil.getSystemPropOrEnvVar("parameterstore.server", null);
				if (!Util.isEmpty(endpoint, true)) {
					endpointOrigin = "system property/environment variable";
				}
			}
		}

		if (!Util.isEmpty(endpoint, true)) {
			String lcep = endpoint.toLowerCase();
			// no protocol
			if (lcep.indexOf("://") == -1) {
				endpoint = "https://" + endpoint;
			}
		}

		// aws credentials (optional when within EC2)
		if (!Util.isEmpty(accessKeyId, true)) {
			CommonsUtil.info(log, "setting secrets with origin [" + secretOrigin + "]");
			builder.credentialsProvider(
					StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKey)));
		} else {
			CommonsUtil.info(log, "setting no secrets explicitly");
		}

		// endpoint
		if (!Util.isEmpty(endpoint, true)) {
			CommonsUtil.info(log, "setting endpoint [" + endpoint + "] with origin [" + endpointOrigin + "]");
			builder.endpointOverride(URI.create(endpoint));
		} else {
			CommonsUtil.info(log, "setting no endpoint explicitly");
		}

		// region
		if (!Util.isEmpty(region, true)) {
			CommonsUtil.info(log, "setting region [" + region + "]");
			builder.region(Region.of(region));
		} else {
			CommonsUtil.info(log, "setting no region explicitly");
		}

		SsmClient client = builder.build();

		try {
			GetParameterRequest request = GetParameterRequest.builder().name(parameterName)
					.withDecryption(withDecryption).build();

			GetParameterResponse response = client.getParameter(request);

			return response.parameter().value();
		} catch (ParameterNotFoundException e) {
			throw CFMLEngineFactory.getInstance().getExceptionUtil()
					.createApplicationException("Parameter [" + parameterName + "] not found in AWS Parameter Store");
		} catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getExceptionUtil().createApplicationException(
					"Error retrieving parameter [" + parameterName + "]: " + e.getMessage());
		}
	}
}