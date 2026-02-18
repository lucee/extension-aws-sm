component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function run(testResults, testBox) {
		describe("Secret Provider Integration Tests", function() {

			describe("Full CRUD lifecycle with AWS Secrets Manager", function() {

				it("can create, read, update, and delete a secret", function() {
					var key = "test-crud-sm-#createUUID()#";

					// Create
					SecretProviderSet(key, "sm-initial", "sm");
					expect(SecretProviderGet(key, "sm")).toBe("sm-initial");

					// Verify in list
					var names = SecretProviderListNames("sm");
					expect(arrayFind(names, key)).toBeGT(0);

					// Update
					SecretProviderSet(key, "sm-updated", "sm");
					expect(SecretProviderGet(key, "sm")).toBe("sm-updated");

					// Delete
					SecretProviderRemove(key, "sm");
					sleep(500);
					expect(function() {
						SecretProviderGet(key, "sm");
					}).toThrow();
				});

			});

			describe("Full CRUD lifecycle with AWS Parameter Store", function() {

				it("can create, read, update, and delete a parameter", function() {
					var key = "test-crud-ps-#createUUID()#";

					// Create
					SecretProviderSet(key, "ps-initial", "ps");
					expect(SecretProviderGet(key, "ps")).toBe("ps-initial");

					// Verify in list
					var names = SecretProviderListNames("ps");
					expect(arrayFind(names, key)).toBeGT(0);

					// Update
					SecretProviderSet(key, "ps-updated", "ps");
					expect(SecretProviderGet(key, "ps")).toBe("ps-updated");

					// Delete
					SecretProviderRemove(key, "ps");
					expect(function() {
						SecretProviderGet(key, "ps");
					}).toThrow();
				});

			});

			describe("Cross-provider operations", function() {

				it("same key can exist in both sm and ps with different values", function() {
					var key = "cross-provider-#createUUID()#";

					// Set in both providers
					SecretProviderSet(key, "sm-value", "sm");
					SecretProviderSet(key, "ps-value", "ps");

					// Verify each provider returns its own value
					expect(SecretProviderGet(key, "sm")).toBe("sm-value");
					expect(SecretProviderGet(key, "ps")).toBe("ps-value");

					// Cleanup
					SecretProviderRemove(key, "sm");
					SecretProviderRemove(key, "ps");
				});

			});

			describe("Type handling with AWS Secrets Manager", function() {

				it("handles JSON string values correctly", function() {
					var key = "type-test-json-sm-#createUUID()#";
					var jsonValue = '{"key":"value","number":123}';

					SecretProviderSet(key, jsonValue, "sm");
					var result = SecretProviderGet(key, "sm");

					expect(isJSON(result)).toBeTrue();
					var parsed = deserializeJSON(result);
					expect(parsed.key).toBe("value");
					expect(parsed.number).toBe(123);

					// Cleanup
					SecretProviderRemove(key, "sm");
				});

				it("handles boolean values", function() {
					var key = "type-test-bool-sm-#createUUID()#";

					SecretProviderSet(key, true, "sm");
					expect(SecretProviderGet(key, "sm")).toBe("true");

					// Cleanup
					SecretProviderRemove(key, "sm");
				});

				it("handles integer values", function() {
					var key = "type-test-int-sm-#createUUID()#";

					SecretProviderSet(key, 12345, "sm");
					expect(SecretProviderGet(key, "sm")).toBe("12345");

					// Cleanup
					SecretProviderRemove(key, "sm");
				});

			});

			describe("Type handling with AWS Parameter Store", function() {

				it("handles JSON string values correctly", function() {
					var key = "type-test-json-ps-#createUUID()#";
					var jsonValue = '{"param":"value","count":456}';

					SecretProviderSet(key, jsonValue, "ps");
					var result = SecretProviderGet(key, "ps");

					expect(isJSON(result)).toBeTrue();
					var parsed = deserializeJSON(result);
					expect(parsed.param).toBe("value");
					expect(parsed.count).toBe(456);

					// Cleanup
					SecretProviderRemove(key, "ps");
				});

				it("handles boolean values", function() {
					var key = "type-test-bool-ps-#createUUID()#";

					SecretProviderSet(key, false, "ps");
					expect(SecretProviderGet(key, "ps")).toBe("false");

					// Cleanup
					SecretProviderRemove(key, "ps");
				});

				it("handles integer values", function() {
					var key = "type-test-int-ps-#createUUID()#";

					SecretProviderSet(key, -9999, "ps");
					expect(SecretProviderGet(key, "ps")).toBe("-9999");

					// Cleanup
					SecretProviderRemove(key, "ps");
				});

			});

		});
	}

}
