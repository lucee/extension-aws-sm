component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function run(testResults, testBox) {
		describe("SecretProviderRemove with AWS Secrets Manager Provider", function() {

			it("can remove a secret", function() {
				// First create a secret
				SecretProviderSet("test-remove-sm", "to-be-removed", "sm");
				expect(SecretProviderGet("test-remove-sm", "sm")).toBe("to-be-removed");

				// Remove it
				SecretProviderRemove("test-remove-sm", "sm");

				// Verify it's gone (may need to wait due to AWS eventual consistency)
				sleep(500);
				expect(function() {
					SecretProviderGet("test-remove-sm", "sm");
				}).toThrow();
			});

			it("removed secret no longer appears in list", function() {
				// Create and remove a secret
				SecretProviderSet("test-remove-list-sm", "value", "sm");
				SecretProviderRemove("test-remove-list-sm", "sm");

				// Check it's not in the list
				sleep(500);
				var names = SecretProviderListNames("sm");
				expect(arrayFind(names, "test-remove-list-sm")).toBe(0);
			});

			it("throws exception for non-existent secret", function() {
				expect(function() {
					SecretProviderRemove("non-existent-secret-12345", "sm");
				}).toThrow();
			});

		});
	}

}
