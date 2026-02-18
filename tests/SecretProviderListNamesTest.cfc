component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function run(testResults, testBox) {
		describe("SecretProviderListNames with AWS Secrets Manager Provider", function() {

			it("can list secret names", function() {
				var result = SecretProviderListNames("sm");
				expect(result).toBeArray();
			});

			it("contains seeded secret", function() {
				var result = SecretProviderListNames("sm");
				expect(arrayFind(result, "mysecret")).toBeGT(0);
			});

			it("contains test secret", function() {
				var result = SecretProviderListNames("sm");
				expect(arrayFind(result, "test-secret")).toBeGT(0);
			});

		});

		describe("SecretProviderListNames with AWS Parameter Store Provider", function() {

			it("can list parameter names", function() {
				var result = SecretProviderListNames("ps");
				expect(result).toBeArray();
			});

			it("contains seeded parameter", function() {
				var result = SecretProviderListNames("ps");
				expect(arrayFind(result, "myparameter")).toBeGT(0);
			});

			it("contains test parameter", function() {
				var result = SecretProviderListNames("ps");
				expect(arrayFind(result, "test-parameter")).toBeGT(0);
			});

		});

		describe("SecretProviderListNames without provider", function() {

			it("can list all secret names from all providers", function() {
				var result = SecretProviderListNames();
				expect(result).toBeArray();
				expect(arrayLen(result)).toBeGT(0);
			});

			it("returns deduplicated results", function() {
				var result = SecretProviderListNames();
				var unique = arrayLen(result);
				var asSet = {};
				for (var name in result) {
					asSet[name] = true;
				}
				expect(structCount(asSet)).toBe(unique);
			});

		});
	}

}
