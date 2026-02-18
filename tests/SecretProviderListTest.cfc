component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function run(testResults, testBox) {
		describe("SecretProviderList with AWS Secrets Manager Provider", function() {

			it("can list secrets as struct", function() {
				var result = SecretProviderList("sm");
				expect(result).toBeStruct();
			});

			it("contains seeded secret", function() {
				var result = SecretProviderList("sm");
				expect(structKeyExists(result, "mysecret")).toBeTrue();
			});

			it("returns lazy references by default", function() {
				var result = SecretProviderList("sm", false);
				expect(result).toBeStruct();
			});

			it("resolves immediately when resolve is true", function() {
				var result = SecretProviderList("sm", true);
				expect(result).toBeStruct();
				if (structKeyExists(result, "test-secret")) {
					expect(result["test-secret"]).toBe("test-value");
				}
			});

		});

		describe("SecretProviderList with AWS Parameter Store Provider", function() {

			it("can list parameters as struct", function() {
				var result = SecretProviderList("ps");
				expect(result).toBeStruct();
			});

			it("contains seeded parameter", function() {
				var result = SecretProviderList("ps");
				expect(structKeyExists(result, "myparameter")).toBeTrue();
			});

			it("returns lazy references by default", function() {
				var result = SecretProviderList("ps", false);
				expect(result).toBeStruct();
			});

			it("resolves immediately when resolve is true", function() {
				var result = SecretProviderList("ps", true);
				expect(result).toBeStruct();
				if (structKeyExists(result, "test-parameter")) {
					expect(result["test-parameter"]).toBe("test-value");
				}
			});

		});

		describe("SecretProviderList without provider", function() {

			it("can list all secrets from all providers", function() {
				var result = SecretProviderList();
				expect(result).toBeStruct();
				expect(structCount(result)).toBeGT(0);
			});

			it("first provider takes precedence for duplicate keys", function() {
				var result = SecretProviderList();
				expect(result).toBeStruct();
			});

		});
	}

}
