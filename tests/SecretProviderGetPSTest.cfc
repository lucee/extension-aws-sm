component extends="org.lucee.cfml.test.LuceeTestCase" labels="aws-sm" {

	function beforeAll() {

		var configDir=getPageContext().getConfig().getConfigServerDir();
		var configFile=configDir&"/.CFConfig.json";
		systemOutput("--- #configFile# ---",1,1);
		systemOutput(fileRead(configFile),1,1);
		systemOutput("lucee.base.config system prop: " & (createObject("java", "java.lang.System").getProperty("lucee.base.config") ?: "NOT SET"), true);
        systemOutput("lucee.base.config system prop: " & (server.system.properties["lucee.base.config"]?:"NOT SET"), 1,1);
        systemOutput("LUCEE_BASE_CONFIG env var: " & (server.system.environment["LUCEE_BASE_CONFIG"]?:"NOT SET"), 1,1);

		variables.testParameterName = "myparameter";
		variables.testParameterSimple = "test-parameter";
	}

	function run(testResults, testBox) {
		describe("SecretProviderGet with AWS Parameter Store Provider", function() {

			it("can get an existing parameter", function() {
				var result = SecretProviderGet(key: variables.testParameterName, name: "ps");
				expect(result).toBeString();
				expect(len(result)).toBeGT(0);
			});

			it("can get simple string parameter", function() {
				var result = SecretProviderGet(key: variables.testParameterSimple, name: "ps");
				expect(result).toBe("test-value");
			});

			it("can get JSON parameter as raw string", function() {
				var result = SecretProviderGet(key: variables.testParameterName, name: "ps");
				expect(isJSON(result)).toBeTrue();
				var parsed = deserializeJSON(result);
				expect(parsed.username).toBe("admin");
				expect(parsed.password).toBe("ps-secret");
			});

			it("can traverse JSON parameter with dot notation", function() {
				var result = SecretProviderGet(key: "#variables.testParameterName#.password", name: "ps");
				expect(result).toBe("ps-secret");
			});

			it("can traverse JSON parameter for username", function() {
				var result = SecretProviderGet(key: "#variables.testParameterName#.username", name: "ps");
				expect(result).toBe("admin");
			});

			it("returns lazy reference when resolve is false", function() {
				var result = SecretProviderGet(key: variables.testParameterName, name: "ps", resolve: false);
				expect(result).toBeString();
			});

			it("resolves immediately when resolve is true", function() {
				var result = SecretProviderGet(key: variables.testParameterName, name: "ps", resolve: true);
				expect(result).toBeString();
			});

			it("throws exception for non-existent parameter", function() {
				expect(function() {
					SecretProviderGet(key: "non-existent-parameter-12345", name: "ps");
				}).toThrow();
			});

		});
	}

}
