component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function beforeAll() {
		variables.testSecretName = "mysecret";
		variables.testSecretSimple = "test-secret";
	}

	function run(testResults, testBox) {
		describe("SecretProviderGet with AWS Secrets Manager Provider", function() {

			it("can get an existing secret", function() {
				var result = SecretProviderGet(key: variables.testSecretName, name: "sm");
				expect(result).toBeString();
				expect(len(result)).toBeGT(0);
			});

			it("can get simple string secret", function() {
				var result = SecretProviderGet(key: variables.testSecretSimple, name: "sm");
				expect(result).toBe("test-value");
			});

			it("can get JSON secret as raw string", function() {
				var result = SecretProviderGet(key: variables.testSecretName, name: "sm");
				expect(isJSON(result)).toBeTrue();
				var parsed = deserializeJSON(result);
				expect(parsed.username).toBe("admin");
				expect(parsed.password).toBe("sm-secret");
			});

			it("can traverse JSON secret with dot notation", function() {
				var result = SecretProviderGet(key: "#variables.testSecretName#.password", name: "sm");
				expect(result).toBe("sm-secret");
			});

			it("can traverse JSON secret for username", function() {
				var result = SecretProviderGet(key: "#variables.testSecretName#.username", name: "sm");
				expect(result).toBe("admin");
			});

			it("returns lazy reference when resolve is false", function() {
				var result = SecretProviderGet(key: variables.testSecretName, name: "sm", resolve: false);
				expect(result).toBeString();
			});

			it("resolves immediately when resolve is true", function() {
				var result = SecretProviderGet(key: variables.testSecretName, name: "sm", resolve: true);
				expect(result).toBeString();
			});

			it("throws exception for non-existent secret", function() {
				expect(function() {
					SecretProviderGet(key: "non-existent-secret-12345", name: "sm");
				}).toThrow();
			});

		});
	}

}
