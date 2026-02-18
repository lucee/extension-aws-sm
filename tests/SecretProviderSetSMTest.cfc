component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function afterAll() {
		// Clean up test secrets
		try {
			SecretProviderRemove("test-set-secret", "sm");
		} catch (any e) {}
		try {
			SecretProviderRemove("test-set-boolean", "sm");
		} catch (any e) {}
		try {
			SecretProviderRemove("test-set-integer", "sm");
		} catch (any e) {}
	}

	function run(testResults, testBox) {
		describe("SecretProviderSet with AWS Secrets Manager Provider", function() {

			it("can set a string value", function() {
				SecretProviderSet("test-set-secret", "sm-test-value", "sm");
				var result = SecretProviderGet("test-set-secret", "sm");
				expect(result).toBe("sm-test-value");
			});

			it("can set a boolean value", function() {
				SecretProviderSet("test-set-boolean", true, "sm");
				var result = SecretProviderGet("test-set-boolean", "sm");
				expect(result).toBe("true");
			});

			it("can set an integer value", function() {
				SecretProviderSet("test-set-integer", 99999, "sm");
				var result = SecretProviderGet("test-set-integer", "sm");
				expect(result).toBe("99999");
			});

			it("can overwrite an existing secret", function() {
				SecretProviderSet("test-set-secret", "original-value", "sm");
				SecretProviderSet("test-set-secret", "updated-value", "sm");
				var result = SecretProviderGet("test-set-secret", "sm");
				expect(result).toBe("updated-value");
			});

			it("can set a JSON string value", function() {
				var jsonValue = '{"key":"value","number":123}';
				SecretProviderSet("test-set-secret", jsonValue, "sm");
				var result = SecretProviderGet("test-set-secret", "sm");
				expect(isJSON(result)).toBeTrue();
				var parsed = deserializeJSON(result);
				expect(parsed.key).toBe("value");
				expect(parsed.number).toBe(123);
			});

			it("new secret appears in list", function() {
				SecretProviderSet("test-set-secret", "list-test", "sm");
				var names = SecretProviderListNames("sm");
				expect(arrayFind(names, "test-set-secret")).toBeGT(0);
			});

		});
	}

}
