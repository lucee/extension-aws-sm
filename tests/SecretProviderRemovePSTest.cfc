component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function run(testResults, testBox) {
		describe("SecretProviderRemove with AWS Parameter Store Provider", function() {

			it("can remove a parameter", function() {
				// First create a parameter
				SecretProviderSet("test-remove-ps", "to-be-removed", "ps");
				expect(SecretProviderGet("test-remove-ps", "ps")).toBe("to-be-removed");

				// Remove it
				SecretProviderRemove("test-remove-ps", "ps");

				// Verify it's gone
				expect(function() {
					SecretProviderGet("test-remove-ps", "ps");
				}).toThrow();
			});

			it("removed parameter no longer appears in list", function() {
				// Create and remove a parameter
				SecretProviderSet("test-remove-list-ps", "value", "ps");
				SecretProviderRemove("test-remove-list-ps", "ps");

				// Check it's not in the list
				var names = SecretProviderListNames("ps");
				expect(arrayFind(names, "test-remove-list-ps")).toBe(0);
			});

			it("throws exception for non-existent parameter", function() {
				expect(function() {
					SecretProviderRemove("non-existent-param-12345", "ps");
				}).toThrow();
			});

		});
	}

}
