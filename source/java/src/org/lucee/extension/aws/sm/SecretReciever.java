package org.lucee.extension.aws.sm;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;

public class SecretReciever {

	public static final String AWSPENDING = "AWSPENDING";
	public static final String AWSCURRENT = "AWSCURRENT";
	public static final String AWSPREVIOUS = "AWSPREVIOUS";

	public static String getSecret(PageContext pc, String secretName, String staging, String region, String accessKeyId,
			String secretKey, String endpoint, boolean checkEnvVarAndSystemProWhenArgNotProvided, Log log)
			throws PageException {
		SecretsManagerClient client = buildClient(pc, region, accessKeyId, secretKey, endpoint,
				checkEnvVarAndSystemProWhenArgNotProvided, log);

		GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretName)
				.versionStage(staging).build();

		GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);

		String str = getSecretValueResponse.secretString();
		if (Util.isEmpty(str, true)) {
			SdkBytes secretBinary = getSecretValueResponse.secretBinary();
			if (secretBinary != null) {
				str = CFMLEngineFactory.getInstance().getCastUtil().toBase64(secretBinary.asByteArray());
			}
		}

		return str;
	}

	public static void setSecret(PageContext pc, String secretName, String secretValue, String region,
			String accessKeyId, String secretKey, String endpoint, boolean checkEnvVarAndSystemProWhenArgNotProvided,
			Log log) throws PageException {
		SecretsManagerClient client = buildClient(pc, region, accessKeyId, secretKey, endpoint,
				checkEnvVarAndSystemProWhenArgNotProvided, log);

		try {
			// Try to update existing secret
			PutSecretValueRequest putRequest = PutSecretValueRequest.builder().secretId(secretName)
					.secretString(secretValue).build();

			client.putSecretValue(putRequest);
			CommonsUtil.info(log, "updated secret [" + secretName + "]");
		} catch (ResourceNotFoundException e) {
			// Secret doesn't exist, create it
			CreateSecretRequest createRequest = CreateSecretRequest.builder().name(secretName).secretString(secretValue)
					.build();

			client.createSecret(createRequest);
			CommonsUtil.info(log, "created secret [" + secretName + "]");
		}
	}

	public static List<String> listSecretNames(PageContext pc, String region, String accessKeyId, String secretKey,
			String endpoint, boolean checkEnvVarAndSystemProWhenArgNotProvided, Log log) throws PageException {
		SecretsManagerClient client = buildClient(pc, region, accessKeyId, secretKey, endpoint,
				checkEnvVarAndSystemProWhenArgNotProvided, log);

		List<String> names = new ArrayList<>();
		String nextToken = null;

		do {
			ListSecretsRequest.Builder requestBuilder = ListSecretsRequest.builder();
			if (nextToken != null) {
				requestBuilder.nextToken(nextToken);
			}

			ListSecretsResponse response = client.listSecrets(requestBuilder.build());

			for (SecretListEntry entry : response.secretList()) {
				names.add(entry.name());
			}

			nextToken = response.nextToken();
		} while (nextToken != null);

		return names;
	}

	public static void removeSecret(PageContext pc, String secretName, String region, String accessKeyId,
			String secretKey, String endpoint, boolean checkEnvVarAndSystemProWhenArgNotProvided, boolean forceDelete,
			Log log) throws PageException {
		SecretsManagerClient client = buildClient(pc, region, accessKeyId, secretKey, endpoint,
				checkEnvVarAndSystemProWhenArgNotProvided, log);

		DeleteSecretRequest.Builder requestBuilder = DeleteSecretRequest.builder().secretId(secretName);

		if (forceDelete) {
			// Immediately delete without recovery window
			requestBuilder.forceDeleteWithoutRecovery(true);
		}

		client.deleteSecret(requestBuilder.build());
		CommonsUtil.info(log, "deleted secret [" + secretName + "]" + (forceDelete ? " (force)" : ""));
	}

	private static SecretsManagerClient buildClient(PageContext pc, String region, String accessKeyId, String secretKey,
			String endpoint, boolean checkEnvVarAndSystemProWhenArgNotProvided, Log log) throws PageException {
		SecretsManagerClientBuilder builder = SecretsManagerClient.builder();

		String secretOrigin = Util.isEmpty(accessKeyId, true) ? null : "provided";
		String endpointOrigin = Util.isEmpty(endpoint, true) ? null : "provided";

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
					if (Util.isEmpty(accessKeyId, true)) {
						accessKeyId = eng.getCastUtil().toString(sm.get("accesskeyid", null), null);
						if (!Util.isEmpty(accessKeyId, true)) {
							secretOrigin = "application";
						}
					}
					if (Util.isEmpty(accessKeyId, true)) {
						accessKeyId = eng.getCastUtil().toString(sm.get("accesskey", null), null);
						if (!Util.isEmpty(accessKeyId, true)) {
							secretOrigin = "application";
						}
					}

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
					if (Util.isEmpty(endpoint, true)) {
						endpoint = eng.getCastUtil().toString(sm.get("endpoint", null), null);
						if (!Util.isEmpty(endpoint, true)) {
							endpointOrigin = "application";
						}
					}
					if (Util.isEmpty(endpoint, true)) {
						endpoint = eng.getCastUtil().toString(sm.get("host", null), null);
						if (!Util.isEmpty(endpoint, true)) {
							endpointOrigin = "application";
						}
					}
					if (Util.isEmpty(endpoint, true)) {
						endpoint = eng.getCastUtil().toString(sm.get("server", null), null);
						if (!Util.isEmpty(endpoint, true)) {
							endpointOrigin = "application";
						}
					}
				}

			}

			// accessKeyId
			if (Util.isEmpty(accessKeyId, true)) {
				accessKeyId = CommonsUtil.getSystemPropOrEnvVar("secretmanager.accesskeyid", null);
				if (!Util.isEmpty(accessKeyId, true)) {
					secretOrigin = "system property/environment variable";
				}
			}
			if (Util.isEmpty(accessKeyId, true)) {
				accessKeyId = CommonsUtil.getSystemPropOrEnvVar("secretmanager.accesskey", null);
				if (!Util.isEmpty(accessKeyId, true)) {
					secretOrigin = "system property/environment variable";
				}
			}

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
			if (Util.isEmpty(endpoint, true)) {
				endpoint = CommonsUtil.getSystemPropOrEnvVar("secretmanager.endpoint", null);
				if (!Util.isEmpty(endpoint, true)) {
					endpointOrigin = "system property/environment variable";
				}
			}
			if (Util.isEmpty(endpoint, true)) {
				endpoint = CommonsUtil.getSystemPropOrEnvVar("secretmanager.host", null);
				if (!Util.isEmpty(endpoint, true)) {
					endpointOrigin = "system property/environment variable";
				}
			}
			if (Util.isEmpty(endpoint, true)) {
				endpoint = CommonsUtil.getSystemPropOrEnvVar("secretmanager.server", null);
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

		return builder.build();
	}
}