component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function afterAll() {
		// Clean up test parameters
		try {
			SecretProviderRemove("test-set-param", "ps");
		} catch (any e) {}
		try {
			SecretProviderRemove("test-set-bool-param", "ps");
		} catch (any e) {}
		try {
			SecretProviderRemove("test-set-int-param", "ps");
		} catch (any e) {}
	}

	function run(testResults, testBox) {
		describe("SecretProviderSet with AWS Parameter Store Provider", function() {

			it("can set a string value", function() {
				SecretProviderSet("test-set-param", "ps-test-value", "ps");
				var result = SecretProviderGet("test-set-param", "ps");
				expect(result).toBe("ps-test-value");
			});

			it("can set a boolean value", function() {
				SecretProviderSet("test-set-bool-param", true, "ps");
				var result = SecretProviderGet("test-set-bool-param", "ps");
				expect(result).toBe("true");
			});

			it("can set an integer value", function() {
				SecretProviderSet("test-set-int-param", 77777, "ps");
				var result = SecretProviderGet("test-set-int-param", "ps");
				expect(result).toBe("77777");
			});

			it("can overwrite an existing parameter", function() {
				SecretProviderSet("test-set-param", "original-value", "ps");
				SecretProviderSet("test-set-param", "updated-value", "ps");
				var result = SecretProviderGet("test-set-param", "ps");
				expect(result).toBe("updated-value");
			});

			it("can set a JSON string value", function() {
				var jsonValue = '{"param":"value","count":456}';
				SecretProviderSet("test-set-param", jsonValue, "ps");
				var result = SecretProviderGet("test-set-param", "ps");
				expect(isJSON(result)).toBeTrue();
				var parsed = deserializeJSON(result);
				expect(parsed.param).toBe("value");
				expect(parsed.count).toBe(456);
			});

			it("new parameter appears in list", function() {
				SecretProviderSet("test-set-param", "list-test", "ps");
				var names = SecretProviderListNames("ps");
				expect(arrayFind(names, "test-set-param")).toBeGT(0);
			});

		});
	}

}
