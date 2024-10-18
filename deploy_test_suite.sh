echo zip test suite...
cd test-suites
zip -r allergy-intolerance-suite.zip allergy-intolerance-suite
curl -F updateSpecification=true -F specification=024311C1XAF0EX4B46X96F4XB516B9F755B1 -F testSuite=@allergy-intolerance-suite.zip --header "ITB_API_KEY: F6628973X0EE7X40ECX9C36X8384C71F63D2" -X POST http://localhost:9000/api/rest/testsuite/deploy
rm -f allergy-intolerance-suite.zip
